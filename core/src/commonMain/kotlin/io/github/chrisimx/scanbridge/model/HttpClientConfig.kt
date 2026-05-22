package io.github.chrisimx.scanbridge.model

import io.github.chrisimx.scanbridge.ports.ScannerConnectionSettings

data class HttpClientConfig(
    val disableCertValidation: Boolean,
    val debugLogging: Boolean,
    val connectTimeoutInSeconds: ULong,
    val socketTimeoutInSeconds: ULong,
    val requestTimeoutInSeconds: ULong
)

fun ScannerConnectionSettings.toHttpClientConfig(): HttpClientConfig = HttpClientConfig(
    this.allowSelfSignedCertificates,
    this.debugLogging,
    this.connectionTimeoutInSeconds,
    this.connectionTimeoutInSeconds,
    this.totalTimeoutInSeconds,
)
