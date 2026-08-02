package io.github.chrisimx.scanbridge.scannerdiscovery

import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import io.github.chrisimx.scanbridge.ports.CustomScannerRepository
import io.github.chrisimx.scanbridge.ports.ScanningProtocolManager
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow

data class ProtocolWithExampleHandleString(val protocolIdentifier: String, val exampleScannerIdentifierString: String)

class ScannerDiscoveryScreenViewModel(
    val customScannerRepo: CustomScannerRepository,
    val discoveryUsecase: DiscoveryUsecase,
    val protocolManager: ScanningProtocolManager
) : ViewModel() {
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
