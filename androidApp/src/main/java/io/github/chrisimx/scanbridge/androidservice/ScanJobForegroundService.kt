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
import io.github.chrisimx.scanbridge.MainActivity
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.scanning.ScanExecutor
import io.github.chrisimx.scanbridge.scanning.ScanJobRepository
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

    @Volatile
    private var isRunning = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val scanJobs: ScanJobRepository by inject()

    private val scanExecutor: ScanExecutor by inject()

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
                scanExecutor.executeScans()
            } finally {
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
}
