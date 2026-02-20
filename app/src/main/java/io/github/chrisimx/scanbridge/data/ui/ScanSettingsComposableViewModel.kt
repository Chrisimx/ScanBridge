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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsLengthUnit.*
import io.github.chrisimx.scanbridge.services.LocaleProvider
import io.github.chrisimx.scanbridge.util.derived
import io.github.chrisimx.scanbridge.util.getMaxResolution
import io.github.chrisimx.scanbridge.util.toDoubleLocalized
import java.util.*
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
import org.koin.core.annotation.InjectedParam

enum class ScanSettingsLengthUnit {
    INCH,
    MILLIMETER
}

class ScanSettingsComposableViewModel(
    @InjectedParam
    private val initialScanSettingsData: ScanSettingsComposableData,
    @InjectedParam
    private val onSettingsChanged: (() -> Unit)? = null,
    private val localeProvider: LocaleProvider,
    private val context: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialScanSettingsData)
    val uiState: StateFlow<ScanSettingsComposableData> = _uiState.asStateFlow()

    val inputSourceOptions: StateFlow<List<InputSource>> = _uiState.derived(viewModelScope) {
        it.capabilities.getInputSourceOptions()
    }

    val duplexAdfSupported: StateFlow<Boolean> = _uiState.derived(viewModelScope) {
        it.capabilities.adf?.duplexCaps != null
    }

    val duplexCurrentlyAvailable: StateFlow<Boolean> = combine(duplexAdfSupported, _uiState) { duplexSupport, uiState ->
        duplexSupport && uiState.scanSettings.inputSource == InputSource.Feeder
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val selectedInputSourceCaps: StateFlow<InputSourceCaps> = _uiState.derived(viewModelScope) {
        it.capabilities.getInputSourceCaps(it.scanSettings.inputSource, it.scanSettings.duplex ?: false)
    }

    val intentOptions = selectedInputSourceCaps.derived(viewModelScope) {
        it.supportedIntents
    }

    val supportedScanResolutions = selectedInputSourceCaps.derived(viewModelScope) {
        it.settingProfiles[0].supportedResolutions
    }

    val currentResolution: StateFlow<DiscreteResolution?> = uiState.derived(viewModelScope) {
        val settings = it.scanSettings
        val x = settings.xResolution
        val y = settings.yResolution

        if (x != null && y != null) DiscreteResolution(x, y) else null
    }

    val lengthUnit = localeProvider.locale.derived(viewModelScope) {
        unitByLocale(it)
    }

    val currentWidthText = _uiState.derived(viewModelScope) {
        it.widthString
    }

    val currentHeightText = _uiState.derived(viewModelScope) {
        it.heightString
    }

    val currentScanRegion = _uiState.derived(viewModelScope) {
        it.scanSettings.scanRegions?.regions?.firstOrNull()
    }

    val heightValidationResult = combine(currentHeightText, lengthUnit, selectedInputSourceCaps)
    { heightText, unit, inputSourceCaps ->
        return@combine validateCustomLengthInput(heightText, unit, inputSourceCaps.maxHeight, inputSourceCaps.minHeight)
    }

    val widthValidationResult = combine(currentWidthText, lengthUnit, selectedInputSourceCaps)
    { widthText, unit, inputSourceCaps ->
        return@combine validateCustomLengthInput(widthText, unit, inputSourceCaps.maxWidth, inputSourceCaps.minWidth)
    }

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
            INCH -> parsedLength.inches()
            MILLIMETER -> parsedLength.millimeters()
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

    private fun toUserUnit(
        unit: ScanSettingsLengthUnit,
        length: LengthUnit
    ): Double = when (unit) {
        INCH -> length.toInches().value
        MILLIMETER -> length.toMillimeters().value
    }

    private inline fun ScanSettingsComposableData.updateScanSettings(update: ScanSettings.() -> ScanSettings) =
        copy(scanSettings = scanSettings.update())

    private inline fun MutableStateFlow<ScanSettingsComposableData>.updateScanSettings(updateLambda: ScanSettings.() -> ScanSettings) =
        this.update {
            it.updateScanSettings(updateLambda)
        }

    init {
        _uiState
            .map { it.scanSettings }
            .distinctUntilChanged()
            .onEach { onSettingsChanged?.invoke() }
            .launchIn(viewModelScope)

        observeHeightValidation()
        observeWidthValidation()

        _uiState
            .map { it.maximumSize }
            .distinctUntilChanged()
            .combine(selectedInputSourceCaps) { maxSize, inputSourceCaps -> Pair(maxSize, inputSourceCaps)}
            .filter { it.first }
            .onEach { (maxSize, inputSourceCaps) ->
                _uiState.updateScanSettings {
                    copy(
                        scanRegions = scanRegion {
                            width = inputSourceCaps.maxWidth
                            height = inputSourceCaps.maxHeight
                            xOffset = 0.millimeters()
                            yOffset = 0.millimeters()
                        }
                    )
                }
            }.launchIn(viewModelScope)
    }

    private fun observeWidthValidation() {
        widthValidationResult
            .filterIsInstance<NumberValidationResult.Success>()
            .distinctUntilChanged()
            .onEach { widthValidationResult ->
                _uiState.update {
                    val currentScanRegion = it.scanSettings.scanRegions?.regions?.firstOrNull()

                    if (currentScanRegion == null) {
                        return@update it.copy(
                            scanSettings = it.scanSettings.copy(
                                scanRegions = scanRegion {
                                    maxHeight()
                                    width = widthValidationResult.value.threeHundredthsOfInch()
                                }
                            ))
                    } else {
                        val currentHeight = currentScanRegion.height
                        return@update it.copy(
                            scanSettings = it.scanSettings.copy(
                                scanRegions = scanRegion {
                                    width = widthValidationResult.value.threeHundredthsOfInch()
                                    height = currentHeight
                                }
                            ))
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeHeightValidation() {
        heightValidationResult
            .filterIsInstance<NumberValidationResult.Success>()
            .distinctUntilChanged()
            .onEach { heightValidationResult ->
                _uiState.update {
                    val currentScanRegion = it.scanSettings.scanRegions?.regions?.firstOrNull()

                    if (currentScanRegion == null) {
                        return@update it.copy(
                            scanSettings = it.scanSettings.copy(
                                scanRegions = scanRegion {
                                    maxWidth()
                                    height = heightValidationResult.value.threeHundredthsOfInch()
                                }
                            ))
                    } else {
                        val currentWidth = currentScanRegion.width
                        return@update it.copy(
                            scanSettings = it.scanSettings.copy(
                                scanRegions = scanRegion {
                                    width = currentWidth
                                    height = heightValidationResult.value.threeHundredthsOfInch()
                                }
                            ))
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun setDuplex(duplex: Boolean) {
        _uiState.updateScanSettings {
            copy(duplex = duplex)
        }
    }

    fun setInputSource(inputSource: InputSource) {
        _uiState.update {
            val currentScanSettings = it.scanSettings
            val inputSourceCaps = it.capabilities.getInputSourceCaps(inputSource)

            val supportedResolutions = inputSourceCaps.settingProfiles[0].supportedResolutions.discreteResolutions

            val xRes = currentScanSettings.xResolution
            val yRes = currentScanSettings.yResolution

            val validResolutionSetting = xRes != null && yRes != null
                && !supportedResolutions.contains(DiscreteResolution(xRes, yRes))

            val replacementResolution = if (validResolutionSetting) {
                val highestScanResolution = it.capabilities.getMaxResolution(inputSource)

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

            it.copy(
                scanSettings = it.scanSettings.copy(
                    inputSource = inputSource,
                    xResolution = replacementResolution.first,
                    yResolution = replacementResolution.second,
                    intent = replacementIntent
                )
            )
        }
    }

    fun setResolution(xResolution: UInt, yResolution: UInt) {
        _uiState.updateScanSettings {
            copy(
                xResolution = xResolution,
                yResolution = yResolution
            )
        }
    }

    fun setIntent(intent: ScanIntentEnumOrRaw?) {
        _uiState.updateScanSettings {
            copy(intent = intent)
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

    private fun unitByLocale(locale: Locale = Locale.getDefault()): ScanSettingsLengthUnit {
        return if (locale.country in setOf("US", "LR", "MM")) {
            INCH
        } else {
            MILLIMETER
        }
    }

    fun selectMaxRegion() {
        _uiState.update {
            it.copy(maximumSize = true)
        }
    }

    fun setRegionDimension(newWidth: LengthUnit, newHeight: LengthUnit) {
        _uiState.update {
            val scanSettings = it.scanSettings.copy(
                scanRegions = scanRegion {
                    width = newWidth
                    height = newHeight
                    xOffset = 0.millimeters()
                    yOffset = 0.millimeters()
                }
            )
            it.copy(
                scanSettings = scanSettings,
                maximumSize = false
            )
        }
    }

    fun copySettingsToClipboard() {
        val systemClipboard =
            context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val scanSettingsString = uiState.value.scanSettings.toString()
        systemClipboard.setPrimaryClip(
            ClipData.newPlainText(
                context.getString(R.string.scan_settings),
                scanSettingsString
            )
        )
    }
}
