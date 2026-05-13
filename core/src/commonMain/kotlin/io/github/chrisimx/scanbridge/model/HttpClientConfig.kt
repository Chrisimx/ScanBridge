package io.github.chrisimx.scanbridge.model

data class HttpClientConfig(val disableCertValidation: Boolean, val debugLogging: Boolean, val timeoutInSeconds: ULong)
