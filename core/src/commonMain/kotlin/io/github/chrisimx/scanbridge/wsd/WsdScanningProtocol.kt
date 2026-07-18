package io.github.chrisimx.scanbridge.wsd

import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.scanbridge.model.ScannerHandle
import io.github.chrisimx.scanbridge.model.ScanningError
import io.github.chrisimx.scanbridge.model.UrlScannerHandle
import io.github.chrisimx.scanbridge.model.toHttpClientConfig
import io.github.chrisimx.scanbridge.ports.HttpClientFactory
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.ports.ScanJobProcessingEvent
import io.github.chrisimx.scanbridge.ports.ScannerCapabilitiesResult
import io.github.chrisimx.scanbridge.ports.ScannerConnectionSettings
import io.github.chrisimx.scanbridge.ports.ScannerDiscoveryBackend
import io.github.chrisimx.scanbridge.ports.ScanningProtocol
import io.github.chrisimx.scanbridge.ports.multicast.MulticastLockHandler
import io.github.chrisimx.wsdkt.anyscancompat.toCommonAbstraction
import io.github.chrisimx.wsdkt.client.WsdScanServiceClient
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow

class WsdScanningProtocol(
    private val loggerFactory: ScanBridgeLoggerFactory,
    private val httpClientFactory: HttpClientFactory,
    private val multicastLockHandler: MulticastLockHandler
) : ScanningProtocol {
    private val _logger = loggerFactory.withClass(this::class)

    override val protocolIdentifier: String
        get() = "WSD"
    override val usesUrls: Boolean
        get() = true

    override fun createScannerHandle(scannerIdentifier: String): ScannerHandle? {

        val url = runCatching { Url(scannerIdentifier) }.getOrNull()

        return url?.let {
            UrlScannerHandle(
                this,
                it
            )
        }
    }

    override fun createDiscoveryBackend(coroutineScope: CoroutineScope, settings: ScannerConnectionSettings): ScannerDiscoveryBackend =
        WsdScannerDiscoveryBackend(
            protocol = this,
            loggerFactory = loggerFactory,
            multicastLockHandler = multicastLockHandler,
            coroutineScope =  coroutineScope
        )

    override suspend fun capabilitiesFor(scanner: ScannerHandle, settings: ScannerConnectionSettings): ScannerCapabilitiesResult {
        val scannerUrlHandle =
            scanner as? UrlScannerHandle ?: return ScannerCapabilitiesResult.InvalidScannerHandle(scanner)

        val httpConfig = settings.toHttpClientConfig()
        val httpClient = httpClientFactory.create(httpConfig)

        val esclRequestClient = WsdScanServiceClient(scannerUrlHandle.url, httpClient)

        val allScannerElementsResult = httpClient.use {
            esclRequestClient.retrieveAllScannerElements()
        }

        when (allScannerElementsResult) {
            is WsdScanServiceClient.RetrieveAllScannerElementsResult.RequestFailure -> return ScannerCapabilitiesResult.Failure(
                allScannerElementsResult.error
            )
            WsdScanServiceClient.RetrieveAllScannerElementsResult.ScannerConfigurationNotFound -> return ScannerCapabilitiesResult.Failure(
                allScannerElementsResult
            ) // TODO: Map UnknownCertificate error here to the right type
            is WsdScanServiceClient.RetrieveAllScannerElementsResult.Success -> {}
        }

        val commonScanCaps = allScannerElementsResult
            .allScannerElements
            .toCommonAbstraction()

        return ScannerCapabilitiesResult.Success(commonScanCaps)
    }


    override fun executeScanJob(
        handle: ScannerHandle,
        settings: ScannerConnectionSettings,
        jobScanSettings: CommonScanSettings,
        cancelled: StateFlow<Boolean>
    ): Flow<ScanJobProcessingEvent> = flow {
        val scannerUrlHandle =
            handle as? UrlScannerHandle

        if (scannerUrlHandle == null) {
            emit(ScanJobProcessingEvent.Failure(
                    ScanningError.InvalidScanHandle(handle)
            ))
            return@flow
        }

        val httpConfig = settings.toHttpClientConfig()
        val httpClient = httpClientFactory.create(httpConfig)

        val esclRequestClient = ESCLRequestClient(scannerUrlHandle.url, httpClient)

        suspend fun abortIfCancelling(scanJob: io.github.chrisimx.esclkt.ScanJob? = null): Boolean = if (cancelled.value) {
            _logger.debug { "Scan job cancelling is set. Aborting, canceling job if possible. scanJob: $scanJob" }
            scanJob?.cancel()

            emit(ScanJobProcessingEvent.Cancelled)
            true
        } else {
            false
        }

        if (abortIfCancelling()) return@flow

    }
}
