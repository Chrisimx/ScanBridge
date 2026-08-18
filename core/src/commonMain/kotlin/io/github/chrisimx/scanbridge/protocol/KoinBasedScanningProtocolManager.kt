package io.github.chrisimx.scanbridge.protocol

import io.github.chrisimx.scanbridge.model.ScannerHandle
import io.github.chrisimx.scanbridge.ports.ScannerDiscoveryBackend
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent

class KoinBasedScanningProtocolManager(private val scanningProtocols: List<ScanningProtocol>) :
    ScanningProtocolManager,
    KoinComponent {
    private val protocolById = scanningProtocols.associateBy {
        it.protocolIdentifier
    }

    override fun getAllProtocols(): List<ScanningProtocol> = scanningProtocols

    override fun getDiscoveryBackends(
        coroutineScope: CoroutineScope,
        connectionSettings: ScannerConnectionSettings
    ): List<ScannerDiscoveryBackend> = scanningProtocols.mapNotNull {
        it.createDiscoveryBackend(coroutineScope, connectionSettings)
    }

    override fun getProtocolFromIdentifier(identifier: String): ScanningProtocol? = protocolById[identifier]

    override fun getScannerHandle(protocolIdentifier: String, scannerHandleStringRepresentation: String): ScannerHandle? {
        return getProtocolFromIdentifier(protocolIdentifier)?.let { protocol ->
            return protocol.createScannerHandle(scannerHandleStringRepresentation)
        }
    }
}
