package io.github.chrisimx.scanbridge.androidservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.app.NotificationCompat
import androidx.lifecycle.application
import androidx.room.withTransaction
import io.github.chrisimx.esclkt.ESCLHttpCallResult
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.JobState
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.scanbridge.MainActivity
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.data.model.ScanJob
import io.github.chrisimx.scanbridge.data.ui.ScanRelativeRotation
import io.github.chrisimx.scanbridge.data.ui.ScanningScreenEvent
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.daos.ScannedPageDao
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.services.ScanJobRepository
import io.github.chrisimx.scanbridge.util.snackbarErrorRetrievingPage
import io.github.chrisimx.scanbridge.util.toJobStateString
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.jvm.java
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import timber.log.Timber

class ScanJobForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "scan_job_service_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_CANCEL = "io.github.chrisimx.scanbridge.CANCEL_SCAN"

        fun startService(context: Context) {
            val intent = Intent(context, ScanJobForegroundService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ScanJobForegroundService::class.java)
            context.stopService(intent)
        }
    }

    private val scanJobs: ScanJobRepository by inject()
    private val db: ScanBridgeDb by inject()
    private val scannedPageDao: ScannedPageDao = db.scannedPageDao()
    @Volatile
    private var isRunning = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Timber.d("Foreground service created")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("Foreground service started")

        // Handle cancel action
        if (intent?.action == ACTION_CANCEL) {
            Timber.d("Cancel action received from notification")
            scanJobs.setCancel(true)
            return START_NOT_STICKY
        }

        if (isRunning) {
            Timber.d("Service already running, ignoring this start request")
            return START_NOT_STICKY
        }
        isRunning = true

        serviceScope.launch {
            try {
                scanJobs.setJobRunning(true)
                var job = scanJobs.nextJob()
                while (job != null) {
                    doScan(job)
                    job = scanJobs.nextJob()
                }
            } finally {
                scanJobs.setJobRunning(false)
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Timber.d("Foreground service destroyed")
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val cancelIntent = Intent(this, ScanJobForegroundService::class.java).apply {
            action = ACTION_CANCEL
        }

        val cancelPendingIntent = PendingIntent.getService(
            this,
            1,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(this.applicationContext.getString(R.string.scan_job_running))
            .setContentText(this.applicationContext.getString(R.string.scanned_pages_received_in_bg))
            .setSmallIcon(R.drawable.icon_about_dialog)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
            })
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.cancel_scan),
                cancelPendingIntent
            )
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "Scan job execution service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Scan job execution service"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun doScan(scanJob: ScanJob) {
        val currentScanSettings = scanJob.scanSettings

        val esclRequestClient = scanJob.esclClient

        scanJobs.notifyStarted(scanJob)

        if (abortIfCancelling()) return

        Timber.d("Creating scan job. eSCLKt scan settings: $currentScanSettings")
        val job =
            esclRequestClient.createJob(currentScanSettings)
        Timber.d("Creation request done. Result: $job")
        if (job !is ESCLRequestClient.ScannerCreateJobResult.Success) {
            Timber.e("Job creation failed. Result: $job")

            scanJobs.notifyFailed(scanJob, job.toString())
            return
        }
        val jobResult = job.scanJob

        if (abortIfCancelling(jobResult)) return

        var polling = false

        while (true) {
            if (polling) {
                for (retries in 0..60) {
                    if (abortIfCancelling(jobResult)) return
                    val status = jobResult.getJobStatus()
                    val isRunning = status?.jobState == JobState.Processing || status?.jobState == JobState.Pending
                    val imagesToTransfer = status?.imagesToTransfer
                    Timber.d(
                        "Polling job status. Retry: $retries Result: $status imagesToTransfer: $imagesToTransfer isRunning: $isRunning"
                    )
                    if (!isRunning) {
                        Timber.d("Job is reported to be not running anymore. jobRunning = false")

                        val deleteResult = jobResult.cancel()
                        Timber.d("Cancelling job after (a likely) failure: $deleteResult")

                        if (status?.jobState != JobState.Completed) {
                            val jobStateString = status?.jobState.toJobStateString(application)
                            Timber.w("Job info doesn't indicate completion: $jobStateString")
                            scanJobs.notifyFailed(scanJob, status.toString())
                        }
                        return
                    }
                    if (imagesToTransfer != null && imagesToTransfer > 0u) {
                        Timber.d("There seem to be images to transfer. Breaking out of polling loop")
                        break
                    }
                    delay(1000)
                }
            }

            if (abortIfCancelling(jobResult)) return

            Timber.d("Retrieving next page")
            val nextPage = jobResult.retrieveNextPage()
            Timber.d("Next page result: $nextPage")
            val status = jobResult.getJobStatus()
            Timber.d("Retrieved job info: $status")
            val jobStateString = status?.jobState.toJobStateString(application)
            Timber.d("Job info as human readable: $jobStateString")
            when (nextPage) {
                is ESCLRequestClient.ScannerNextPageResult.NoFurtherPages -> {
                    Timber.d("Next page result is seen as no further pages. jobRunning = false")

                    if (status?.jobState != JobState.Completed) {
                        Timber.w("Job info doesn't indicate completion: $jobStateString")
                        scanJobs.notifyFailed(scanJob,application.getString(
                            R.string.no_further_pages,
                            jobStateString
                        ))
                    } else {
                        scanJobs.notifyCompleted(scanJob)
                    }
                    val deletionResult = jobResult.cancel()
                    Timber.d("Cancelling job after no further pages is reported: $deletionResult")
                    return
                }

                is ESCLRequestClient.ScannerNextPageResult.RequestFailure -> {
                    if (nextPage.exception !is ESCLHttpCallResult.Error.HttpError) {
                        reportErrorWhileScanning(scanJob, nextPage, jobResult)
                        return
                    }
                    val error = nextPage.exception as ESCLHttpCallResult.Error.HttpError

                    if (status?.jobState == JobState.Completed) {
                        Timber.d("Job info indicates completion but response was not 404: $jobStateString")
                        scanJobs.notifyFailed(scanJob, application.getString(
                            R.string.no_further_pages,
                            jobStateString
                        ))
                        val deletionResult = jobResult.cancel()
                        Timber.d("Cancelling job after non-standard completion: $deletionResult")
                        return
                    } else {
                        Timber.e("Not successful code while retrieving next page: $nextPage")
                        if (error.code == 503) {
                            // Retry with polling
                            Timber.d("503 error received. Retrying with polling")
                            polling = true
                            continue
                        } else {
                            scanJobs.notifyFailed(scanJob, nextPage.toString())
                            val deletionResult = jobResult.cancel()
                            Timber.d("Cancelling job after not successful response while trying to retrieve page: $deletionResult")
                            return
                        }
                    }
                }

                is ESCLRequestClient.ScannerNextPageResult.Success -> {
                }

                else -> {
                    reportErrorWhileScanning(scanJob, nextPage, jobResult)
                    return
                }
            }
            Timber.d("Received page. Copying to file")
            var filePath: Path
            while (true) {
                val scanPageFile = "scan-" + Uuid.random().toString() + ".jpg"
                val file = File(application.filesDir, scanPageFile)
                file.exists().let {
                    if (!it) {
                        filePath = file.toPath()
                        break
                    }
                }
            }

            Timber.d("Scan page file created: $filePath")

            try {
                withContext(Dispatchers.IO) {
                    Files.copy(nextPage.page.data.inputStream(), filePath)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error while copying received image to file. Aborting!")
                scanJobs.notifyFailed(scanJob, application.getString(
                    R.string.error_while_copying_received_image_to_file,
                    e.message
                ))
                val deletionResult = jobResult.cancel()
                Timber.d("Cancelling job after error while trying to copy received page to file: $deletionResult")
                return
            }

            val imageBitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(filePath.toString())?.asImageBitmap()
            }

            if (imageBitmap == null) {
                Timber.e("Couldn't decode received image as Bitmap. Aborting!")
                scanJobs.notifyFailed(scanJob,
                    application.getString(R.string.couldn_t_decode_received_image, jobStateString)
                )
                filePath.toFile().delete()
                val deletionResult = jobResult.cancel()
                Timber.d("Cancelling job after error while trying to decode received page as bitmap: $deletionResult")
                return
            }
            addScan(scanJob.ownerSessionId, filePath.toString(), currentScanSettings, ScanRelativeRotation.Original)
        }
    }

    suspend fun addScan(sessionID: Uuid, path: String, settings: ScanSettings, rotation: ScanRelativeRotation) {
        Timber.d("Adding scan: $path, $rotation")
        db.withTransaction {
            val highestIdx = scannedPageDao.getHighestIdxForSession(sessionID) ?: -1

            Timber.d("Inserting scan with index ${highestIdx + 1}")
            scannedPageDao.insertAll(ScannedPage(Uuid.generateV4(),
                sessionID,
                path,
                settings,
                rotation,
                highestIdx + 1
            ))
        }
    }

    private suspend fun abortIfCancelling(scanJob: io.github.chrisimx.esclkt.ScanJob? = null): Boolean = if (scanJobs.shouldCancel.value) {
        Timber.d("Scan job cancelling is set. Aborting, canceling job if possible. scanJob: $scanJob")
        scanJob?.cancel()

        scanJobs.setCancel(false)
        true
    } else {
        false
    }

    private suspend fun reportErrorWhileScanning(
        scanJob: ScanJob,
        nextPage: ESCLRequestClient.ScannerNextPageResult,
        jobResult: io.github.chrisimx.esclkt.ScanJob
    ) {
        Timber.e("Error while retrieving next page: $nextPage")
        scanJobs.notifyFailed(scanJob, nextPage.toString())
        val deletionResult = jobResult.cancel()
        Timber.d("Cancelling job after error while trying to retrieve page: $deletionResult")
    }
}
