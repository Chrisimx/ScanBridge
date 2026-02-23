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
import io.github.chrisimx.esclkt.DiscreteResolution
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.InputSourceCaps
import io.github.chrisimx.esclkt.LengthUnit
import io.github.chrisimx.esclkt.ScanIntentEnumOrRaw
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ThreeHundredthsOfInch
import io.github.chrisimx.esclkt.getInputSourceCaps
import io.github.chrisimx.esclkt.getInputSourceOptions
import io.github.chrisimx.esclkt.inches
import io.github.chrisimx.esclkt.millimeters
import io.github.chrisimx.esclkt.scanRegion
import io.github.chrisimx.esclkt.threeHundredthsOfInch
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.services.LocaleProvider
import io.github.chrisimx.scanbridge.util.derived
import io.github.chrisimx.scanbridge.util.getMaxResolution
import io.github.chrisimx.scanbridge.util.toDoubleLocalized
import java.util.*
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
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import timber.log.Timber

enum class ScanSettingsLengthUnit {
    INCH,
    MILLIMETER
}

class ScanSettingsComposableStateHolder(
    @InjectedParam
    val scanSettings: StateFlow<ScanSettings>,
    @InjectedParam
    private val initialScanSettingsData: ScanSettingsStateData,
    @InjectedParam
    private val updateSettings: suspend (ScanSettings.() -> ScanSettings) -> Unit,
    @InjectedParam
    private val coroutineScope: CoroutineScope,
    private val localeProvider: LocaleProvider,
    private val context: Application
) {

    private val _uiState = MutableStateFlow(initialScanSettingsData)
    val uiState: StateFlow<ScanSettingsStateData> = _uiState.asStateFlow()

    val inputSourceOptions: StateFlow<List<InputSource>> = _uiState.derived(coroutineScope) {
        it.capabilities.getInputSourceOptions()
    }

    val duplexAdfSupported: StateFlow<Boolean> = _uiState.derived(coroutineScope) {
        it.capabilities.adf?.duplexCaps != null
    }

    val duplexCurrentlyAvailable: StateFlow<Boolean> = combine(duplexAdfSupported, scanSettings) { duplexSupport, scanSettings ->
        duplexSupport && scanSettings.inputSource == InputSource.Feeder
    }.stateIn(coroutineScope, SharingStarted.Lazily, false)

    private val selectedInputSourceCaps: StateFlow<InputSourceCaps> = combine(scanSettings, _uiState) { settings, uiState ->
        uiState.capabilities.getInputSourceCaps(settings.inputSource, settings.duplex ?: false)
    }.stateIn(coroutineScope, SharingStarted.Lazily, uiState.value.capabilities.getInputSourceOptions().first().let {
        uiState.value.capabilities.getInputSourceCaps(it, scanSettings.value.duplex == true)
    })

    val intentOptions = selectedInputSourceCaps.derived(coroutineScope) {
        it.supportedIntents
    }

    val supportedScanResolutions = selectedInputSourceCaps.derived(coroutineScope) {
        it.settingProfiles[0].supportedResolutions
    }

    val currentResolution: StateFlow<DiscreteResolution?> = scanSettings.derived(coroutineScope) {
        val x = it.xResolution
        val y = it.yResolution

        if (x != null && y != null) DiscreteResolution(x, y) else null
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
        it.scanRegions?.regions?.firstOrNull()
    }

    val heightValidationResult = combine(currentHeightText, lengthUnit, selectedInputSourceCaps)
        { heightText, unit, inputSourceCaps ->
            return@combine validateCustomLengthInput(heightText, unit, inputSourceCaps.maxHeight, inputSourceCaps.minHeight)
        }.stateIn(coroutineScope, SharingStarted.Lazily, NumberValidationResult.NotANumber)

    val widthValidationResult = combine(currentWidthText, lengthUnit, selectedInputSourceCaps)
        { widthText, unit, inputSourceCaps ->
            return@combine validateCustomLengthInput(widthText, unit, inputSourceCaps.maxWidth, inputSourceCaps.minWidth)
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
        //observeHeightValidation()
        //observeWidthValidation()

        /*_uiState
            .map { it.maximumSize }
            .distinctUntilChanged()
            .combine(selectedInputSourceCaps) { maxSize, inputSourceCaps -> Pair(maxSize, inputSourceCaps) }
            .filter { it.first }
            .onEach { (maxSize, inputSourceCaps) ->
                updateSettings {
                    copy(
                        scanRegions = scanRegion {
                            width = inputSourceCaps.maxWidth
                            height = inputSourceCaps.maxHeight
                            xOffset = 0.millimeters()
                            yOffset = 0.millimeters()
                        }
                    )
                }
            }.launchIn(coroutineScope)*/
    }

    private fun observeWidthValidation() {
        widthValidationResult
            .filterIsInstance<NumberValidationResult.Success>()
            .distinctUntilChanged()
            .onEach { widthValidationResult ->
                updateSettings {
                    val currentScanRegion = scanRegions?.regions?.firstOrNull()

                    if (currentScanRegion == null) {
                        return@updateSettings copy(
                                scanRegions = scanRegion {
                                    maxHeight()
                                    width = widthValidationResult.value.threeHundredthsOfInch()
                                }
                            )
                    } else {
                        val currentHeight = currentScanRegion.height
                        return@updateSettings copy(
                                scanRegions = scanRegion {
                                    width = widthValidationResult.value.threeHundredthsOfInch()
                                    height = currentHeight
                                }
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
            .onEach { heightValidationResult ->
                Timber.d("Height validation success result received: $heightValidationResult")
                updateSettings {
                    val currentScanRegion = scanRegions?.regions?.firstOrNull()

                    if (currentScanRegion == null) {
                        return@updateSettings copy(
                                scanRegions = scanRegion {
                                    maxWidth()
                                    height = heightValidationResult.value.threeHundredthsOfInch()
                                }
                            )
                    } else {
                        val currentWidth = currentScanRegion.width
                        return@updateSettings copy(
                                scanRegions = scanRegion {
                                    width = currentWidth
                                    height = heightValidationResult.value.threeHundredthsOfInch()
                                }
                            )
                    }
                }
            }
            .launchIn(coroutineScope)
    }

    fun setDuplex(duplex: Boolean) {
        coroutineScope.launch {
            updateSettings {
                copy(duplex = duplex)
            }
        }
    }

    fun setInputSource(inputSource: InputSource) {
        coroutineScope.launch {
            updateSettings {
                val currentScanSettings = scanSettings.value
                val uiState = uiState.value
                val inputSourceCaps = uiState.capabilities.getInputSourceCaps(inputSource, currentScanSettings.duplex == true)

                val supportedResolutions = inputSourceCaps.settingProfiles[0].supportedResolutions.discreteResolutions

                val xRes = currentScanSettings.xResolution
                val yRes = currentScanSettings.yResolution

                val invalidResolutionSetting = xRes != null && yRes != null &&
                    !supportedResolutions.contains(DiscreteResolution(xRes, yRes))

                val replacementResolution = if (invalidResolutionSetting) {
                    val highestScanResolution = uiState.capabilities.getMaxResolution(inputSource)

                    Pair(highestScanResolution.xResolution, highestScanResolution.yResolution)
                } else {
                    Pair(xRes, yRes)
                }

                val intentSupported = currentScanSettings.intent?.let { inputSourceCaps.supportedIntents.contains(it) } ?: true

                val replacementIntent = if (intentSupported) {
                    currentScanSettings.intent
                } else {
                    null
                }

                copy(
                    inputSource = inputSource,
                    xResolution = replacementResolution.first,
                    yResolution = replacementResolution.second,
                    intent = replacementIntent
                )
            }
        }
    }

    fun setResolution(xResolution: UInt, yResolution: UInt) {
        coroutineScope.launch {
            updateSettings {
                copy(
                    xResolution = xResolution,
                    yResolution = yResolution
                )
            }
        }
    }

    fun setIntent(intent: ScanIntentEnumOrRaw?) {
        coroutineScope.launch {
            updateSettings {
                copy(intent = intent)
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

    private fun unitByLocale(locale: Locale = Locale.getDefault()): ScanSettingsLengthUnit =
        if (locale.country in setOf("US", "LR", "MM")) {
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
                    scanRegions = scanRegion {
                        width = newWidth
                        height = newHeight
                        xOffset = 0.millimeters()
                        yOffset = 0.millimeters()
                    }
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
