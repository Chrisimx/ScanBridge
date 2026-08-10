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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.chrisimx.anyscan.CommonScanSettings
import io.github.chrisimx.anyscan.LengthUnit
import io.github.chrisimx.anyscan.ScanSettingParam
import io.github.chrisimx.anyscan.ScannerConcept
import io.github.chrisimx.anyscan.equalsLength
import io.github.chrisimx.scanbridge.model.UIInputSourceType
import io.github.chrisimx.scanbridge.uicomponents.SelectionButtonRow
import io.github.chrisimx.scanbridge.uicomponents.SelectionCard
import io.github.chrisimx.scanbridge.uicomponents.SizeBasedConditionalView
import io.github.chrisimx.scanbridge.uicomponents.ValidatedDimensionsTextEdit
import io.github.chrisimx.scanbridge.localizationhelper.toLocalizedName
import io.github.chrisimx.scanbridge.localizationhelper.toReadableString
import org.jetbrains.compose.resources.stringResource
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.custom
import scanbridge.composeui.generated.resources.height_in_unit
import scanbridge.composeui.generated.resources.inches
import scanbridge.composeui.generated.resources.input_source
import scanbridge.composeui.generated.resources.maximum_size
import scanbridge.composeui.generated.resources.millimeter_unit_abbreviation
import scanbridge.composeui.generated.resources.setting_duplex
import scanbridge.composeui.generated.resources.width_in_unit

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanSettingsUI(modifier: Modifier, scanSettingsStateHolder: ScanSettingsComposableStateHolder) {
    val duplexCurrentlyAvailable by scanSettingsStateHolder.duplexSettingAvailable.collectAsState()

    val inputSourceOptions by scanSettingsStateHolder.inputSourceOptions.collectAsState()

    val userUnitEnum by scanSettingsStateHolder.userLengthUnit.collectAsState(ScanSettingsLengthUnit.MILLIMETER)

    val userUnitString = when (userUnitEnum) {
        ScanSettingsLengthUnit.INCH -> stringResource(Res.string.inches)
        ScanSettingsLengthUnit.MILLIMETER -> stringResource(Res.string.millimeter_unit_abbreviation)
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
        InputSourceSelection(
            inputSourceOptions,
            scanSettingsStateHolder,
            selectedInputSource,
            duplexCurrentlyAvailable,
            duplexUsed
        )

        for (parameterPair in availableParameters) {
            val (scannerConcept, parameter) = parameterPair

            val scannerConceptLocalizedName = scannerConcept.toLocalizedName()

            when (parameter) {
                is ScanSettingParam.ScanSettingBoolParam -> TODO()

                is ScanSettingParam.ScanSettingChoiceParam<*> -> {
                    ChoiceParameterDisplay(scannerConceptLocalizedName, parameter, scanSettingsStateHolder, scannerConcept, scanSettings)
                }

                is ScanSettingParam.ScanSettingDoubleParam -> TODO()

                is ScanSettingParam.ScanSettingFloatParam -> TODO()

                is ScanSettingParam.ScanSettingIntParam -> TODO()

                is ScanSettingParam.ScanSettingRegionParam -> {
                    RegionParameterDisplay(
                        scannerConceptLocalizedName,
                        scanSettingsStateHolder,
                        scanSettings,
                        userUnitString
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionParameterDisplay(
    scannerConceptLocalizedName: String,
    scanSettingsStateHolder: ScanSettingsComposableStateHolder,
    scanSettings: CommonScanSettings,
    userUnitString: String
) {
    val vmData by scanSettingsStateHolder.uiState.collectAsState()
    val availablePaperFormats by scanSettingsStateHolder.availablePaperFormats.collectAsState()
    val currentScanRegion = scanSettings.setting[ScannerConcept.ScanRegion]

    val widthValidationResult by scanSettingsStateHolder.validationResultWidth.collectAsState()
    val heightValidationResult by scanSettingsStateHolder.validationResultHeight.collectAsState()

    val currentVmData = vmData ?: return

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 15.dp, bottom = 15.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                scannerConceptLocalizedName,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                availablePaperFormats.forEach { paperFormat ->
                    InputChip(
                        onClick = {
                            scanSettingsStateHolder.setFormat(paperFormat)
                        },
                        label = { Text(paperFormat.name) },
                        selected = !currentVmData.customMenuEnabled && !currentVmData.maximumSize &&
                            currentScanRegion?.value?.width?.equalsLength(paperFormat.width) == true &&
                            currentScanRegion?.value?.height?.equalsLength(paperFormat.height) == true
                    )
                }
                InputChip(
                    onClick = {
                        scanSettingsStateHolder.selectMaxRegion()
                    },
                    label = { Text(stringResource(Res.string.maximum_size)) },
                    selected =
                        currentVmData.maximumSize && !currentVmData.customMenuEnabled
                )
                InputChip(
                    selected = currentVmData.customMenuEnabled,
                    onClick = { scanSettingsStateHolder.setCustomMenuEnabled(true) },
                    label = { Text(stringResource(Res.string.custom)) }
                )
            }
            AnimatedVisibility(currentVmData.customMenuEnabled) {
                Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                    ValidatedDimensionsTextEdit(
                        currentVmData.widthString,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        stringResource(Res.string.width_in_unit, userUnitString),
                        { newText: String ->
                            scanSettingsStateHolder.setCustomWidthTextFieldContent(
                                newText
                            )
                        },
                        widthValidationResult
                    )
                    ValidatedDimensionsTextEdit(
                        currentVmData.heightString,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp),
                        stringResource(Res.string.height_in_unit, userUnitString),
                        { scanSettingsStateHolder.setCustomHeightTextFieldContent(it) },
                        heightValidationResult
                    )
                }
            }
        }
    }
}

private fun LengthUnit.equalsLength(other: LengthUnit): Boolean = this.toMillimeters().value == other.toMillimeters().value

@Composable
private fun ChoiceParameterDisplay(
    scannerConceptLocalizedName: String,
    parameter: ScanSettingParam.ScanSettingChoiceParam<*>,
    scanSettingsStateHolder: ScanSettingsComposableStateHolder,
    scannerConcept: ScannerConcept<*>,
    scanSettings: CommonScanSettings
) {
    SizeBasedConditionalView(
        largeView = {
            SelectionButtonRow(
                scannerConceptLocalizedName,
                parameter.availableChoices.map { it.value },
                { selectedChoice ->
                    scanSettingsStateHolder.setSetting(parameter.concept, selectedChoice)
                },
                { this.toLocalizedName(scannerConcept) },
                scanSettings.setting[parameter.concept]
            )
        },
        smallView = {
            SelectionCard(
                scannerConceptLocalizedName,
                parameter.availableChoices.map { it.value },
                { selectedChoice ->
                    scanSettingsStateHolder.setSetting(parameter.concept, selectedChoice)
                },
                { this.toLocalizedName(scannerConcept) },
                scanSettings.setting[parameter.concept]
            )
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun InputSourceSelection(
    inputSourceOptions: List<UIInputSourceType>,
    scanSettingsStateHolder: ScanSettingsComposableStateHolder,
    selectedInputSource: UIInputSourceType,
    duplexCurrentlyAvailable: Boolean,
    duplexUsed: Boolean
) {
    Text(stringResource(Res.string.input_source))
    FlowRow(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
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
                    Text(inputSource.toReadableString())
                }
            }
        }
        ToggleButton(
            enabled = duplexCurrentlyAvailable,
            checked = duplexUsed,
            onCheckedChange = { scanSettingsStateHolder.setDuplex(it) }
        ) { Text(stringResource(Res.string.setting_duplex)) }
    }
}
