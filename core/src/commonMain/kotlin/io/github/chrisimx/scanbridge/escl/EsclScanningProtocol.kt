package io.github.chrisimx.scanbridge.escl

import io.github.chrisimx.esclkt.ESCLHttpCallResult
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.JobState
import io.github.chrisimx.esclkt.ScanJob
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.scanbridge.model.ScanProtocolScannedPage
import io.github.chrisimx.scanbridge.model.ScannerHandle
import io.github.chrisimx.scanbridge.model.ScanningError
import io.github.chrisimx.scanbridge.model.UrlScannerHandle
import io.github.chrisimx.scanbridge.model.toHttpClientConfig
import io.github.chrisimx.scanbridge.ports.HttpClientFactory
import io.github.chrisimx.scanbridge.ports.MdnsDiscoverService
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.ports.ScanJobProcessingEvent
import io.github.chrisimx.scanbridge.ports.ScannerCapabilitiesResult
import io.github.chrisimx.scanbridge.ports.ScannerConnectionSettings
import io.github.chrisimx.scanbridge.ports.ScannerDiscoveryBackend
import io.github.chrisimx.scanbridge.ports.ScanningProtocol
import io.ktor.http.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
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

        val httpConfig = settings.toHttpClientConfig()
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

    private enum class PollResult {
        Abort,
        JobFinished,
        ImagesReady,
        TimedOut
    }

    private suspend fun pollUntilImagesReady(
        jobResult: ScanJob,
        abortIfCancelling: suspend (ScanJob?) -> Boolean,
        emit: suspend (ScanJobProcessingEvent) -> Unit
    ): PollResult {
        for (retries in 0..60) {
            if (abortIfCancelling(jobResult)) {
                return PollResult.Abort
            }

            val status = jobResult.getJobStatus()
            val isRunning =
                status?.jobState == JobState.Processing ||
                    status?.jobState == JobState.Pending

            val imagesToTransfer = status?.imagesToTransfer

            _logger.debug {
                "Polling job status. Retry: $retries Result: $status imagesToTransfer: $imagesToTransfer isRunning: $isRunning"
            }

            if (!isRunning) {
                _logger.debug { "Job is reported to be not running anymore. jobRunning = false" }

                val deleteResult = jobResult.cancel()
                _logger.debug { "Cancelling job after (a likely) failure: $deleteResult" }

                if (status?.jobState != JobState.Completed) {
                    _logger.warn { "Job info doesn't indicate completion: ${status?.jobState}" }
                    emit(ScanJobProcessingEvent.Failure(
                        ScanningError.JobCompletedInFailedState(status.toString())
                    ))
                }

                return PollResult.JobFinished
            }

            if (imagesToTransfer != null && imagesToTransfer > 0u) {
                _logger.debug { "There seem to be images to transfer. Breaking out of polling loop" }
                return PollResult.ImagesReady
            }

            delay(1000)
        }

        return PollResult.TimedOut
    }

    override fun executeScanJob(
        handle: ScannerHandle,
        settings: ScannerConnectionSettings,
        jobScanSettings: ScanSettings,
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

        _logger.debug { "Creating scan job. eSCLKt scan settings: $jobScanSettings" }

        val job = esclRequestClient.createJob(jobScanSettings)

        _logger.debug { "Creation request done. Result: $job" }
        if (job !is ESCLRequestClient.ScannerCreateJobResult.Success) {
            _logger.error { "Job creation failed. Result: $job" }

            emit(
                ScanJobProcessingEvent.Failure(ScanningError.JobCreationFailed(job.toString()))
            )
            return@flow
        }
        val jobResult = job.scanJob

        if (abortIfCancelling(jobResult)) return@flow

        var polling = false

        while (true) {
            if (polling) {
                val pollResult = pollUntilImagesReady(jobResult, ::abortIfCancelling, ::emit)

                when (pollResult) {
                    PollResult.Abort,
                    PollResult.JobFinished -> return@flow

                    PollResult.ImagesReady -> {
                        // continue with transfer logic
                    }

                    PollResult.TimedOut -> {
                        emit(ScanJobProcessingEvent.Failure(ScanningError.PollingTimedOut))
                        return@flow
                    }
                }
            }

            if (abortIfCancelling(jobResult)) return@flow

            _logger.debug { "Retrieving next page" }
            val nextPage = jobResult.retrieveNextPage()
            _logger.debug { "Next page result: $nextPage" }
            val status = jobResult.getJobStatus()
            _logger.debug { "Retrieved job info: $status" }
            // val jobStateString = status?.jobState.toJobStateString(application)
            when (nextPage) {
                is ESCLRequestClient.ScannerNextPageResult.NoFurtherPages -> {
                    _logger.debug { "Next page result is seen as no further pages. jobRunning = false" }

                    if (status?.jobState != JobState.Completed) {
                        _logger.warn { "Job info doesn't indicate completion: $status" }

                        emit(ScanJobProcessingEvent.Failure(
                            ScanningError.NextPageRetrievalError(nextPage.toString(), status.toString())
                        ))
                    }
                    val deletionResult = jobResult.cancel()
                    _logger.debug { "Cancelling job after no further pages is reported: $deletionResult" }
                    return@flow
                }

                is ESCLRequestClient.ScannerNextPageResult.RequestFailure -> {
                    if (nextPage.exception !is ESCLHttpCallResult.Error.HttpError) {
                        _logger.error { "Error while retrieving next page: $nextPage" }
                        emit(ScanJobProcessingEvent.Failure(
                            ScanningError.NextPageRetrievalError(nextPage.toString(), status.toString())
                        ))
                        return@flow
                    }
                    val error = nextPage.exception as ESCLHttpCallResult.Error.HttpError

                    if (status?.jobState == JobState.Completed) {
                        _logger.debug { "Job info indicates completion but response was not 404: $status" }

                        emit(ScanJobProcessingEvent.Failure(
                            ScanningError.NextPageRetrievalError(nextPage.toString(), status.toString())
                        ))

                        val deletionResult = jobResult.cancel()
                        _logger.debug { "Cancelling job after non-standard completion: $deletionResult" }
                        return@flow
                    } else {
                        _logger.error { "Not successful code while retrieving next page: $nextPage" }
                        if (error.code == 503) {
                            // Retry with polling
                            _logger.debug { "503 error received. Retrying with polling" }
                            polling = true
                            continue
                        } else {
                            emit(ScanJobProcessingEvent.Failure(
                                ScanningError.NextPageRetrievalError(nextPage.toString(), status.toString())
                            ))
                            val deletionResult = jobResult.cancel()
                            _logger.debug {
                                "Cancelling job after not successful response while trying to retrieve page: $deletionResult"
                            }
                            return@flow
                        }
                    }
                }

                is ESCLRequestClient.ScannerNextPageResult.Success -> {
                    _logger.debug { "Received page." }

                    val esclScannedPage = nextPage.page
                    val correspondingScannedPage = ScanProtocolScannedPage(
                        esclScannedPage.contentType,
                        esclScannedPage.data
                    )

                    emit(ScanJobProcessingEvent.NewPage(correspondingScannedPage))
                }

                else -> {
                    jobResult.cancel()
                    emit(ScanJobProcessingEvent.Failure(
                        ScanningError.NextPageRetrievalError(nextPage.toString(), status.toString())
                    ))
                    return@flow
                }
            }

        }
    }
}
