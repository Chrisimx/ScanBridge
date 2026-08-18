package io.github.chrisimx.scanbridge.model

import io.github.chrisimx.scanbridge.protocol.ScannerCapabilitiesResult
import io.github.chrisimx.scanbridge.protocol.ScannerConnectionSettings
import io.github.chrisimx.scanbridge.protocol.ScanningProtocol
import io.ktor.http.Url

sealed interface ScannerHandle {
    val protocol: ScanningProtocol
    val stringRepresentation: String
}

suspend fun ScannerHandle.scannerCapabilities(settings: ScannerConnectionSettings): ScannerCapabilitiesResult {
    val protocol = this.protocol
    return protocol.capabilitiesFor(this, settings)
}

data class UrlScannerHandle(override val protocol: ScanningProtocol, val url: Url) : ScannerHandle {
    override val stringRepresentation: String
        get() = url.toString()
}
