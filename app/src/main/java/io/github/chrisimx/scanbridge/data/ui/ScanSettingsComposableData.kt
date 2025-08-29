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

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.chrisimx.esclkt.ColorMode
import io.github.chrisimx.esclkt.DiscreteResolution
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.InputSourceCaps
import io.github.chrisimx.esclkt.ScanIntentData
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.scanbridge.data.model.ImmutableESCLScanSettingsState
import io.github.chrisimx.scanbridge.data.model.MutableESCLScanSettingsState
import io.github.chrisimx.scanbridge.data.model.PaperFormat
import io.github.chrisimx.scanbridge.data.model.StatelessImmutableESCLScanSettingsState
import io.github.chrisimx.scanbridge.data.model.loadDefaultFormats
import io.github.chrisimx.scanbridge.util.getInputSourceOptions
import kotlinx.serialization.Serializable

@Serializable
data class ScanSettingsComposableData(val scanSettingsState: MutableESCLScanSettingsState, val capabilities: ScannerCapabilities) {
    val inputSourceOptions: List<InputSource> =
        capabilities.getInputSourceOptions()
    val paperFormats: List<PaperFormat> = loadDefaultFormats()
    val duplexAdfSupported = capabilities.adf?.duplexCaps != null

    val customMenuEnabledState = mutableStateOf(false)
    var customMenuEnabled by customMenuEnabledState

    val widthTextFieldState = mutableStateOf("")
    var widthTextFieldString by widthTextFieldState
    val heightTextFieldState = mutableStateOf("")
    var heightTextFieldString by heightTextFieldState
    val xOffsetTextFieldState = mutableStateOf("0")
    var xOffsetTextFieldString by xOffsetTextFieldState
    val yOffsetTextFieldState = mutableStateOf("0")
    var yOffsetTextFieldString by yOffsetTextFieldState

    private val selectedInputSourceCapabilitiesState = derivedStateOf {
        when (scanSettingsState.inputSource) {
            InputSource.Platen -> capabilities.platen!!.inputSourceCaps
            InputSource.Feeder -> if (scanSettingsState.duplex == true) capabilities.adf!!.duplexCaps!! else capabilities.adf!!.simplexCaps
            InputSource.Camera -> throw UnsupportedOperationException("Camera is not supported yet!")
            null -> capabilities.platen!!.inputSourceCaps // assumes default is Platen
        }
    }
    val selectedInputSourceCapabilities by selectedInputSourceCapabilitiesState

    private val intentOptionsState = derivedStateOf {
        selectedInputSourceCapabilities.supportedIntents
    }
    val intentOptions by intentOptionsState

    val supportedScanResolutionsState = derivedStateOf {
        selectedInputSourceCapabilities.settingProfiles[0].supportedResolutions
    }
    val supportedScanResolutions by supportedScanResolutionsState

    private val availableColorModesState = derivedStateOf {
        selectedInputSourceCapabilities.settingProfiles.elementAtOrNull(0)?.colorModes ?: emptyList()
    }
    val availableColorModes by availableColorModesState

    // Determine supported effects based on scanner capabilities
    private val supportedEffectsState = derivedStateOf {
        val basicEffects = setOf("brightness", "contrast", "sharpen")
        val advancedEffects = mutableSetOf<String>()
        
        // Check if scanner supports advanced features that suggest more effect support
        val settingProfile = selectedInputSourceCapabilities.settingProfiles.elementAtOrNull(0)
        val hasMultipleColorModes = (settingProfile?.colorModes?.size ?: 0) > 1
        val hasAdvancedColorSpaces = settingProfile?.colorSpaces?.any { it != "sRGB" } ?: false
        val hasMultipleCcdChannels = (settingProfile?.ccdChannels?.size ?: 0) > 3
        
        // If scanner has advanced color capabilities, likely supports more effects
        if (hasMultipleColorModes || hasAdvancedColorSpaces || hasMultipleCcdChannels) {
            advancedEffects.addAll(setOf("gamma", "highlight", "shadow"))
        }
        
        // If scanner has very advanced capabilities, it might support even more effects
        if (hasMultipleColorModes && hasAdvancedColorSpaces && hasMultipleCcdChannels) {
            advancedEffects.addAll(setOf("noiseRemoval", "compressionFactor"))
        }
        
        basicEffects + advancedEffects
    }
    val supportedEffects by supportedEffectsState

    fun toImmutable(): ImmutableScanSettingsComposableData = ImmutableScanSettingsComposableData(
        scanSettingsState.toImmutable(),
        capabilities,
        inputSourceOptions,
        paperFormats,
        duplexAdfSupported,
        widthTextFieldState,
        heightTextFieldState,
        xOffsetTextFieldState,
        yOffsetTextFieldState,
        customMenuEnabledState,
        selectedInputSourceCapabilitiesState,
        intentOptionsState,
        supportedScanResolutionsState,
        availableColorModesState,
        supportedEffectsState
    )

    fun toStateless(): StatelessImmutableScanSettingsComposableData = StatelessImmutableScanSettingsComposableData(
        scanSettingsState.toStateless(),
        capabilities,
        inputSourceOptions,
        paperFormats,
        duplexAdfSupported,
        widthTextFieldState.value,
        heightTextFieldState.value,
        xOffsetTextFieldState.value,
        yOffsetTextFieldState.value,
        customMenuEnabledState.value,
        selectedInputSourceCapabilitiesState.value,
        intentOptionsState.value,
        supportedScanResolutionsState.value,
        availableColorModesState.value,
        supportedEffectsState.value
    )
}

@Serializable
data class ImmutableScanSettingsComposableData(
    val scanSettingsState: ImmutableESCLScanSettingsState,
    val capabilities: ScannerCapabilities,
    val inputSourceOptions: List<InputSource>,
    val paperFormats: List<PaperFormat>,
    val duplexAdfSupported: Boolean,
    private val widthTextFieldState: State<String>,
    private val heightTextFieldState: State<String>,
    private val xOffsetTextFieldState: State<String>,
    private val yOffsetTextFieldState: State<String>,
    private val customMenuEnabledState: State<Boolean>,
    private val selectedInputSourceCapabilitiesState: State<InputSourceCaps>,
    private val intentOptionsState: State<List<ScanIntentData>>,
    private val supportedScanResolutionsState: State<List<DiscreteResolution>>,
    private val availableColorModesState: State<List<ColorMode>>,
    private val supportedEffectsState: State<Set<String>>
) {
    val customMenuEnabled by customMenuEnabledState
    val widthTextFieldString by widthTextFieldState
    val heightTextFieldString by heightTextFieldState
    val xOffsetTextFieldString by xOffsetTextFieldState
    val yOffsetTextFieldString by yOffsetTextFieldState
    val selectedInputSourceCapabilities by selectedInputSourceCapabilitiesState
    val intentOptions by intentOptionsState
    val supportedScanResolutions by supportedScanResolutionsState
    val availableColorModes by availableColorModesState
    val supportedEffects by supportedEffectsState
}

@Serializable
data class StatelessImmutableScanSettingsComposableData(
    val scanSettings: StatelessImmutableESCLScanSettingsState,
    val capabilities: ScannerCapabilities,
    val inputSourceOptions: List<InputSource>,
    val paperFormats: List<PaperFormat>,
    val duplexAdfSupported: Boolean,
    val widthTextField: String,
    val heightTextField: String,
    val xOffsetTextField: String,
    val yOffsetTextField: String,
    val customMenuEnabled: Boolean,
    val selectedInputSourceCapabilities: InputSourceCaps,
    val intentOptions: List<ScanIntentData>,
    val supportedScanResolutions: List<DiscreteResolution>,
    val availableColorModes: List<ColorMode>,
    val supportedEffects: Set<String>
)
