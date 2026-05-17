package io.github.chrisimx.scanbridge.ports

import io.github.chrisimx.scanbridge.model.ScannerHandle
import kotlinx.coroutines.CoroutineScope

interface ScanningProtocolManager {
    fun getAllProtocols(): List<ScanningProtocol>
    fun getDiscoveryBackends(coroutineScope: CoroutineScope, connectionSettings: ScannerConnectionSettings): List<ScannerDiscoveryBackend>
    fun getProtocolFromIdentifier(identifier: String): ScanningProtocol?
    fun getScannerHandle(protocolIdentifier: String, scannerHandleStringRepresentation: String): ScannerHandle?
}
