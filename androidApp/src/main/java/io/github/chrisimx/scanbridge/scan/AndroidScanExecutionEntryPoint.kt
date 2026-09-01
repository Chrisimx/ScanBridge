package io.github.chrisimx.scanbridge.scan

import android.app.Application
import io.github.chrisimx.scanbridge.androidservice.ScanJobForegroundService
import io.github.chrisimx.scanbridge.scanning.ScanExecutionEntryPoint

class AndroidScanExecutionEntryPoint(
    val application: Application
) : ScanExecutionEntryPoint {
    override fun start() {
        ScanJobForegroundService.startService(application)
    }
}
