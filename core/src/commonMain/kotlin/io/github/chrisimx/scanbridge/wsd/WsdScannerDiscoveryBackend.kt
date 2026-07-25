package io.github.chrisimx.scanbridge.wsd

import io.github.chrisimx.scanbridge.model.DiscoveredScanner
import io.github.chrisimx.scanbridge.model.UrlScannerHandle
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.ports.ScannerDiscoveryBackend
import io.github.chrisimx.scanbridge.ports.multicast.MulticastLockHandler
import io.github.chrisimx.wsdkt.wsdiscovery.WsScannerServiceDiscovery
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam


@OptIn(ExperimentalAtomicApi::class)
class WsdScannerDiscoveryBackend(
    val multicastLockHandler: MulticastLockHandler,
    @InjectedParam
    val protocol: WsdScanningProtocol,
    @InjectedParam
    val loggerFactory: ScanBridgeLoggerFactory,
    @InjectedParam
    val coroutineScope: CoroutineScope
) : ScannerDiscoveryBackend {
    private val _logger = loggerFactory.withClass(this::class)

    fun discoveryDebugLog(message: String) {
        _logger.debug { message }
    }

    val wsScannerServiceDiscovery: WsScannerServiceDiscovery = WsScannerServiceDiscovery(
        debugLog = ::discoveryDebugLog
    )

    init {
        multicastLockHandler.acquire()

        coroutineScope.launch {
            try {
                awaitCancellation()
            } finally {
                close()
            }
        }
    }

    override fun close() {}

    override val scanners: Flow<List<DiscoveredScanner>> =
        wsScannerServiceDiscovery.discoveredDevices
            .onStart {
                multicastLockHandler.acquire()
            }
            .onCompletion { cause ->
                multicastLockHandler.release()
            }.map { scannerList ->
                scannerList.map { scannerService ->
                    DiscoveredScanner(
                        name = scannerService.name,
                        handle = UrlScannerHandle(protocol, scannerService.url),
                        scannerCaps = null,
                        iconUrl = null
                    )
                }
            }
}
