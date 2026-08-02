package io.github.chrisimx.scanbridge.wsd

import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.scanbridge.model.ScanProtocolScannedPage
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
import io.github.chrisimx.wsdkt.anyscancompat.toWSDScanTicket
import io.github.chrisimx.wsdkt.client.WsdScanServiceClient
import io.github.chrisimx.wsdkt.scanjob.ScanJob
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
    private val logger = loggerFactory.withClass(this::class)

    override val protocolIdentifier: String
        get() = "WSD"
    override val usesUrls: Boolean
        get() = true
    override val exampleScannerIdentifierString: String
        get() = "http://192.168.178.122/WebServices/ScannerService"

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
            coroutineScope = coroutineScope
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
            )

            // TODO: Map UnknownCertificate error here to the right type
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
            emit(
                ScanJobProcessingEvent.Failure(
                    ScanningError.InvalidScanHandle(handle)
                )
            )
            return@flow
        }

        val httpConfig = settings.toHttpClientConfig()
        val httpClient = httpClientFactory.create(httpConfig)

        val wsdRequestClient = WsdScanServiceClient(scannerUrlHandle.url, httpClient)

        suspend fun abortIfCancelling(scanJob: ScanJob? = null): Boolean = if (cancelled.value) {
            logger.debug { "Scan job cancelling is set. Aborting, canceling job if possible. scanJob: $scanJob" }
            scanJob?.cancel()

            emit(ScanJobProcessingEvent.Cancelled)
            true
        } else {
            false
        }

        if (abortIfCancelling()) return@flow

        val caps = wsdRequestClient.retrieveAllScannerElements()

        if (caps !is WsdScanServiceClient.RetrieveAllScannerElementsResult.Success) {
            emit(
                ScanJobProcessingEvent.Failure(
                    ScanningError.CannotGetScannerCaps(caps.toString())
                )
            )
            return@flow
        }

        val scanTicket = jobScanSettings.toWSDScanTicket(
            caps.allScannerElements.scannerConfiguration
        )

        val jobCreationResult = wsdRequestClient.createScanJob(scanTicket)

        if (jobCreationResult !is WsdScanServiceClient.CreateScanJobResult.Success) {
            emit(
                ScanJobProcessingEvent.Failure(
                    ScanningError.JobCreationFailed(jobCreationResult.toString())
                )
            )
            return@flow
        }

        val job = jobCreationResult.scanJob

        if (abortIfCancelling(job)) return@flow

        while (true) {
            if (abortIfCancelling(job)) return@flow

            val newPageResult = job.retrieveNextPage()
            when (newPageResult) {
                WsdScanServiceClient.RetrieveImageResult.NoFurtherPages -> {
                    break
                }

                is WsdScanServiceClient.RetrieveImageResult.Success -> {}

                else -> {
                    job.cancel()
                    emit(
                        ScanJobProcessingEvent.Failure(
                            ScanningError.NextPageRetrievalError(newPageResult.toString(), "null")
                        )
                    )
                    return@flow
                }
            }

            val newPage = newPageResult.page

            if (newPage.contentType == null) {
                job.cancel()
                emit(
                    ScanJobProcessingEvent.Failure(
                        ScanningError.NextPageRetrievalError("Content type missing", "null")
                    )
                )
                return@flow
            }

            emit(
                ScanJobProcessingEvent.NewPage(
                    ScanProtocolScannedPage(
                        newPage.contentType!!,
                        newPage.data
                    )
                )
            )
        }
    }
}
