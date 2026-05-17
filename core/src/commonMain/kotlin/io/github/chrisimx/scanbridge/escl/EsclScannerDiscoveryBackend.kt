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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam

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

    private val isScannerReachableMap = mutableMapOf<String, Boolean>()

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
                    iconUrlWithResolvedIp,
                    scannerHandle,
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
    ): Flow<List<DiscoveredScanner>> = flow {
        val discoveredScanners = mdnsServicesToDiscoveredScanners(mdnsServices)
            .distinctBy { it.handle.stringRepresentation }

        val alreadyKnownReachable = discoveredScanners.filter { scanner ->
            isScannerReachableMap[scanner.handle.stringRepresentation] == true
        }

        emit(alreadyKnownReachable)

        val uncheckedScanners = discoveredScanners.filterNot { scanner ->
            isScannerReachableMap[scanner.handle.stringRepresentation] == true
        }

        val fastReachable = validateScanners(
            scanners = uncheckedScanners,
            connectionTimeoutSeconds = 1uL,
            totalTimeoutSeconds = 2uL,
        )

        fastReachable.forEach { scanner ->
            isScannerReachableMap[scanner.handle.stringRepresentation] = true
        }

        emit((alreadyKnownReachable + fastReachable).distinctBy { it.handle.stringRepresentation })

        val slowRetryCandidates = uncheckedScanners - fastReachable.toSet()

        val slowReachable = validateScanners(
            scanners = slowRetryCandidates,
            connectionTimeoutSeconds = 5uL,
            totalTimeoutSeconds = 5uL,
        )

        slowReachable.forEach { scanner ->
            isScannerReachableMap[scanner.handle.stringRepresentation] = true
        }

        emit(
            (alreadyKnownReachable + fastReachable + slowReachable)
                .distinctBy { it.handle.stringRepresentation }
        )
    }

    private suspend fun validateScanners(
        scanners: List<DiscoveredScanner>,
        connectionTimeoutSeconds: ULong,
        totalTimeoutSeconds: ULong,
    ): List<DiscoveredScanner> = coroutineScope {
        scanners
            .map { scanner ->
                async(capabilityFetchDispatcher) {
                    val reachable = isReachable(
                        scanner = scanner,
                        connectionTimeoutSeconds = connectionTimeoutSeconds,
                        totalTimeoutSeconds = totalTimeoutSeconds,
                    )

                    scanner.takeIf { reachable }
                }
            }
            .awaitAll()
            .filterNotNull()
    }

    private suspend fun isReachable(
        scanner: DiscoveredScanner,
        connectionTimeoutSeconds: ULong,
        totalTimeoutSeconds: ULong,
    ): Boolean {
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
                false
            }

            else -> true
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
