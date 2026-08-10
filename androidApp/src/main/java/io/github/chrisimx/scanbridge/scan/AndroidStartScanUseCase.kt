package io.github.chrisimx.scanbridge.scan

import android.app.Application
import io.github.chrisimx.anyscan.ColorMode
import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.anyscan.FileFormat
import io.github.chrisimx.anyscan.ScannerConcept
import io.github.chrisimx.enumorrawcodegen.asEnumOrRaw
import io.github.chrisimx.scanbridge.androidservice.ScanJobForegroundService
import io.github.chrisimx.scanbridge.model.ScanJob
import io.github.chrisimx.scanbridge.model.ScannerHandle
import io.github.chrisimx.scanbridge.ports.ScannerConnectionSettings
import io.github.chrisimx.scanbridge.services.ScanJobRepository
import io.github.chrisimx.scanbridge.usecases.StartScanUseCase
import kotlin.uuid.Uuid

class AndroidStartScanUseCase(val scanJobRepo: ScanJobRepository, val application: Application) : StartScanUseCase {
    override fun startScan(
        ownerSessionId: Uuid,
        scannerHandle: ScannerHandle,
        scanSettings: CommonScanSettings,
        connectionSettings: ScannerConnectionSettings
    ) {
        val scanJob = ScanJob(
            Uuid.generateV4(),
            ownerSessionId,
            scanSettings.applyFileFormat(),
            scannerHandle,
            connectionSettings
        )

        scanJobRepo.enqueue(scanJob)
        ScanJobForegroundService.startService(application)
    }

    /**
     * Decides the correct file format for the scan and returns the modified [CommonScanSettings]
     * with this file format.
     *
     * @return Modified scan settings with the correct file format selected
     */
    private fun CommonScanSettings.applyFileFormat(): CommonScanSettings {
        val isBlackAndWhite = this.setting[ScannerConcept.ColorMode]
            ?.value == ColorMode.BlackAndWhite1.toString()

        val format = if (isBlackAndWhite) FileFormat.PDF else FileFormat.JPEG

        return this.copy(
            format = format.asEnumOrRaw()
        )
    }
}
