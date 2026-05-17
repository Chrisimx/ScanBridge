package io.github.chrisimx.scanbridge.scannerdiscovery

import io.github.chrisimx.scanbridge.model.DiscoveredScanner
import io.github.chrisimx.scanbridge.ports.ScannerConnectionSettings
import io.github.chrisimx.scanbridge.ports.ScanningProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent

class DiscoveryUsecase(
    private val scanningProtocols: List<ScanningProtocol>,
) {
    fun discoveredScanners(
        coroutineScope: CoroutineScope,
        connectionSettings: ScannerConnectionSettings = ScannerConnectionSettings(),
    ): Flow<List<DiscoveredScanner>> {
        val discoveryBackends =
            scanningProtocols.mapNotNull { protocol ->
                protocol.createDiscoveryBackend(coroutineScope, connectionSettings)
            }

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
