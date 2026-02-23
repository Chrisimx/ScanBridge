package io.github.chrisimx.scanbridge.data.model

import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.ScanSettings
import kotlin.uuid.Uuid

data class ScanJob(
    val jobID: Uuid,
    val ownerSessionId: Uuid,
    val scanSettings: ScanSettings,
    val esclClient: ESCLRequestClient
)
