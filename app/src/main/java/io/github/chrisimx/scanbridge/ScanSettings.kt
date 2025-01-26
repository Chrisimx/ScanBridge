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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.util.Log
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.ScanIntentData
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsComposableViewModel
import io.github.chrisimx.scanbridge.uicomponents.ValidatedDimensionsTextEdit
import io.github.chrisimx.scanbridge.util.toReadableString

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)
private val TAG = "ScanSettings"

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanSettingsUI(modifier: Modifier, context: Context, scanSettingsViewModel: ScanSettingsComposableViewModel = viewModel()) {
    val scanSettingsUIState = scanSettingsViewModel.scanSettingsComposableData

    assert(scanSettingsUIState.inputSourceOptions.isNotEmpty()) // The settings are useless if this is the case

    val scrollState = rememberScrollState()

    Column(
        modifier
            .fillMaxWidth()
            .padding(10.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.input_source))
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SingleChoiceSegmentedButtonRow {
                scanSettingsUIState.inputSourceOptions.forEachIndexed { index, inputSource ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = scanSettingsUIState.inputSourceOptions.size
                        ),
                        onClick = { scanSettingsViewModel.setInputSourceOptions(inputSource) },
                        selected = scanSettingsUIState.scanSettingsState.inputSource == inputSource
                    ) {
                        Text(inputSource.toReadableString(context))
                    }
                }
            }
            val duplexAvailable =
                scanSettingsUIState.duplexAdfSupported && scanSettingsUIState.scanSettingsState.inputSource == InputSource.Feeder
            ToggleButton(
                enabled = duplexAvailable,
                checked = scanSettingsUIState.scanSettingsState.duplex == true,
                onCheckedChange = { scanSettingsViewModel.setDuplex(it) }
            ) { Text(stringResource(R.string.setting_duplex)) }
        }
        Text(stringResource(R.string.resolution_dpi))
        SingleChoiceSegmentedButtonRow {
            scanSettingsUIState.supportedScanResolutions.forEachIndexed { index, discreteResolution ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = scanSettingsUIState.supportedScanResolutions.size
                    ),
                    onClick = {
                        scanSettingsViewModel.setResolution(
                            discreteResolution.xResolution,
                            discreteResolution.yResolution
                        )
                    },
                    selected =
                    scanSettingsUIState.scanSettingsState.xResolution == discreteResolution.xResolution &&
                        scanSettingsUIState.scanSettingsState.yResolution == discreteResolution.yResolution
                ) {
                    if (discreteResolution.xResolution == discreteResolution.yResolution) {
                        Text("${discreteResolution.xResolution}")
                    } else {
                        Text("${discreteResolution.xResolution}x${discreteResolution.yResolution}")
                    }
                }
            }
        }
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 15.dp)
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
                    scanSettingsUIState.intentOptions.forEach { intentData ->
                        val name = when (intentData) {
                            is ScanIntentData.ScanIntentEnum -> intentData.scanIntent.name
                            is ScanIntentData.StringData -> intentData.string
                        }
                        InputChip(
                            onClick = {
                                scanSettingsViewModel.setIntent(intentData)
                            },
                            label = { Text(name) },
                            selected = scanSettingsUIState.scanSettingsState.intent == intentData
                        )
                    }
                    InputChip(
                        onClick = {
                            scanSettingsViewModel.setIntent(null)
                        },
                        label = { Text(stringResource(R.string.intent_none)) },
                        selected = scanSettingsUIState.scanSettingsState.intent == null
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
                    scanSettingsUIState.paperFormats.forEach { paperFormat ->
                        InputChip(
                            onClick = {
                                scanSettingsViewModel.setCustomMenuEnabled(false)
                                scanSettingsViewModel.setRegionDimension(
                                    paperFormat.width.toMillimeters().value.toString(),
                                    paperFormat.height.toMillimeters().value.toString()
                                )
                                Log.d(
                                    TAG,
                                    "New region state: ${scanSettingsUIState.scanSettingsState.scanRegions}"
                                )
                            },
                            label = { Text(paperFormat.name) },
                            selected =
                            scanSettingsUIState.scanSettingsState.scanRegions?.width ==
                                paperFormat.width.toMillimeters().value.toString() &&
                                scanSettingsUIState.scanSettingsState.scanRegions?.height ==
                                paperFormat.height.toMillimeters().value.toString() &&
                                !scanSettingsUIState.customMenuEnabled
                        )
                    }
                    InputChip(
                        onClick = {
                            scanSettingsViewModel.setCustomMenuEnabled(false)
                            scanSettingsViewModel.setRegionDimension("max", "max")
                        },
                        label = { Text(stringResource(R.string.maximum_size)) },
                        selected =
                        scanSettingsUIState.scanSettingsState.scanRegions?.width == "max" && !scanSettingsUIState.customMenuEnabled
                    )
                    InputChip(
                        selected = scanSettingsUIState.customMenuEnabled,
                        onClick = { scanSettingsViewModel.setCustomMenuEnabled(true) },
                        label = { Text(stringResource(R.string.custom)) }
                    )
                }
                AnimatedVisibility(scanSettingsUIState.customMenuEnabled) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                        ValidatedDimensionsTextEdit(
                            scanSettingsUIState.widthTextFieldString,
                            context,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            stringResource(R.string.width_in_mm),
                            { newText: String ->
                                scanSettingsViewModel.setWidthTextFieldContent(
                                    newText
                                )
                            },
                            { newWidth: String ->
                                scanSettingsViewModel.setRegionDimension(
                                    newWidth,
                                    scanSettingsUIState.heightTextFieldString
                                )
                            },
                            min = scanSettingsUIState.selectedInputSourceCapabilities.minWidth.toMillimeters().value,
                            max = scanSettingsUIState.selectedInputSourceCapabilities.maxWidth.toMillimeters().value
                        )
                        ValidatedDimensionsTextEdit(
                            scanSettingsUIState.heightTextFieldString,
                            context,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp),
                            stringResource(R.string.height_in_mm),
                            { scanSettingsViewModel.setHeightTextFieldContent(it) },
                            {
                                scanSettingsViewModel.setRegionDimension(
                                    scanSettingsUIState.widthTextFieldString,
                                    it
                                )
                            },
                            min = scanSettingsUIState.selectedInputSourceCapabilities.minHeight.toMillimeters().value,
                            max = scanSettingsUIState.selectedInputSourceCapabilities.maxHeight.toMillimeters().value
                        )
                    }
                }
            }
        }
        val localContext = LocalContext.current
        Button(onClick = {
            val systemClipboard =
                localContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val scanSettingsString =
                scanSettingsUIState.scanSettingsState.toESCLKtScanSettings(scanSettingsUIState.selectedInputSourceCapabilities)
                    .toString()
            systemClipboard.setPrimaryClip(
                ClipData.newPlainText(
                    localContext.getString(R.string.scan_settings),
                    scanSettingsString
                )
            )
        }) {
            Text(
                stringResource(R.string.copy_current_scanner_options_in_esclkt_format),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
