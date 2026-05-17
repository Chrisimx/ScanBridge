package io.github.chrisimx.scanbridge.model

import io.github.chrisimx.scanbridge.ports.ScanningProtocol
import io.ktor.http.Url

sealed interface ScannerHandle {
    val protocol: ScanningProtocol
    val stringRepresentation: String
}

data class UrlScannerHandle(
    override val protocol: ScanningProtocol,
    val url: Url
) : ScannerHandle {
    override val stringRepresentation: String
        get() = url.toString()
}
