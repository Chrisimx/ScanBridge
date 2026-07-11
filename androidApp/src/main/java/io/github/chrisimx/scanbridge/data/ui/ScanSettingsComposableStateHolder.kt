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

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import io.github.chrisimx.anyscan.Area
import io.github.chrisimx.anyscan.AnyScanEnumOrRaw
import io.github.chrisimx.anyscan.ColorMode
import io.github.chrisimx.anyscan.CommonInputSourceCaps
import io.github.chrisimx.anyscan.CommonInputSourceType
import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.anyscan.DiscreteResolution
import io.github.chrisimx.anyscan.FileFormat
import io.github.chrisimx.anyscan.LengthUnit
import io.github.chrisimx.anyscan.ScanIntent
import io.github.chrisimx.anyscan.ThreeHundredthsOfInch
import io.github.chrisimx.anyscan.asEnumOrRaw
import io.github.chrisimx.anyscan.inches
import io.github.chrisimx.anyscan.millimeters
import io.github.chrisimx.anyscan.threeHundredthsOfInch
import io.github.chrisimx.scanbridge.R
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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import timber.log.Timber

enum class ScanSettingsLengthUnit {
    INCH,
    MILLIMETER
}

class ScanSettingsComposableStateHolder(
    @InjectedParam
    val scanSettings: StateFlow<CommonScanSettings>,
    @InjectedParam
    private val initialScanSettingsData: ScanSettingsEnterableDataV1,
    @InjectedParam
    private val updateSettings: suspend (CommonScanSettings.() -> CommonScanSettings) -> Unit,
    @InjectedParam
    private val coroutineScope: CoroutineScope,
    private val localeProvider: LocaleProvider,
    private val context: Application
) {

    private val _uiState = MutableStateFlow(initialScanSettingsData)
    val uiState: StateFlow<ScanSettingsEnterableDataV1> = _uiState.asStateFlow()

    val inputSourceOptions: StateFlow<List<UIInputSourceType>> = _uiState.derived(coroutineScope) {
        it.capabilities.inputSources.map {
            it.inputSourceType.toUIInputSourceType()
        }.distinct()
    }

    val selectedInputSource: StateFlow<UIInputSourceType> = scanSettings.derived(coroutineScope) {
        it.inputSource?.toUIInputSourceType() ?: inputSourceOptions.value.first()
    }

    val duplexUsed: StateFlow<Boolean> = scanSettings.derived(coroutineScope) {
        it.inputSource == CommonInputSourceType.ADF_DUPLEX
    }

    val duplexAdfSupported: StateFlow<Boolean> = _uiState.derived(coroutineScope) {
        it.capabilities.inputSources.firstOrNull {
            it.inputSourceType == CommonInputSourceType.ADF_DUPLEX
        } != null
    }

    val duplexCurrentlyAvailable: StateFlow<Boolean> = combine(duplexAdfSupported, scanSettings) { duplexSupport, scanSettings ->
        duplexSupport && scanSettings.inputSource == CommonInputSourceType.ADF_DUPLEX
    }.stateIn(coroutineScope, SharingStarted.Lazily, false)

    private val selectedInputSourceCaps: StateFlow<CommonInputSourceCaps> = combine(scanSettings, _uiState) { settings, uiState ->
        uiState.capabilities.inputSources.first {
            it.inputSourceType == settings.inputSource
        }
    }.stateIn(
        coroutineScope,
        SharingStarted.Lazily,
        uiState.value.capabilities.inputSources.first()
    )

    val intentOptions = selectedInputSourceCaps.derived(coroutineScope) {
        it.supportedIntents
    }

    val supportedScanResolutions = selectedInputSourceCaps.derived(coroutineScope) {
        it.supportedResolutions
    }

    val supportedColorModes = selectedInputSourceCaps.derived(coroutineScope) {
        it.supportedColorModes
    }

    val currentColorMode = scanSettings.derived(coroutineScope) {
        it.colorMode
    }

    val currentResolution: StateFlow<DiscreteResolution?> = scanSettings.derived(coroutineScope) {
        return@derived it.resolution
    }

    val lengthUnit = localeProvider.locale.derived(coroutineScope) {
        unitByLocale(it)
    }

    val currentWidthText = _uiState.derived(coroutineScope) {
        it.widthString
    }

    val currentHeightText = _uiState.derived(coroutineScope) {
        it.heightString
    }

    val currentScanRegion = scanSettings.derived(coroutineScope) {
        it.scanArea
    }

    val currentIntent = scanSettings.derived(coroutineScope) {
        it.scanIntent
    }

    val heightValidationResult = combine(currentHeightText, lengthUnit, selectedInputSourceCaps)
        { heightText, unit, inputSourceCaps ->
            val maxHeight = inputSourceCaps.maxSize.height
            val minHeight = inputSourceCaps.minSize.height
            return@combine validateCustomLengthInput(
                lengthText = heightText,
                unit = unit,
                max = maxHeight.toThreeHundredthsOfInch(),
                min = minHeight.toThreeHundredthsOfInch())
        }.stateIn(coroutineScope, SharingStarted.Lazily, NumberValidationResult.NotANumber)

    val widthValidationResult = combine(currentWidthText, lengthUnit, selectedInputSourceCaps)
        { widthText, unit, inputSourceCaps ->
            val maxWidth = inputSourceCaps.maxSize.width
            val minWidth = inputSourceCaps.minSize.width
            return@combine validateCustomLengthInput(
                lengthText = widthText,
                unit = unit,
                max = maxWidth.toThreeHundredthsOfInch(),
                min = minWidth.toThreeHundredthsOfInch())
        }.stateIn(coroutineScope, SharingStarted.Lazily, NumberValidationResult.NotANumber)

    private fun validateCustomLengthInput(
        lengthText: String,
        unit: ScanSettingsLengthUnit,
        max: ThreeHundredthsOfInch,
        min: ThreeHundredthsOfInch
    ): NumberValidationResult {
        val parsedLength = runCatching {
            lengthText.toDoubleLocalized()
        }.getOrNull()

        if (parsedLength == null) {
            return NumberValidationResult.NotANumber
        }

        val lengthInUnit = when (unit) {
            ScanSettingsLengthUnit.INCH -> parsedLength.inches()
            ScanSettingsLengthUnit.MILLIMETER -> parsedLength.millimeters()
        }

        val inputLengthInT300 = lengthInUnit.toThreeHundredthsOfInch().value

        if (inputLengthInT300 in min.value..max.value) {
            return NumberValidationResult.Success(inputLengthInT300.toDouble())
        } else {
            val maxInUserUnit = toUserUnit(unit, max)
            val minInUserUnit = toUserUnit(unit, min)

            return NumberValidationResult.OutOfRange(minInUserUnit, maxInUserUnit)
        }
    }

    private fun toUserUnit(unit: ScanSettingsLengthUnit, length: LengthUnit): Double = when (unit) {
        ScanSettingsLengthUnit.INCH -> length.toInches().value
        ScanSettingsLengthUnit.MILLIMETER -> length.toMillimeters().value
    }

    init {
        observeHeightValidation()
        observeWidthValidation()

        _uiState
            .map { it.maximumSize }
            .distinctUntilChanged()
            .combine(selectedInputSourceCaps) { maxSize, inputSourceCaps -> Pair(maxSize, inputSourceCaps) }
            .filter { it.first }
            .onEach { (maxSize, inputSourceCaps) ->
                Timber.d("Maximum size flag set to $maxSize: This means we should set scanRegion to maximum")
                updateSettings {
                    copy(
                        scanArea = inputSourceCaps.maxSize
                    )
                }
            }.launchIn(coroutineScope)
    }

    private fun observeWidthValidation() {
        widthValidationResult
            .filterIsInstance<NumberValidationResult.Success>()
            .distinctUntilChanged()
            .zip(selectedInputSourceCaps) { widthValidationResult, inputSourceCaps -> Pair(widthValidationResult, inputSourceCaps) }
            .onEach { (widthValidationResult, inputSourceCaps) ->
                updateSettings {
                    val currentScanRegion = scanArea
                    Timber.d("Width validation success result received: $widthValidationResult")

                    if (currentScanRegion == null) {
                        Timber.d("Width validation success and current scanRegion null, replacing!")
                        return@updateSettings copy(
                            scanArea = Area(
                                height = inputSourceCaps.maxSize.height,
                                width = widthValidationResult.value.threeHundredthsOfInch()
                            )
                        )
                    } else {
                        Timber.d("Width validation success and current scanRegion not null, reusing!")
                        val currentHeight = currentScanRegion.height
                        return@updateSettings copy(
                            scanArea = Area(
                                width = widthValidationResult.value.threeHundredthsOfInch(),
                                height = currentHeight
                            )
                        )
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    private fun observeHeightValidation() {
        heightValidationResult
            .onEach {
                Timber.d("Height Validation result $it")
            }
            .filterIsInstance<NumberValidationResult.Success>()
            .distinctUntilChanged()
            .zip(selectedInputSourceCaps) { heightValidationResult, inputSourceCaps -> Pair(heightValidationResult, inputSourceCaps) }
            .onEach { (heightValidationResult, inputSource) ->
                Timber.d("Height validation success result received: $heightValidationResult")
                updateSettings {
                    val currentScanRegion = scanArea

                    if (currentScanRegion == null) {
                        Timber.d("Height validation success and current scanRegion null, replacing!")
                        return@updateSettings copy(
                            scanArea = Area(
                                width = inputSource.maxSize.width,
                                height = heightValidationResult.value.threeHundredthsOfInch()
                            )
                        )
                    } else {
                        Timber.d("Height validation success and current scanRegion not null, reusing!")
                        val currentWidth = currentScanRegion.width
                        return@updateSettings copy(
                            scanArea = Area(
                                width = currentWidth,
                                height = heightValidationResult.value.threeHundredthsOfInch()
                            )
                        )
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun setDuplex(duplex: Boolean) {
        val isDuplexCurrentlyAvailable = duplexCurrentlyAvailable.value

        if (duplex && !isDuplexCurrentlyAvailable) {
            Timber.d("Duplex can not be turned on because it is not available. Current duplex state: $duplexCurrentlyAvailable")
            return
        }

        val newInputSource = if (duplex) {
            CommonInputSourceType.ADF_DUPLEX
        } else {
             CommonInputSourceType.ADF_SIMPLEX
        }

        coroutineScope.launch {
            updateSettings {
                copy(inputSource = newInputSource)
            }
        }
    }

    fun setColorMode(colorMode: AnyScanEnumOrRaw<ColorMode>?) {
        coroutineScope.launch {
            if (colorMode is AnyScanEnumOrRaw.Known && colorMode.value == ColorMode.BlackAndWhite1) {
                Timber.d("Selecting b&w. Switching to PDF format")
                updateSettings {
                    copy(colorMode = colorMode,
                        format = FileFormat.PDF.asEnumOrRaw())
                }
            } else {
                Timber.d("Selecting a color mode not b&w. Using jpeg again")
                updateSettings {
                    copy(colorMode = colorMode, format = FileFormat.JPEG.asEnumOrRaw())
                }
            }
        }
    }

    fun setInputSource(inputSource: UIInputSourceType) {
        Timber.d("Input Source being set to $inputSource. Validating existing settings.")

        val newInputSource = when (inputSource) {
            UIInputSourceType.PLATEN -> CommonInputSourceType.PLATEN
            UIInputSourceType.ADF -> CommonInputSourceType.ADF_SIMPLEX
        }

        coroutineScope.launch {
            updateSettings {
                //val currentScanSettings = scanSettings.value
                val uiState = uiState.value
                val inputSourceCaps = uiState.capabilities.getInputSourceCaps(newInputSource)!!

                val supportedResolutions = inputSourceCaps.supportedResolutions

                val xRes = this.resolution?.widthDPI
                val yRes = this.resolution?.heightDPI

                Timber.d("Input source being set. Current Resolution is: $xRes x $yRes")

                val invalidResolutionSetting = xRes != null && yRes != null &&
                    !supportedResolutions.contains(DiscreteResolution(xRes, yRes))

                val replacementResolution = if (invalidResolutionSetting) {
                    val highestScanResolution = inputSourceCaps.supportedResolutions
                        .maxBy { it.widthDPI + it.heightDPI }

                    DiscreteResolution(widthDPI = highestScanResolution.widthDPI, heightDPI =  highestScanResolution.heightDPI)
                } else {
                    if (xRes != null && yRes != null) {
                        DiscreteResolution(widthDPI = xRes, heightDPI = yRes)
                    } else {
                        null
                    }
                }

                val intentSupported = this.scanIntent?.let { inputSourceCaps.supportedIntents.contains(it) } ?: true

                val replacementIntent = if (intentSupported) {
                    this.scanIntent
                } else {
                    null
                }

                Timber.d(
                    "Input Source being set to $inputSource. " +
                        "Validated existing settings to: Res: ${replacementResolution} Intent: $replacementIntent"
                )
                copy(
                    inputSource = newInputSource,
                    resolution = replacementResolution,
                    scanIntent = replacementIntent
                )
            }
        }
    }

    fun setResolution(xResolution: UInt, yResolution: UInt) {
        coroutineScope.launch {
            updateSettings {
                copy(
                    resolution = DiscreteResolution(
                        widthDPI = xResolution,
                        heightDPI = yResolution
                    )
                )
            }
        }
    }

    fun setIntent(intent: AnyScanEnumOrRaw<ScanIntent>?) {
        coroutineScope.launch {
            updateSettings {
                copy(scanIntent = intent)
            }
        }
    }

    fun setCustomMenuEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                maximumSize = false,
                customMenuEnabled = enabled
            )
        }
    }

    fun setCustomWidthTextFieldContent(width: String) {
        check(_uiState.value.customMenuEnabled)
        _uiState.update {
            it.copy(
                maximumSize = false,
                widthString = width
            )
        }
    }

    fun setCustomHeightTextFieldContent(height: String) {
        check(_uiState.value.customMenuEnabled)
        _uiState.update {
            it.copy(
                maximumSize = false,
                heightString = height
            )
        }
    }

    private fun unitByLocale(locale: Locale): ScanSettingsLengthUnit = if (locale.country in setOf("US", "LR", "MM")) {
        ScanSettingsLengthUnit.INCH
    } else {
        ScanSettingsLengthUnit.MILLIMETER
    }

    fun selectMaxRegion() {
        _uiState.update {
            it.copy(maximumSize = true)
        }
    }

    fun setRegionDimension(newWidth: LengthUnit, newHeight: LengthUnit) {
        _uiState.update {
            it.copy(
                maximumSize = false
            )
        }
        coroutineScope.launch {
            updateSettings {
                copy(
                    scanArea = Area(
                        width = newWidth,
                        height = newHeight
                    )
                )
            }
        }
    }

    fun copySettingsToClipboard() {
        val systemClipboard =
            context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val scanSettingsString = scanSettings.toString()
        systemClipboard.setPrimaryClip(
            ClipData.newPlainText(
                context.getString(R.string.scan_settings),
                scanSettingsString
            )
        )
    }
}
