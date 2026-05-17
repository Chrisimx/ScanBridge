package io.github.chrisimx.scanbridge.model

import io.ktor.http.Url

data class DiscoveredScanner(
    val name: String,
    val iconUrl: Url?,
    val handle: ScannerHandle
)
