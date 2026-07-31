package io.github.chrisimx.scanbridge.usecases

import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.scanbridge.model.ScannerHandle
import io.github.chrisimx.scanbridge.ports.ScannerConnectionSettings
import kotlin.uuid.Uuid

interface StartScanUseCase {
    fun startScan(
        ownerSessionId: Uuid,
        scannerHandle: ScannerHandle,
        scanSettings: CommonScanSettings,
        connectionSettings: ScannerConnectionSettings,
    )
}
