package io.github.chrisimx.scanbridge.adapters

import io.github.chrisimx.scanbridge.model.ScannerHandle
import io.github.chrisimx.scanbridge.ports.ScannerConnectionSettings
import io.github.chrisimx.scanbridge.ports.ScannerDiscoveryBackend
import io.github.chrisimx.scanbridge.ports.ScanningProtocol
import io.github.chrisimx.scanbridge.ports.ScanningProtocolManager
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent

class KoinBasedScanningProtocolManager(
    private val scanningProtocols: List<ScanningProtocol>
): ScanningProtocolManager, KoinComponent {
    private val protocolById = scanningProtocols.associateBy {
        it.protocolIdentifier
    }

    override fun getAllProtocols(): List<ScanningProtocol> {
        return scanningProtocols
    }

    override fun getDiscoveryBackends(
        coroutineScope: CoroutineScope,
        connectionSettings: ScannerConnectionSettings
    ): List<ScannerDiscoveryBackend> {
        return scanningProtocols.mapNotNull {
            it.createDiscoveryBackend(coroutineScope, connectionSettings)
        }
    }

    override fun getProtocolFromIdentifier(identifier: String): ScanningProtocol? {
        return protocolById[identifier]
    }

    override fun getScannerHandle(
        protocolIdentifier: String,
        scannerHandleStringRepresentation: String
    ): ScannerHandle? {
        return getProtocolFromIdentifier(protocolIdentifier)?.let { protocol ->
            return protocol.createScannerHandle(scannerHandleStringRepresentation)
        }
    }
}
