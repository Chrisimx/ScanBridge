/*
 *     Copyright (C) 2024-2025 Christian Nagel and contributors
 *
 *     This file is part of ScanBridge.
 *
 *     ScanBridge is free software: you can redistribute it and/or modify it under the terms of
 *     the GNU General Public License as published by the Free Software Foundation, either
 *     version 3 of the License, or (at your option) any later version.
 *
 *     ScanBridge is distributed in the hope that it will be useful, but WITHOUT ANY
 *     WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 *     FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along with eSCLKt.
 *     If not, see <https://www.gnu.org/licenses/>.
 *
 *     SPDX-License-Identifier: GPL-3.0-or-later
 */

package io.github.chrisimx.scanbridge.data.ui

import com.google.protobuf.LazyStringArrayList.emptyList
import io.github.chrisimx.anyscan.Area
import io.github.chrisimx.anyscan.CommonInputSourceCaps
import io.github.chrisimx.anyscan.CommonInputSourceType
import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.anyscan.CommonScanSettingsEditor
import io.github.chrisimx.anyscan.CommonScannerCapabilities
import io.github.chrisimx.anyscan.LengthUnit
import io.github.chrisimx.anyscan.ScanRegionValue
import io.github.chrisimx.anyscan.ScanSettingParam
import io.github.chrisimx.anyscan.ScannerConcept
import io.github.chrisimx.anyscan.SettingValue
import io.github.chrisimx.anyscan.inches
import io.github.chrisimx.anyscan.millimeters
import io.github.chrisimx.scanbridge.PaperFormat
import io.github.chrisimx.scanbridge.PaperFormatProvider
import io.github.chrisimx.scanbridge.model.Locale
import io.github.chrisimx.scanbridge.model.NumberValidationResult
import io.github.chrisimx.scanbridge.model.ScanSettingsEnterableDataV1
import io.github.chrisimx.scanbridge.ports.LocaleProvider
import io.github.chrisimx.scanbridge.util.UIInputSourceType
import io.github.chrisimx.scanbridge.util.derived
import io.github.chrisimx.scanbridge.util.toDoubleLocalized
import io.github.chrisimx.scanbridge.util.toUIInputSourceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import timber.log.Timber

enum class ScanSettingsLengthUnit {
    INCH,
    MILLIMETER
}

class ScanSettingsComposableStateHolder(
    @InjectedParam
    val capabilities: StateFlow<CommonScannerCapabilities>,
    @InjectedParam
    val scanSettings: StateFlow<CommonScanSettings>,
    @InjectedParam
    private val initialScanSettingsData: ScanSettingsEnterableDataV1,
    @InjectedParam
    private val updateSettings: suspend (CommonScanSettingsEditor.() -> Unit) -> Unit,
    @InjectedParam
    private val coroutineScope: CoroutineScope,
    private val localeProvider: LocaleProvider,
    private val paperFormatProvider: PaperFormatProvider
) {

    private val _uiState = MutableStateFlow(initialScanSettingsData)
    val uiState: StateFlow<ScanSettingsEnterableDataV1> = _uiState.asStateFlow()

    val inputSourceOptions: StateFlow<List<UIInputSourceType>> = capabilities.derived(coroutineScope) { caps ->
        caps.inputSources.map {
            it.inputSourceType.toUIInputSourceType()
        }.distinct()
    }

    val selectedInputSource: StateFlow<UIInputSourceType> = scanSettings.derived(coroutineScope) {
        it.inputSource?.toUIInputSourceType() ?: inputSourceOptions.value.first()
    }

    val duplexUsed: StateFlow<Boolean> = scanSettings.derived(coroutineScope) {
        it.inputSource == CommonInputSourceType.ADF_DUPLEX
    }

    val duplexAdfSupported: StateFlow<Boolean> = capabilities.derived(coroutineScope) { caps ->
        caps.inputSources.firstOrNull {
            it.inputSourceType == CommonInputSourceType.ADF_DUPLEX
        } != null
    }

    val duplexSettingAvailable: StateFlow<Boolean> = combine(duplexAdfSupported, scanSettings) { duplexSupport, scanSettings ->
        duplexSupport &&
            (scanSettings.inputSource in setOf(CommonInputSourceType.ADF_DUPLEX, CommonInputSourceType.ADF_SIMPLEX))
    }.stateIn(coroutineScope, SharingStarted.Lazily, false)

    private val selectedInputSourceCaps: StateFlow<CommonInputSourceCaps> = combine(scanSettings, capabilities) { settings, caps ->
        caps.inputSources.first {
            it.inputSourceType == settings.inputSource
        }
    }.stateIn(
        coroutineScope,
        SharingStarted.Lazily,
        capabilities.value.inputSources.first()
    )

    private fun validateDimensionValue(valueString: String, getDimension: (Area) -> LengthUnit): NumberValidationResult {
        if (valueString.isBlank()) {
            return NumberValidationResult.NotANumber
        }

        val dimension = valueString.toDoubleLocalized() ?: return NumberValidationResult.NotANumber

        val regionParam = capabilities.value.inputSources.firstOrNull {
            it.inputSourceType == scanSettings.value.inputSource
        }?.furtherOptions?.get(ScannerConcept.ScanRegion) as? ScanSettingParam.ScanSettingRegionParam

        val lengthInUnit = when (lengthUnit.value) {
            ScanSettingsLengthUnit.INCH -> dimension.inches()
            ScanSettingsLengthUnit.MILLIMETER -> dimension.millimeters()
        }

        if (regionParam == null) {
            return NumberValidationResult.Success(lengthInUnit)
        }

        val maxDimension = toUserUnit(lengthUnit.value, getDimension(regionParam.maxArea.value))
        val minDimension = toUserUnit(lengthUnit.value, getDimension(regionParam.minArea.value))

        if (dimension !in minDimension..maxDimension) {
            return NumberValidationResult.OutOfRange(minDimension, maxDimension)
        } else {
            return NumberValidationResult.Success(lengthInUnit)
        }
    }

    val validationResultHeight: StateFlow<NumberValidationResult> = combine(uiState, capabilities) { settings, caps ->
        validateDimensionValue(settings.heightString) { it.height }
    }.stateIn(coroutineScope, SharingStarted.Lazily, NumberValidationResult.NotANumber)

    val validationResultWidth: StateFlow<NumberValidationResult> = combine(uiState, capabilities) { settings, caps ->
        validateDimensionValue(settings.widthString) { it.width }
    }.stateIn(coroutineScope, SharingStarted.Lazily, NumberValidationResult.NotANumber)

    val availableParameters = selectedInputSourceCaps.derived(coroutineScope) {
        it.furtherOptions
    }

    val availablePaperFormats: StateFlow<List<PaperFormat>> = combine(
        selectedInputSourceCaps,
        paperFormatProvider.formats
    ) { inputSourceCaps, paperFormats ->
        val regionParam = inputSourceCaps.furtherOptions[ScannerConcept.ScanRegion] as? ScanSettingParam.ScanSettingRegionParam
        if (regionParam == null) {
            emptyList<PaperFormat>()
        } else {
            val maxArea = regionParam.maxArea.value
            val minArea = regionParam.minArea.value

            val maxAreaWithTol = maxArea + 0.1.millimeters()
            val minAreaWithTol = minArea - 0.1.millimeters()

            paperFormats
                .filter { paperFormat ->
                    paperFormat.area in maxAreaWithTol && minAreaWithTol in paperFormat.area
                }
        }
    }.stateIn(coroutineScope, SharingStarted.Lazily, emptyList<PaperFormat>())

    val lengthUnit = localeProvider.locale.derived(coroutineScope) {
        unitByLocale(it)
    }

    private fun unitByLocale(locale: Locale): ScanSettingsLengthUnit = if (locale.country in setOf("US", "LR", "MM")) {
        ScanSettingsLengthUnit.INCH
    } else {
        ScanSettingsLengthUnit.MILLIMETER
    }

    private fun toUserUnit(unit: ScanSettingsLengthUnit, length: LengthUnit): Double = when (unit) {
        ScanSettingsLengthUnit.INCH -> length.toInches().value
        ScanSettingsLengthUnit.MILLIMETER -> length.toMillimeters().value
    }

    init {
        combine(validationResultHeight, validationResultWidth, _uiState) { height, width, ui ->
            Triple(height, width, ui.customMenuEnabled)
        }.mapNotNull { (height, width, customMenuEnabled) ->
            if (customMenuEnabled && height is NumberValidationResult.Success && width is NumberValidationResult.Success) {
                height to width
            } else {
                null
            }
        }.onEach { (height, width) ->
            updateSettings {
                this.set(
                    ScannerConcept.ScanRegion,
                    ScanRegionValue(
                        Area(height.value, width.value)
                    )
                )
            }
        }.launchIn(coroutineScope)

        _uiState
            .map { it.maximumSize }
            .distinctUntilChanged()
            .combine(selectedInputSourceCaps) { maxSize, inputSourceCaps -> Pair(maxSize, inputSourceCaps) }
            .filter { it.first }
            .onEach { (maxSize, inputSourceCaps) ->
                Timber.d("Maximum size flag set to $maxSize: This means we should set scanRegion to maximum")
                val regionParam = inputSourceCaps
                    .furtherOptions[ScannerConcept.ScanRegion] as ScanSettingParam.ScanSettingRegionParam?
                regionParam?.maxArea?.let { maxArea ->
                    updateSettings {
                        set(ScannerConcept.ScanRegion, maxArea)
                    }
                }
            }.launchIn(coroutineScope)
    }

    fun setDuplex(duplex: Boolean) {
        val duplexCurrentlyActive = duplexSettingAvailable.value

        if (duplex && !duplexCurrentlyActive) {
            Timber.d("Duplex can not be turned on because it is not available. Current duplex state: $duplexCurrentlyActive")
            return
        }

        val newInputSource = if (duplex) {
            CommonInputSourceType.ADF_DUPLEX
        } else {
            CommonInputSourceType.ADF_SIMPLEX
        }

        coroutineScope.launch {
            updateSettings {
                setInputSource(inputSource = newInputSource)
            }
        }
    }

    fun setInputSource(inputSource: UIInputSourceType) {
        Timber.d("Input Source being set to $inputSource")

        coroutineScope.launch {
            updateSettings {
                val newInputSource = when (inputSource) {
                    UIInputSourceType.PLATEN -> CommonInputSourceType.PLATEN

                    UIInputSourceType.ADF -> if (this.inputSource == CommonInputSourceType.ADF_DUPLEX) {
                        CommonInputSourceType.ADF_DUPLEX
                    } else {
                        CommonInputSourceType.ADF_SIMPLEX
                    }
                }
                setInputSource(inputSource = newInputSource)
            }
        }
    }

    fun <T : SettingValue> setSetting(concept: ScannerConcept<T>, value: Any?) {
        coroutineScope.launch {
            updateSettings {
                if (value == null) {
                    remove(concept)
                } else {
                    @Suppress("UNCHECKED_CAST")
                    set(concept, value as T)
                }
            }
        }
    }

    fun setCustomMenuEnabled(enabled: Boolean) {
        _uiState.update { it.copy(customMenuEnabled = enabled) }
    }

    fun setFormat(paperFormat: PaperFormat) {
        val area = Area(
            height = paperFormat.height,
            width = paperFormat.width
        )
        _uiState.update { it.copy(maximumSize = false, customMenuEnabled = false) }

        coroutineScope.launch {
            updateSettings {
                set(ScannerConcept.ScanRegion, ScanRegionValue(area))
            }
        }
    }

    fun setCustomWidthTextFieldContent(content: String) {
        _uiState.update {
            it.copy(widthString = content)
        }
    }

    fun setCustomHeightTextFieldContent(content: String) {
        _uiState.update {
            it.copy(heightString = content)
        }
    }

    fun selectMaxRegion() {
        _uiState.update {
            it.copy(maximumSize = true, customMenuEnabled = false)
        }
    }
}
