package io.github.chrisimx.scanbridge.model

import io.github.chrisimx.esclkt.ScanSettings
import io.ktor.http.Url
import kotlin.uuid.Uuid

data class ScanJob(
    val jobID: Uuid,
    val ownerSessionId: Uuid,
    val scanSettings: ScanSettings,
    val scannerBaseUrl: Url,
    val httpClientConfig: HttpClientConfig
)
