package io.github.chrisimx.scanbridge.model

import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.scanbridge.ports.ScannerConnectionSettings
import kotlin.uuid.Uuid

data class ScanJob(
    val jobID: Uuid,
    val ownerSessionId: Uuid,
    val scanSettings: CommonScanSettings,
    val scannerHandle: ScannerHandle,
    val connectionSettings: ScannerConnectionSettings
)
