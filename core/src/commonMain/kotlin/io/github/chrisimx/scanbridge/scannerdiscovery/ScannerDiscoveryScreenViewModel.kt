package io.github.chrisimx.scanbridge.scannerdiscovery

import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import io.github.chrisimx.scanbridge.model.EditedCustomScanner
import io.github.chrisimx.scanbridge.model.UrlValidationResult
import io.github.chrisimx.scanbridge.logging.ScanBridgeLoggerFactory
import io.github.chrisimx.scanbridge.protocol.ScanningProtocolManager
import io.ktor.http.Url
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

data class ProtocolWithExampleHandleString(val protocolIdentifier: String, val exampleScannerIdentifierString: String)

class ScannerDiscoveryScreenViewModel(
    val customScannerRepo: CustomScannerRepository,
    val discoveryUsecase: DiscoveryUsecase,
    val protocolManager: ScanningProtocolManager,
    val loggerFactory: ScanBridgeLoggerFactory
) : ViewModel() {

    private val logger = loggerFactory.withClass(this::class)
    private val _customScanners = customScannerRepo.allFlow()

    val customScanners: StateFlow<List<CustomScanner>> = _customScanners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val discoveredScanners = discoveryUsecase.discoveredScanners(viewModelScope.coroutineScope)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    /**
     * The protocols for which we can create a custom scanner.
     */
    val protocolsForCustomScanners = protocolManager
        .getAllProtocols()
        .map { ProtocolWithExampleHandleString(it.protocolIdentifier, it.exampleScannerIdentifierString) }

    fun addScanner(scanner: CustomScanner) {
        viewModelScope.launch {
            customScannerRepo.add(scanner)
        }
    }

    fun validateUrl(urlString: String): UrlValidationResult {
        if (urlString.isEmpty()) {
            return UrlValidationResult.Empty
        }

        return try {
            UrlValidationResult.NoError(
                Url(urlString)
            )
        } catch (_: Exception) {
            UrlValidationResult.InvalidUrl
        }
    }

    data class ScannerCreationResult(
        val sessionId: Uuid,
        val customScanner: CustomScanner
    )

    fun onCustomScannerCreation(
        name: String,
        url: String,
        protocolIdentifier: String,
        save: Boolean,
        defaultScannerName: String,
        currentlyEditedScanner: EditedCustomScanner
    ): ScannerCreationResult? {
        val validationResultUrl = validateUrl(url)
        if (validationResultUrl !is UrlValidationResult.NoError) {
            logger.info {
                "Tried to create custom scanner with invalid URL: $validationResultUrl"
            }
            return null
        }

        val url = validationResultUrl.url
        val finalName = name.ifEmpty { defaultScannerName }

        val sessionID = Uuid.random()
        val uuid = when (currentlyEditedScanner) {
            is EditedCustomScanner.EditingOld -> currentlyEditedScanner.scanner.uuid
            EditedCustomScanner.New -> Uuid.random()
        }

        val newCustomScanner = CustomScanner(uuid, finalName, url, protocolIdentifier)
        if (save) {
            addScanner(newCustomScanner)
        }

        return ScannerCreationResult(
            sessionID,
            newCustomScanner
        )
    }

    suspend fun loadScannerByUuid(scanner: Uuid): CustomScanner? = customScannerRepo.getById(scanner)

    @OptIn(ExperimentalUuidApi::class)
    fun deleteScanner(scanner: CustomScanner) {
        deleteScannerByUuid(scanner.uuid)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun deleteScannerByUuid(scanner: Uuid) {
        viewModelScope.launch {
            customScannerRepo.deleteById(scanner)
        }
    }
}
