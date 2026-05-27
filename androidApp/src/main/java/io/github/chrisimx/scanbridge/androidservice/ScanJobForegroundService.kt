package io.github.chrisimx.scanbridge.androidservice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.scanbridge.MainActivity
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.daos.ScannedPageDao
import io.github.chrisimx.scanbridge.db.entities.ScannedPage
import io.github.chrisimx.scanbridge.model.ScanJob
import io.github.chrisimx.scanbridge.model.ScanRelativeRotation
import io.github.chrisimx.scanbridge.model.ScanningError
import io.github.chrisimx.scanbridge.ports.HttpClientFactory
import io.github.chrisimx.scanbridge.ports.ScanJobProcessingEvent
import io.github.chrisimx.scanbridge.services.ScanJobRepository
import io.github.chrisimx.scanbridge.util.extractPdfImages
import java.io.File
import kotlin.jvm.java
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

    private val httpClientFactory: HttpClientFactory by inject()

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

    override fun onBind(intent: Intent?): IBinder? = null

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
            .setContentIntent(
                Intent(this, MainActivity::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
                }
            )
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
        scanJobs.notifyStarted(scanJob)

        val scanningProtocol = scanJob.scannerHandle.protocol
        val scanningFlow = scanningProtocol.executeScanJob(
            scanJob.scannerHandle,
            scanJob.connectionSettings,
            scanJob.scanSettings,
            scanJobs.shouldCancel
        )

        var failed = false

        var pageCounter = 1

        scanningFlow.collect { processingEvent ->
            when (processingEvent) {
                ScanJobProcessingEvent.Cancelled -> scanJobs.setCancel(false)
                is ScanJobProcessingEvent.Failure -> {
                    failed = true
                    scanJobs.notifyFailed(scanJob, processingEvent.error)
                    return@collect
                }
                is ScanJobProcessingEvent.NewPage -> {
                    val pageData = processingEvent.scannedPage

                    val scanPageFileName = "scan-" + Uuid.random().toString()
                    val scanPageFile = File(application.filesDir, scanPageFileName)

                    scanPageFile.writeBytes(pageData.data)

                    when (pageData.contentType) {
                        // TODO: Extract this conversion to a service
                        "image/jpeg" -> {
                            addScan(
                                scanJob.ownerSessionId,
                                scanPageFile.absolutePath,
                                scanJob.scanSettings,
                                ScanRelativeRotation.Original,
                                "scan-${pageCounter.toString().padStart(4, '0')}.jpg"
                            )
                        }
                        "application/pdf" -> {
                            val extractedImages = extractPdfImages(
                                scanPageFile.absolutePath,
                                File(scanPageFile.parent!!)
                            )

                            extractedImages.forEach {
                                addScan(scanJob.ownerSessionId, it, scanJob.scanSettings, ScanRelativeRotation.Original)
                            }
                        }
                        else -> {
                            failed = true
                            scanJobs.notifyFailed(scanJob, ScanningError.UnsupportedContentType(pageData.contentType))
                            return@collect
                        }
                    }

                    pageCounter++
                }
            }
        }

        if (failed) return

        scanJobs.notifyCompleted(scanJob)
    }

    suspend fun addScan(sessionID: Uuid, path: String, settings: ScanSettings, rotation: ScanRelativeRotation, fileName: String? = null) {
        Timber.d("Adding scan: $path, $rotation")
        db.useWriterConnection {
            it.immediateTransaction {
                val highestIdx = scannedPageDao.getHighestIdxForSession(sessionID) ?: -1

                Timber.d("Inserting scan with index ${highestIdx + 1}")
                scannedPageDao.insertAll(
                    ScannedPage(
                        Uuid.generateV4(),
                        sessionID,
                        path,
                        settings,
                        rotation,
                        highestIdx + 1,
                        fileName
                    )
                )
            }
        }
    }
}
