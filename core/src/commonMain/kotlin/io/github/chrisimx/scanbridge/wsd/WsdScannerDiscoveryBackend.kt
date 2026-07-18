package io.github.chrisimx.scanbridge.wsd

import io.github.chrisimx.scanbridge.model.DiscoveredScanner
import io.github.chrisimx.scanbridge.model.UrlScannerHandle
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.ports.ScannerDiscoveryBackend
import io.github.chrisimx.scanbridge.ports.multicast.MulticastLockHandler
import io.github.chrisimx.wsdkt.wsdiscovery.WsScannerServiceDiscovery
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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

    private val closed = AtomicBoolean(false)

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
    
    override fun close() {
        if (closed.compareAndSet(expectedValue = false, newValue = true)) {
            multicastLockHandler.release()
        }
    }

    override val scanners: StateFlow<List<DiscoveredScanner>>
        get() = wsScannerServiceDiscovery.discoveredDevices.map { scannerList ->
            scannerList.map { scannerService ->
                DiscoveredScanner(
                    name = scannerService.name,
                    handle = UrlScannerHandle(protocol, scannerService.url),
                    scannerCaps = null,
                    iconUrl = null
                )
            }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())
}
