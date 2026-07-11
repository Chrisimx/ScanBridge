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
import io.github.chrisimx.anyscan.CommonInputSourceCaps
import io.github.chrisimx.anyscan.CommonInputSourceType
import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.anyscan.CommonScanSettingsEditor
import io.github.chrisimx.anyscan.LengthUnit
import io.github.chrisimx.anyscan.ScanSettingParam
import io.github.chrisimx.anyscan.ScannerConcept
import io.github.chrisimx.scanbridge.model.Locale
import io.github.chrisimx.scanbridge.model.ScanSettingsEnterableDataV1
import io.github.chrisimx.scanbridge.ports.LocaleProvider
import io.github.chrisimx.scanbridge.util.UIInputSourceType
import io.github.chrisimx.scanbridge.util.derived
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
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
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
    private val updateSettings: suspend (CommonScanSettingsEditor.() -> Unit) -> Unit,
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

    val duplexCurrentlyActive: StateFlow<Boolean> = combine(duplexAdfSupported, scanSettings) { duplexSupport, scanSettings ->
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

    val availableParameters = selectedInputSourceCaps.derived(coroutineScope) {
        it.furtherOptions
    }

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
        val duplexCurrentlyActive = duplexCurrentlyActive.value

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
        Timber.d("Input Source being set to $inputSource. Validating existing settings.")

        val newInputSource = when (inputSource) {
            UIInputSourceType.PLATEN -> CommonInputSourceType.PLATEN
            UIInputSourceType.ADF -> CommonInputSourceType.ADF_SIMPLEX
        }

        coroutineScope.launch {
            updateSettings {
                setInputSource(inputSource = newInputSource)
            }
        }
    }

    fun <T : Any> setSetting(concept: ScannerConcept<T>, value: Any?) {
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
}
