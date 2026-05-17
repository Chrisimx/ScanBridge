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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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


    @OptIn(ExperimentalCoroutinesApi::class)
    override val scanners: StateFlow<List<DiscoveredScanner>>
        get() = combine(mdnsDiscoveryInsecureEscl.foundServices, mdnsDiscoverySecureEscl.foundServices) { foundServices1, foundServices2 ->
            foundServices1.values to foundServices2.values
        }.mapLatest { (foundServices1, foundServices2) ->
            val allFoundServices = foundServices1 + foundServices2
            mdnsServicesToDiscoveredScanners(allFoundServices)
        }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())


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

    private suspend fun mdnsServicesToDiscoveredScanners(mdnsServices: List<MdnsService>): List<DiscoveredScanner> {
        val scannerConnectionSettingsForTesting = ScannerConnectionSettings(
            timeoutInSeconds = 3uL,
            allowSelfSignedCertificates = true,
        )

        return mdnsServices.mapNotNull {
            val mdnsService = it
            val scannerName = mdnsService.serviceName
            var rs = mdnsService.txtAttributes["rs"]?.decodeToString() ?: "/"
            val iconUrlString = mdnsService.txtAttributes["representation"]?.decodeToString()

            rs = if (rs.isEmpty()) "/" else "/$rs/"

            val iconUrl = iconUrlString?.toNullableUrl()
            val iconUrlWithResolvedIp = iconUrl?.let { icoUrl ->
                URLBuilder(icoUrl)
                    .apply {
                        host = mdnsService.addresses.firstOrNull()?.urlHost ?: icoUrl.host
                    }.build()
            }

            val scannerUrls = mdnsService.addresses.mapNotNull { address ->
                tryParseScannerUrl(address, mdnsService, rs)
            }

            scannerUrls.mapNotNull { url ->
                val scannerHandle = UrlScannerHandle(esclScanningProtocol, url)

                val scannerCapabilitiesResult = esclScanningProtocol.capabilitiesFor(
                    scannerHandle,
                    scannerConnectionSettingsForTesting
                )

                when (scannerCapabilitiesResult) {
                    is ScannerCapabilitiesResult.Failure,
                    is ScannerCapabilitiesResult.InternalBug,
                    is ScannerCapabilitiesResult.InvalidScannerHandle -> return@mapNotNull null

                    else -> {}
                }

                DiscoveredScanner(scannerName, iconUrlWithResolvedIp, scannerHandle)
            }
        }.flatten()
    }

    private fun String.toNullableUrl(): Url? {
        return runCatching {
            Url(this)
        }.getOrNull()
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
