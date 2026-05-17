package io.github.chrisimx.scanbridge.escl

import io.github.chrisimx.esclkt.ESCLHttpCallResult
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.scanbridge.model.HttpClientConfig
import io.github.chrisimx.scanbridge.model.ScannerHandle
import io.github.chrisimx.scanbridge.model.UrlScannerHandle
import io.github.chrisimx.scanbridge.ports.HttpClientFactory
import io.github.chrisimx.scanbridge.ports.MdnsDiscoverService
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.ports.ScanJobProcessingEvent
import io.github.chrisimx.scanbridge.ports.ScannerCapabilitiesResult
import io.github.chrisimx.scanbridge.ports.ScannerDiscoveryBackend
import io.github.chrisimx.scanbridge.ports.ScanningProtocol
import io.github.chrisimx.scanbridge.ports.ScannerConnectionSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class EsclScanningProtocol(
    val mdnsDiscoverySecureEscl: MdnsDiscoverService,
    val mdnsDiscoveryInsecureEscl: MdnsDiscoverService,
    private val loggerFactory: ScanBridgeLoggerFactory,
    private val httpClientFactory: HttpClientFactory
) : ScanningProtocol {
    private val _logger = loggerFactory.withClass(this::class)

    override val protocolIdentifier: String
        get() = "eSCL"
    override val usesUrls: Boolean
        get() = true

    init {
        check(mdnsDiscoverySecureEscl !== mdnsDiscoveryInsecureEscl) {
            "The MdnsDiscoverServices for secure and insecure eSCL must be different instances"
        }
    }

    override fun createDiscoveryBackend(coroutineScope: CoroutineScope, settings: ScannerConnectionSettings): ScannerDiscoveryBackend =
        EsclScannerDiscoveryBackend(
            mdnsDiscoverySecureEscl,
            mdnsDiscoveryInsecureEscl,
            loggerFactory,
            this,
            coroutineScope
        )

    override suspend fun capabilitiesFor(scanner: ScannerHandle, settings: ScannerConnectionSettings): ScannerCapabilitiesResult {
        val scannerUrlHandle =
            scanner as? UrlScannerHandle ?: return ScannerCapabilitiesResult.InvalidScannerHandle(scanner)

        val httpConfig = deriveHttpConfigFromConnectionSettings(settings)
        val httpClient = httpClientFactory.create(httpConfig)

        val esclRequestClient = ESCLRequestClient(scannerUrlHandle.url, httpClient)

        val scannerCapsResult = httpClient.use {
            esclRequestClient.getScannerCapabilities()
        }

        return when (scannerCapsResult) {
            is ESCLRequestClient.ScannerCapabilitiesResult.InternalBug -> ScannerCapabilitiesResult.InternalBug(scannerCapsResult.exception)
            is ESCLRequestClient.ScannerCapabilitiesResult.RequestFailure -> when (val error = scannerCapsResult.error) {
                is ESCLHttpCallResult.Error.UntrustedCertificate -> ScannerCapabilitiesResult.UntrustedCertificate(error.cause)
                else -> ScannerCapabilitiesResult.Failure(error)
            }
            is ESCLRequestClient.ScannerCapabilitiesResult.ScannerCapabilitiesMalformed -> ScannerCapabilitiesResult.ScannerCapsFormatInvalid(
                scannerCapsResult.exception,
                scannerCapsResult.content
            )
            is ESCLRequestClient.ScannerCapabilitiesResult.Success -> ScannerCapabilitiesResult.Success(scannerCapsResult.scannerCapabilities)
        }
    }

    private fun deriveHttpConfigFromConnectionSettings(settings: ScannerConnectionSettings): HttpClientConfig = HttpClientConfig(
        settings.allowSelfSignedCertificates,
        settings.debugLogging,
        settings.timeoutInSeconds
    )

    override fun executeScanJob(
        handle: ScannerHandle,
        settings: ScannerConnectionSettings,
        jobScanSettings: ScanSettings
    ): Flow<ScanJobProcessingEvent> = flow {
        /*val scannerUrlHandle =
            handle as? UrlScannerHandle ?: return ScannerCapabilitiesResult.InvalidScannerHandle(scanner)

        requireNotNull(scannerUrlHandle) { "Scanner handle is not a URL handle" }

        val httpConfig = deriveHttpConfigFromConnectionSettings(settings)
        val httpClient = httpClientFactory.create(httpConfig)

        val esclRequestClient = ESCLRequestClient(scannerUrlHandle.url, httpClient)*/

        emit(ScanJobProcessingEvent.Failure(Exception("Not implemented")))
    }
}
