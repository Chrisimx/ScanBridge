package io.github.chrisimx.scanbridge.model

import io.github.chrisimx.scanbridge.ports.ScannerCapabilitiesResult
import io.ktor.http.Url

data class DiscoveredScanner(
    val name: String,
    val handle: ScannerHandle,
    val scannerCaps: ScannerCapabilitiesResult? = null,
    val iconUrl: Url? = null,
)
