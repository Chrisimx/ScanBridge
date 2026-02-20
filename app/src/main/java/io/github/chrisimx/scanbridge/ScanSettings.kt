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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
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
import io.github.chrisimx.esclkt.DiscreteResolution
import io.github.chrisimx.esclkt.SupportedResolutions
import io.github.chrisimx.esclkt.equalsLength
import io.github.chrisimx.scanbridge.data.ui.NumberValidationResult
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsComposableViewModel
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsLengthUnit
import io.github.chrisimx.scanbridge.uicomponents.SizeBasedConditionalView
import io.github.chrisimx.scanbridge.uicomponents.ValidatedDimensionsTextEdit
import io.github.chrisimx.scanbridge.util.toReadableString
import org.koin.androidx.compose.koinViewModel
import timber.log.Timber

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
private val TAG = "ScanSettings"


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanSettingsUI(modifier: Modifier,
                   scanSettingsViewModel: ScanSettingsComposableViewModel = koinViewModel<ScanSettingsComposableViewModel>()
) {
    val context = LocalContext.current
    val vmData by scanSettingsViewModel.uiState.collectAsState()

    val currentResolution by scanSettingsViewModel.currentResolution.collectAsState()
    val currentScanRegion by scanSettingsViewModel.currentScanRegion.collectAsState()

    val duplexCurrentlyAvailable by scanSettingsViewModel.duplexCurrentlyAvailable.collectAsState()

    val inputSourceOptions by scanSettingsViewModel.inputSourceOptions.collectAsState()
    val supportedResolutions by scanSettingsViewModel.supportedScanResolutions.collectAsState()
    val intentOptions by scanSettingsViewModel.intentOptions.collectAsState()

    val widthValidationResult by scanSettingsViewModel.widthValidationResult.collectAsState(NumberValidationResult.NotANumber)
    val heightValidationResult by scanSettingsViewModel.heightValidationResult.collectAsState(NumberValidationResult.NotANumber)

    val userUnitEnum by scanSettingsViewModel.lengthUnit.collectAsState(ScanSettingsLengthUnit.MILLIMETER)

    val userUnitString = when (userUnitEnum) {
        ScanSettingsLengthUnit.INCH -> stringResource(R.string.inches)
        ScanSettingsLengthUnit.MILLIMETER -> stringResource(R.string.mm)
    }

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
                        onClick = { scanSettingsViewModel.setInputSource(inputSource) },
                        selected = vmData.scanSettings.inputSource == inputSource
                    ) {
                        Text(inputSource.toReadableString(context))
                    }
                }
            }
            ToggleButton(
                enabled = duplexCurrentlyAvailable,
                checked = vmData.scanSettings.duplex == true,
                onCheckedChange = { scanSettingsViewModel.setDuplex(it) }
            ) { Text(stringResource(R.string.setting_duplex)) }
        }

        var fitsRowVersion by remember { mutableStateOf(false) }

        SizeBasedConditionalView(
            modifier = Modifier,
            largeView = {
                ResolutionSettingButtonRowVersion(supportedResolutions, currentResolution) { x, y ->
                    scanSettingsViewModel.setResolution(x, y)
                }
            },
            smallView = {
                ResolutionSettingCardVersion(supportedResolutions, currentResolution) { x, y ->
                    scanSettingsViewModel.setResolution(x, y)
                }
            },
            onViewChosen = { fitsRowVersion = it }
        )

        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = if (fitsRowVersion) 30.dp else 15.dp, bottom = 15.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.intent),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                FlowRow(
                    Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    intentOptions.forEach { intentData ->
                        val name = intentData.asString()
                        InputChip(
                            onClick = {
                                scanSettingsViewModel.setIntent(intentData)
                            },
                            label = { Text(name) },
                            selected = vmData.scanSettings.intent == intentData
                        )
                    }
                    InputChip(
                        onClick = {
                            scanSettingsViewModel.setIntent(null)
                        },
                        label = { Text(stringResource(R.string.intent_none)) },
                        selected = vmData.scanSettings.intent == null
                    )
                }
            }
        }
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 15.dp, bottom = 15.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    stringResource(R.string.scan_region),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    vmData.paperFormats.forEach { paperFormat ->
                        InputChip(
                            onClick = {
                                scanSettingsViewModel.setCustomMenuEnabled(false)
                                scanSettingsViewModel.setRegionDimension(
                                    paperFormat.width, paperFormat.height
                                )
                                Timber.tag(TAG).d("New region state: ${vmData.scanSettings.scanRegions}")
                            },
                            label = { Text(paperFormat.name) },
                            selected = !vmData.customMenuEnabled && !vmData.maximumSize
                                && currentScanRegion?.width?.equalsLength(paperFormat.width) == true
                                && currentScanRegion?.height?.equalsLength(paperFormat.height) == true
                        )
                    }
                    InputChip(
                        onClick = {
                            scanSettingsViewModel.setCustomMenuEnabled(false)
                            scanSettingsViewModel.selectMaxRegion()
                        },
                        label = { Text(stringResource(R.string.maximum_size)) },
                        selected =
                            vmData.maximumSize && !vmData.customMenuEnabled
                    )
                    InputChip(
                        selected = vmData.customMenuEnabled,
                        onClick = { scanSettingsViewModel.setCustomMenuEnabled(true) },
                        label = { Text(stringResource(R.string.custom)) }
                    )
                }
                AnimatedVisibility(vmData.customMenuEnabled) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                        ValidatedDimensionsTextEdit(
                            vmData.widthString,
                            context,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            stringResource(R.string.width_in_unit, userUnitString),
                            { newText: String ->
                                scanSettingsViewModel.setCustomWidthTextFieldContent(
                                    newText
                                )
                            },
                            widthValidationResult
                        )
                        ValidatedDimensionsTextEdit(
                            vmData.heightString,
                            context,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp),
                            stringResource(R.string.height_in_unit, userUnitString),
                            { scanSettingsViewModel.setCustomHeightTextFieldContent(it) },
                            heightValidationResult
                        )
                    }
                }
            }
        }
        Button(
            modifier = Modifier.padding(horizontal = 15.dp).testTag("copyesclkt"),
            onClick = { scanSettingsViewModel.copySettingsToClipboard() }
        ) {
            Text(
                stringResource(R.string.copy_current_scanner_options_in_esclkt_format),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ResolutionSettingButtonRowVersion(
    supportedResolutions: SupportedResolutions,
    currentResolution: DiscreteResolution?,
    setSelectedResolution: (UInt, UInt) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.resolution_dpi))
        SingleChoiceSegmentedButtonRow {
            supportedResolutions.discreteResolutions.forEachIndexed { index, discreteResolution ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = supportedResolutions.discreteResolutions.size
                    ),
                    onClick = {
                        setSelectedResolution(discreteResolution.xResolution, discreteResolution.yResolution)
                    },
                    selected = currentResolution == discreteResolution
                ) {
                    if (discreteResolution.xResolution == discreteResolution.yResolution) {
                        Text("${discreteResolution.xResolution}")
                    } else {
                        Text("${discreteResolution.xResolution}x${discreteResolution.yResolution}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResolutionSettingCardVersion(
    supportedResolutions: SupportedResolutions,
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
                supportedResolutions.discreteResolutions.forEachIndexed { index, discreteResolution ->
                    val text = if (discreteResolution.xResolution == discreteResolution.yResolution) {
                        "${discreteResolution.xResolution}"
                    } else {
                        "${discreteResolution.xResolution}x${discreteResolution.yResolution}"
                    }
                    InputChip(
                        onClick = {
                            setSelectedResolution(
                                discreteResolution.xResolution,
                                discreteResolution.yResolution
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
