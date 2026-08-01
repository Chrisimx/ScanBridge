package io.github.chrisimx.scanbridge.ports

import io.github.chrisimx.scanbridge.model.DiscoveredScanner
import kotlinx.coroutines.flow.Flow

interface ScannerDiscoveryBackend : AutoCloseable {
    val scanners: Flow<List<DiscoveredScanner>>
}
