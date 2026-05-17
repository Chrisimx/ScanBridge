package io.github.chrisimx.scanbridge.scannerdiscovery

import io.github.chrisimx.scanbridge.model.DiscoveredScanner
import io.github.chrisimx.scanbridge.ports.ScannerConnectionSettings
import io.github.chrisimx.scanbridge.ports.ScanningProtocolManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class DiscoveryUsecase(
    private val scanningProtocolManager: ScanningProtocolManager
) {
    fun discoveredScanners(
        coroutineScope: CoroutineScope,
        connectionSettings: ScannerConnectionSettings = ScannerConnectionSettings(),
    ): Flow<List<DiscoveredScanner>> {
        val discoveryBackends = scanningProtocolManager.getDiscoveryBackends(coroutineScope, connectionSettings)
        if (discoveryBackends.isEmpty()) {
            return flowOf(emptyList())
        }

        return combine(discoveryBackends.map { it.scanners }) { scannerLists ->
            scannerLists
                .flatMap { it.asIterable() }
                .distinctBy { it.handle }
        }
    }
}
