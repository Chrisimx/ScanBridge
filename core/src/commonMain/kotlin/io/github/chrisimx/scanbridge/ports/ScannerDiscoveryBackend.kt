package io.github.chrisimx.scanbridge.ports

import io.github.chrisimx.scanbridge.model.DiscoveredScanner
import io.github.chrisimx.scanbridge.model.ScannerHandle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ScannerDiscoveryBackend : AutoCloseable {
    val scanners: Flow<List<DiscoveredScanner>>
}
