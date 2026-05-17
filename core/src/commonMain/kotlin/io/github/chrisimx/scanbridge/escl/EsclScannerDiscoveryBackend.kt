package io.github.chrisimx.scanbridge.escl

import io.github.chrisimx.scanbridge.model.DiscoveredScanner
import io.github.chrisimx.scanbridge.model.IpAddress
import io.github.chrisimx.scanbridge.model.MdnsService
import io.github.chrisimx.scanbridge.model.UrlScannerHandle
import io.github.chrisimx.scanbridge.ports.MdnsDiscoverService
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.ports.ScannerCapabilitiesResult
import io.github.chrisimx.scanbridge.ports.ScannerConnectionSettings
import io.github.chrisimx.scanbridge.ports.ScannerDiscoveryBackend
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import io.ktor.http.encodedPath
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.InjectedParam

sealed class Optional<out T> {
    data class Some<out T>(val value: T) : Optional<T>()
    object None : Optional<Nothing>()
}

class EsclScannerDiscoveryBackend(
    val mdnsDiscoverySecureEscl: MdnsDiscoverService,
    val mdnsDiscoveryInsecureEscl: MdnsDiscoverService,
    val loggerFactory: ScanBridgeLoggerFactory,
    @InjectedParam
    val esclScanningProtocol: EsclScanningProtocol,
    @InjectedParam
    val coroutineScope: CoroutineScope
) : ScannerDiscoveryBackend {
    private val _logger = loggerFactory.withClass(this::class)

    private val SECURE_SCANNER_DISCOVER_TYPE = "_uscans._tcp"
    private val INSECURE_SCANNER_DISCOVER_TYPE = "_uscan._tcp"

    private val isScannerReachableMap = mutableMapOf<String, Optional<ScannerCapabilitiesResult>>()
    private val isScannerReachableMapMutex = Mutex()

    private val capabilityFetchDispatcher = Dispatchers.IO.limitedParallelism(8)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _scanners: StateFlow<List<DiscoveredScanner>> =
        combine(
            mdnsDiscoveryInsecureEscl.foundServices,
            mdnsDiscoverySecureEscl.foundServices,
        ) { insecure, secure ->
            insecure.values + secure.values
        }
            .flatMapLatest { allFoundServices ->
                discoveredScannersFlow(allFoundServices)
            }
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList(),
            )

    override val scanners: StateFlow<List<DiscoveredScanner>>
        get() = _scanners


    init {
        check(mdnsDiscoverySecureEscl !== mdnsDiscoveryInsecureEscl) {
            "The MdnsDiscoverServices for secure and insecure eSCL must be different instances"
        }

        mdnsDiscoverySecureEscl.start(SECURE_SCANNER_DISCOVER_TYPE)
        mdnsDiscoveryInsecureEscl.start(INSECURE_SCANNER_DISCOVER_TYPE)

        coroutineScope.launch {
            try {
                awaitCancellation() // Close the discovery backend when the coroutine is canceled
            } finally {
                close()
            }
        }
    }

    private fun mdnsServicesToDiscoveredScanners(
        mdnsServices: List<MdnsService>,
    ): List<DiscoveredScanner> {
        return mdnsServices.flatMap { mdnsService ->
            val scannerName = mdnsService.serviceName
            var rs = mdnsService.txtAttributes["rs"]?.decodeToString() ?: "/"
            val iconUrlString = mdnsService.txtAttributes["representation"]?.decodeToString()

            rs = if (rs.isEmpty()) "/" else "/$rs/"

            val iconUrl = iconUrlString?.toNullableUrl()
            val iconUrlWithResolvedIp = iconUrl?.let { icoUrl ->
                URLBuilder(icoUrl)
                    .apply {
                        host = mdnsService.addresses.firstOrNull()?.urlHost ?: icoUrl.host
                    }
                    .build()
            }

            val scannerUrls = mdnsService.addresses.mapNotNull { address ->
                tryParseScannerUrl(address, mdnsService, rs)
            }

            scannerUrls.map { url ->
                val scannerHandle = UrlScannerHandle(esclScanningProtocol, url)

                DiscoveredScanner(
                    scannerName,
                    scannerHandle,
                    null,
                    iconUrlWithResolvedIp,
                )
            }
        }
    }

    private fun String.toNullableUrl(): Url? {
        return runCatching {
            Url(this)
        }.getOrNull()
    }

    private fun discoveredScannersFlow(
        mdnsServices: List<MdnsService>,
    ): Flow<List<DiscoveredScanner>> = channelFlow {
        val discoveredScanners = mdnsServicesToDiscoveredScanners(mdnsServices)
            .distinctBy { it.handle.stringRepresentation }

        send(emptyList())

        discoveredScanners.forEach { scanner ->
            launch(capabilityFetchDispatcher) {
                _logger.debug {
                    "Checking reachability of ${scanner.handle.stringRepresentation}"
                }


                val cachedReachability = isScannerReachableMapMutex.withLock {
                    isScannerReachableMap[scanner.handle.stringRepresentation]
                }

                when (cachedReachability) {
                    Optional.None -> return@launch

                    is Optional.Some<ScannerCapabilitiesResult> -> {
                        send(listOf(scanner.copy(scannerCaps = cachedReachability.value)))
                        return@launch
                    }

                    null -> Unit
                }

                val scannerCapabilitiesResult = checkReachabilityAndGetScannerCaps(
                    scanner = scanner,
                    connectionTimeoutSeconds = 20u,
                    totalTimeoutSeconds = 20u,
                )

                isScannerReachableMapMutex.withLock {
                    isScannerReachableMap[scanner.handle.stringRepresentation] = scannerCapabilitiesResult
                        ?.let { Optional.Some(it) }
                        ?: Optional.None
                }

                val discoveredScannerWithCaps = scanner.copy(scannerCaps = scannerCapabilitiesResult)

                if (scannerCapabilitiesResult != null) {
                    send(listOf(discoveredScannerWithCaps))
                }
            }
        }
    }
        .runningFold(emptyList()) { current, newlyReachable ->
            (current + newlyReachable)
                .distinctBy { it.handle.stringRepresentation }
        }

    private suspend fun checkReachabilityAndGetScannerCaps(
        scanner: DiscoveredScanner,
        connectionTimeoutSeconds: ULong,
        totalTimeoutSeconds: ULong,
    ): ScannerCapabilitiesResult? {
        val settings = ScannerConnectionSettings(
            connectionTimeoutInSeconds = connectionTimeoutSeconds,
            totalTimeoutInSeconds = totalTimeoutSeconds,
            debugLogging = true,
            allowSelfSignedCertificates = true,
        )

        val result = measureTimedValue {
            esclScanningProtocol.capabilitiesFor(scanner.handle, settings)
        }
        _logger.debug {
            "Scanner capabilities for ${scanner.handle.stringRepresentation} took ${result.duration}"
        }

        return when (result.value) {
            is ScannerCapabilitiesResult.Failure,
            is ScannerCapabilitiesResult.InvalidScannerHandle -> {
                _logger.debug { "Scanner ${scanner.handle.stringRepresentation} is not reachable. Result ${result.value}" }
                null
            }

            else -> result.value
        }
    }

    private fun tryParseScannerUrl(address: IpAddress, serviceInfo: MdnsService, rs: String): Url? {
        if (address.isLinkLocal()) {
            _logger.debug { "Ignoring link local address: ${address.text}, url text rep: ${address.urlHost}" }
            return null
        }

        val isSecure = serviceInfo.serviceType == SECURE_SCANNER_DISCOVER_TYPE

        return try {
            val result = URLBuilder().apply {
                protocol = if (isSecure) URLProtocol.HTTPS else URLProtocol.HTTP
                host = address.urlHost
                port = serviceInfo.port
                encodedPath = rs
            }.build()
            Url(result.toString()) // Try to parse it to confirm that no invalid URLs will be shown
            result
        } catch (e: Exception) {
            _logger.error { "Couldn't built address from: ${address.urlHost} Exception: $e" }
            null
        }
    }


    override fun close() {
        mdnsDiscoverySecureEscl.close()
        mdnsDiscoveryInsecureEscl.close()
    }
}
