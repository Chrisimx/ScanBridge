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

package io.github.chrisimx.scanbridge

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.chrisimx.anyscan.DiscreteResolution
import io.github.chrisimx.anyscan.ScanSettingParam
import io.github.chrisimx.anyscan.ScannerConcept
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsComposableStateHolder
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsLengthUnit
import io.github.chrisimx.scanbridge.util.toReadableString

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
private val TAG = "ScanSettings"

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanSettingsUI(modifier: Modifier, scanSettingsStateHolder: ScanSettingsComposableStateHolder) {
    val context = LocalContext.current
    val vmData by scanSettingsStateHolder.uiState.collectAsState()

    val duplexCurrentlyAvailable by scanSettingsStateHolder.duplexSettingAvailable.collectAsState()

    val inputSourceOptions by scanSettingsStateHolder.inputSourceOptions.collectAsState()

    val userUnitEnum by scanSettingsStateHolder.lengthUnit.collectAsState(ScanSettingsLengthUnit.MILLIMETER)

    val userUnitString = when (userUnitEnum) {
        ScanSettingsLengthUnit.INCH -> stringResource(R.string.inches)
        ScanSettingsLengthUnit.MILLIMETER -> stringResource(R.string.millimeter_unit_abbreviation)
    }

    val availableParameters by scanSettingsStateHolder.availableParameters.collectAsState()

    val scanSettings by scanSettingsStateHolder.scanSettings.collectAsState()

    val selectedInputSource by scanSettingsStateHolder.selectedInputSource.collectAsState()
    val duplexUsed by scanSettingsStateHolder.duplexUsed.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier
            .fillMaxWidth()
            .padding(10.dp)
            .testTag("scsetcolumn")
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.input_source))
        FlowRow(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)
        ) {
            SingleChoiceSegmentedButtonRow {
                inputSourceOptions.forEachIndexed { index, inputSource ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = inputSourceOptions.size
                        ),
                        onClick = { scanSettingsStateHolder.setInputSource(inputSource) },
                        selected = selectedInputSource == inputSource
                    ) {
                        Text(inputSource.toReadableString(context))
                    }
                }
            }
            ToggleButton(
                enabled = duplexCurrentlyAvailable,
                checked = duplexUsed,
                onCheckedChange = { scanSettingsStateHolder.setDuplex(it) }
            ) { Text(stringResource(R.string.setting_duplex)) }
        }

        var fitsRowVersion by remember { mutableStateOf(false) }

        for (parameterPair in availableParameters) {
            val (scannerConcept, parameter) = parameterPair

            val scannerConceptLocalizedName = when (scannerConcept) {
                ScannerConcept.ColorMode -> R.string.color_mode
                ScannerConcept.ScanIntent -> R.string.intent
                ScannerConcept.ScanRegion -> R.string.scan_region
                ScannerConcept.ScanResolution -> R.string.resolution_dpi
            }

            when (parameter) {
                is ScanSettingParam.ScanSettingBoolParam -> TODO()
                is ScanSettingParam.ScanSettingChoiceParam<*> -> {
                    SelectionCardWithDefault(
                        stringResource(scannerConceptLocalizedName),
                        parameter.availableChoices.map { it.value },
                        { selectedChoice ->
                            scanSettingsStateHolder.setSetting(parameter.concept, selectedChoice)
                        },
                        { this.toString() },
                        scanSettings.setting[parameter.concept]
                    )
                }
                is ScanSettingParam.ScanSettingDoubleParam -> TODO()
                is ScanSettingParam.ScanSettingFloatParam -> TODO()
                is ScanSettingParam.ScanSettingIntParam -> TODO()
                is ScanSettingParam.ScanSettingRegionParam -> {}
            }
        }
    }
}

@Composable
private fun <T> SelectionCardWithDefault(
    title: String,
    options: List<T>,
    onSet: (T?) -> Unit,
    stringify: T.() -> String,
    value: T?,
    isSmallRowAbove: Boolean = false
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = if (isSmallRowAbove) 30.dp else 15.dp, bottom = 15.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            FlowRow(
                Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                options.forEach { option ->
                    val name = option.stringify()
                    InputChip(
                        onClick = {
                            onSet(option)
                        },
                        label = { Text(name) },
                        selected = value == option
                    )
                }
                InputChip(
                    onClick = {
                        onSet(null)
                    },
                    label = { Text(stringResource(R.string.default_string)) },
                    selected = value == null
                )
            }
        }
    }
}

@Composable
private fun ResolutionSettingButtonRowVersion(
    supportedResolutions: List<DiscreteResolution>,
    currentResolution: DiscreteResolution?,
    setSelectedResolution: (UInt, UInt) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.resolution_dpi))
        SingleChoiceSegmentedButtonRow {
            supportedResolutions.forEachIndexed { index, discreteResolution ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = supportedResolutions.size
                    ),
                    onClick = {
                        setSelectedResolution(discreteResolution.widthDPI, discreteResolution.heightDPI)
                    },
                    selected = currentResolution == discreteResolution
                ) {
                    if (discreteResolution.widthDPI == discreteResolution.heightDPI) {
                        Text("${discreteResolution.widthDPI}")
                    } else {
                        Text("${discreteResolution.widthDPI}x${discreteResolution.heightDPI}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResolutionSettingCardVersion(
    supportedResolutions: List<DiscreteResolution>,
    currentResolution: DiscreteResolution?,
    setSelectedResolution: (UInt, UInt) -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 15.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                stringResource(R.string.resolution_dpi),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            FlowRow(
                Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                supportedResolutions.forEach { discreteResolution ->
                    val text = if (discreteResolution.widthDPI == discreteResolution.heightDPI) {
                        "${discreteResolution.widthDPI}"
                    } else {
                        "${discreteResolution.widthDPI}x${discreteResolution.heightDPI}"
                    }
                    InputChip(
                        onClick = {
                            setSelectedResolution(
                                discreteResolution.widthDPI,
                                discreteResolution.heightDPI
                            )
                        },
                        label = { Text(text) },
                        selected = currentResolution == discreteResolution
                    )
                }
            }
        }
    }
}
