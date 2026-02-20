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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.chrisimx.esclkt.DiscreteResolution
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.InputSourceCaps
import io.github.chrisimx.esclkt.ScanIntentEnumOrRaw
import io.github.chrisimx.esclkt.ScanRegionLength
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.getInputSourceCaps
import io.github.chrisimx.esclkt.getInputSourceOptions
import io.github.chrisimx.esclkt.millimeters
import io.github.chrisimx.esclkt.scanRegion
import io.github.chrisimx.scanbridge.util.derived
import io.github.chrisimx.scanbridge.util.getMaxResolution
import io.github.chrisimx.scanbridge.util.toDoubleLocalized
import java.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class ScanSettingsComposableViewModel(
    private val initialScanSettingsData: ScanSettingsComposableData,
    private val onSettingsChanged: (() -> Unit)? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialScanSettingsData)
    val uiState: StateFlow<ScanSettingsComposableData> = _uiState.asStateFlow()

    val inputSourceOptions: StateFlow<List<InputSource>> = _uiState.derived(viewModelScope) {
        it.capabilities.getInputSourceOptions()
    }

    val duplexAdfSupported: StateFlow<Boolean> = _uiState.derived(viewModelScope) {
        it.capabilities.adf?.duplexCaps != null
    }

    private val selectedInputSourceCaps: StateFlow<InputSourceCaps> = _uiState.derived(viewModelScope) {
        it.capabilities.getInputSourceCaps(it.scanSettings.inputSource, it.scanSettings.duplex ?: false)
    }

    private val intentOptions = selectedInputSourceCaps.derived(viewModelScope) {
        it.supportedIntents
    }

    val supportedScanResolutions = selectedInputSourceCaps.derived(viewModelScope) {
        it.settingProfiles[0].supportedResolutions
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

    }

    fun setDuplex(duplex: Boolean) {
        _uiState.updateScanSettings {
            copy(duplex = duplex)
        }
    }

    fun setInputSourceOptions(inputSource: InputSource) {
        _uiState.update {
            val currentScanSettings = it.scanSettings
            val inputSourceCaps = it.capabilities.getInputSourceCaps(inputSource)

            val supportedResolutions = inputSourceCaps.settingProfiles[0].supportedResolutions.discreteResolutions

            val xRes = currentScanSettings.xResolution
            val yRes = currentScanSettings.yResolution

            val validResolutionSetting =  xRes != null && yRes != null
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

            it.copy(scanSettings = it.scanSettings.copy(
                xResolution = replacementResolution.first,
                yResolution = replacementResolution.second,
                intent = replacementIntent
            ))
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
            it.copy(customMenuEnabled = enabled)
        }
    }

    fun setCustomWidthTextFieldContent(width: String) {
        check(_uiState.value.customMenuEnabled)
        _uiState.update {
            it.copy(widthString = width)
        }
    }

    fun setCustomHeightTextFieldContent(height: String) {
        check(_uiState.value.customMenuEnabled)

        val parsedHeight = runCatching {
            height.toDoubleLocalized()
        }.getOrNull()

        _uiState.update {
            if (parsedHeight == null) {
                return@update it.copy(
                    heightString = height,
                    heightError = NumberValidationError.NotANumber
                )
            }

            val maxHeight = selectedInputSourceCaps.value.maxHeight.toMillimeters().value
            val minHeight = selectedInputSourceCaps.value.minHeight.toMillimeters().value

            val scanSettingsWithNewHeight = it.scanSettings.copy(scanRegions = )

            it.copy(heightString = height, scanSettings = parsedHeight.coerceIn(minHeight, maxHeight))
        }
    }

    fun usesImperial(locale: Locale = Locale.getDefault()): Boolean {
        return locale.country in setOf("US", "LR", "MM")
    }

    fun setRegionDimension(newWidth: ScanRegionLength, newHeight: ScanRegionLength) {
        _uiState.updateScanSettings {
            copy(
                scanRegions = scanRegion {
                    width(newWidth)
                    height(newHeight)
                    xOffset = 0.millimeters()
                    yOffset = 0.millimeters()
                }
            )
        }
    }
}
