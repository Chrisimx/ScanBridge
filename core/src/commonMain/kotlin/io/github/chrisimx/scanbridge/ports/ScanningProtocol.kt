package io.github.chrisimx.scanbridge.ports

import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.scanbridge.model.ScannerHandle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

data class ScannerConnectionSettings(
    val connectionTimeoutInSeconds: ULong = 10uL,
    val totalTimeoutInSeconds: ULong = 10uL,
    val allowSelfSignedCertificates: Boolean = false,
    val debugLogging: Boolean = false,
)

sealed class ScannerCapabilitiesResult {
    data class Success(val scannerCapabilities: ScannerCapabilities) : ScannerCapabilitiesResult()
    data class ScannerCapsFormatInvalid(val error: Exception, val scannerCapsContent: String?) : ScannerCapabilitiesResult()
    data class UntrustedCertificate(val error: String?) : ScannerCapabilitiesResult()
    data class InvalidScannerHandle(val scannerHandle: ScannerHandle) : ScannerCapabilitiesResult()
    data class InternalBug(val exception: Any) : ScannerCapabilitiesResult()
    data class Failure(val reason: Any) : ScannerCapabilitiesResult()
}

sealed class ScanJobProcessingEvent {
    data class Update(val progress: Double) : ScanJobProcessingEvent()
    data class Success(val scannedPages: ESCLRequestClient.ScannedPage) : ScanJobProcessingEvent()
    data class Failure(val reason: Any) : ScanJobProcessingEvent()
}

interface ScanningProtocol {
    /**
     * A string that uniquely identifies this protocol (e.g. "eSCL", "WSD", ...)
     */
    val protocolIdentifier: String

    /**
     * Whether this scanning protocol uses URLs for identifying a scanner.
     * This flag is needed so that it can be decided for which protocols the manual URL entry
     * feature is allowed.
     */
    val usesUrls: Boolean

    /**
     * Creates a [ScannerHandle] for the given scanner identifier. This handle can be used for the rest of the API.
     *
     * The meaning of the string [scannerIdentifier] is protocol dependent. It could be a URL,
     * a PCI device number or something else. This is left free to allow support for protocols that
     * do not use URLs.
     * Within a [ScanningProtocol] this identifier should have
     * consistent meaning.
     */
    fun createScannerHandle(scannerIdentifier: String): ScannerHandle?

    /**
     * Creates a [ScannerDiscoveryBackend] that can be used to find scanners providing
     * this scanning protocol.
     *
     * If the protocol does not support automatic discovery, this can return null.
     */
    fun createDiscoveryBackend(coroutineScope: CoroutineScope, settings: ScannerConnectionSettings): ScannerDiscoveryBackend?

    /**
     * Retrieves the capabilities of a scanner identified by the given string.
     */
    suspend fun capabilitiesFor(scanner: ScannerHandle, settings: ScannerConnectionSettings): ScannerCapabilitiesResult

    /**
     * Executes the scan job on the given scanner.
     *
     * Each process update is provided as a [ScanJobProcessingEvent]. This also provides
     * the scanned pages if the scanning job succeeds.
     */
    fun executeScanJob(handle: ScannerHandle, settings: ScannerConnectionSettings, jobScanSettings: ScanSettings): Flow<ScanJobProcessingEvent>
}
