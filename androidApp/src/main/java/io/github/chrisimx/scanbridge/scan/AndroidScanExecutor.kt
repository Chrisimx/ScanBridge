package io.github.chrisimx.scanbridge.scan

import android.app.Application
import io.github.chrisimx.scanbridge.androidservice.ScanJobForegroundService
import io.github.chrisimx.scanbridge.scanning.ScanExecutor

class AndroidScanExecutor(
    val application: Application
) : ScanExecutor {
    override fun start() {
        ScanJobForegroundService.startService(application)
    }
}
