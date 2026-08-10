package io.github.chrisimx.scanbridge.cropfeature

import com.rickclephas.kmp.observableviewmodel.ViewModel
import com.rickclephas.kmp.observableviewmodel.launch
import com.rickclephas.kmp.observableviewmodel.stateIn
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.model.Rect
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.InjectedParam

sealed interface CropState {
    data object Idle : CropState
    data object Processing : CropState
    data object Finished : CropState
}

class CropScreenViewModel(
    @InjectedParam
    val scannedPageId: Uuid,
    scanBridgeDb: ScanBridgeDb,
    val finishCropUseCase: FinishCropUseCase
) : ViewModel() {
    val scannedPagesDao = scanBridgeDb.scannedPageDao()

    private val _cropRect = MutableStateFlow(
        Rect(0f, 0f, 1f, 1f)
    )
    val cropRect = _cropRect.asStateFlow()

    val scannedPageMetadata = scannedPagesDao.getByScanIdFlowNullable(scannedPageId)

    val croppedImageFilePath = scannedPageMetadata.map {
        it?.filePath
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setCropRect(rect: Rect) {
        _cropRect.value = rect
    }

    private val _cropState = MutableStateFlow<CropState>(CropState.Idle)
    val cropState = _cropState.asStateFlow()

    fun finishCrop() = viewModelScope.launch {
        if (!_cropState.compareAndSet(
                CropState.Idle,
                CropState.Processing
            )
        ) {
            return@launch
        }

        var success = false

        try {
            val currentCropRect = cropRect.value
            val currentScannedPage = scannedPagesDao.getByScanId(scannedPageId) ?: return@launch

            success = finishCropUseCase(
                currentScannedPage,
                currentCropRect
            )
        } finally {
            if (success) {
                _cropState.value = CropState.Finished
            } else {
                _cropState.value = CropState.Idle
            }
        }
    }
}
