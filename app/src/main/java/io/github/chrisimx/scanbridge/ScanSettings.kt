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
import androidx.compose.material3.Slider
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
import io.github.chrisimx.esclkt.CcdChannel
import io.github.chrisimx.esclkt.ColorMode
import io.github.chrisimx.esclkt.FeedDirection
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.ScanIntentData
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsComposableViewModel
import io.github.chrisimx.scanbridge.uicomponents.ValidatedDimensionsTextEdit
import io.github.chrisimx.scanbridge.util.toReadableString
import timber.log.Timber

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
                    stringResource(R.string.content_type),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // For now, add a simple "Default" option - we can expand this later
                    InputChip(
                        onClick = {
                            scanSettingsViewModel.setContentType(null)
                        },
                        label = { Text("Default") },
                        selected = scanSettingsUIState.scanSettingsState.contentType == null
                    )
                }

                // Color Space
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.color_space))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToggleButton(
                            checked = scanSettingsUIState.scanSettingsState.colorSpace != null,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    scanSettingsViewModel.setColorSpace("sRGB")
                                } else {
                                    scanSettingsViewModel.setColorSpace(null)
                                }
                            }
                        ) {
                            Text(scanSettingsUIState.scanSettingsState.colorSpace ?: "Default")
                        }
                    }
                }

                // Media Type
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.media_type))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToggleButton(
                            checked = scanSettingsUIState.scanSettingsState.mediaType != null,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    scanSettingsViewModel.setMediaType("Paper")
                                } else {
                                    scanSettingsViewModel.setMediaType(null)
                                }
                            }
                        ) {
                            Text(scanSettingsUIState.scanSettingsState.mediaType ?: "Default")
                        }
                    }
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
                    stringResource(R.string.advanced_settings),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Number of Pages
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.number_of_pages))
                    ToggleButton(
                        checked = scanSettingsUIState.scanSettingsState.numberOfPages != null,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                scanSettingsViewModel.setNumberOfPages(1u)
                            } else {
                                scanSettingsViewModel.setNumberOfPages(null)
                            }
                        }
                    ) {
                        Text(if (scanSettingsUIState.scanSettingsState.numberOfPages != null) "Enabled" else "Disabled")
                    }
                }

                // Blank Page Detection
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.blank_page_detection))
                    ToggleButton(
                        checked = scanSettingsUIState.scanSettingsState.blankPageDetection == true,
                        onCheckedChange = { scanSettingsViewModel.setBlankPageDetection(it) }
                    ) {
                        Text(if (scanSettingsUIState.scanSettingsState.blankPageDetection == true) "On" else "Off")
                    }
                }

                // Blank Page Removal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.blank_page_removal))
                    ToggleButton(
                        checked = scanSettingsUIState.scanSettingsState.blankPageDetectionAndRemoval == true,
                        onCheckedChange = { scanSettingsViewModel.setBlankPageDetectionAndRemoval(it) }
                    ) {
                        Text(if (scanSettingsUIState.scanSettingsState.blankPageDetectionAndRemoval == true) "On" else "Off")
                    }
                }

                // Threshold
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.threshold))
                    Slider(
                        value = (scanSettingsUIState.scanSettingsState.threshold ?: 50u).toFloat(),
                        onValueChange = { scanSettingsViewModel.setThreshold(it.toUInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Color Mode
                if (scanSettingsUIState.availableColorModes.isNotEmpty()) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(stringResource(R.string.color_mode))
                        SingleChoiceSegmentedButtonRow {
                            scanSettingsUIState.availableColorModes.forEachIndexed { index, colorMode ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = scanSettingsUIState.availableColorModes.size
                                    ),
                                    onClick = { scanSettingsViewModel.setColorMode(colorMode) },
                                    selected = scanSettingsUIState.scanSettingsState.colorMode == colorMode
                                ) {
                                    Text(colorMode.name)
                                }
                            }
                        }
                    }
                }

                // Feed Direction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.feed_direction))
                    ToggleButton(
                        checked = scanSettingsUIState.scanSettingsState.feedDirection != null,
                        onCheckedChange = { enabled ->
                            // For now, just toggle between null and null since we don't know enum values
                            scanSettingsViewModel.setFeedDirection(null)
                        }
                    ) {
                        Text(scanSettingsUIState.scanSettingsState.feedDirection?.name ?: "Default")
                    }
                }

                // CCD Channel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.ccd_channel))
                    ToggleButton(
                        checked = scanSettingsUIState.scanSettingsState.ccdChannel != null,
                        onCheckedChange = { enabled ->
                            // For now, just toggle between null and null since we don't know enum values
                            scanSettingsViewModel.setCcdChannel(null)
                        }
                    ) {
                        Text(scanSettingsUIState.scanSettingsState.ccdChannel?.name ?: "Default")
                    }
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
                                Timber.tag(TAG).d("New region state: ${scanSettingsUIState.scanSettingsState.scanRegions}")
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
                    Column {
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
                        Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                            ValidatedDimensionsTextEdit(
                                scanSettingsUIState.xOffsetTextFieldString,
                                context,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                                stringResource(R.string.x_offset_in_mm),
                                { newText: String ->
                                    scanSettingsViewModel.setXOffsetTextFieldContent(newText)
                                },
                                { newXOffset: String ->
                                    scanSettingsViewModel.setOffset(
                                        newXOffset,
                                        scanSettingsUIState.yOffsetTextFieldString
                                    )
                                },
                                min = 0.0,
                                max = scanSettingsUIState.selectedInputSourceCapabilities.maxWidth.toMillimeters().value
                            )
                            ValidatedDimensionsTextEdit(
                                scanSettingsUIState.yOffsetTextFieldString,
                                context,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp),
                                stringResource(R.string.y_offset_in_mm),
                                { newText: String ->
                                    scanSettingsViewModel.setYOffsetTextFieldContent(newText)
                                },
                                { newYOffset: String ->
                                    scanSettingsViewModel.setOffset(
                                        scanSettingsUIState.xOffsetTextFieldString,
                                        newYOffset
                                    )
                                },
                                min = 0.0,
                                max = scanSettingsUIState.selectedInputSourceCapabilities.maxHeight.toMillimeters().value
                            )
                        }
                    }
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
                    stringResource(R.string.effects),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                // Brightness
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.brightness))
                    Slider(
                        value = (scanSettingsUIState.scanSettingsState.brightness ?: 50u).toFloat(),
                        onValueChange = { scanSettingsViewModel.setBrightness(it.toUInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Contrast
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.contrast))
                    Slider(
                        value = (scanSettingsUIState.scanSettingsState.contrast ?: 50u).toFloat(),
                        onValueChange = { scanSettingsViewModel.setContrast(it.toUInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Sharpen
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.sharpen))
                    Slider(
                        value = (scanSettingsUIState.scanSettingsState.sharpen ?: 50u).toFloat(),
                        onValueChange = { scanSettingsViewModel.setSharpen(it.toUInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Gamma
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.gamma))
                    Slider(
                        value = (scanSettingsUIState.scanSettingsState.gamma ?: 50u).toFloat(),
                        onValueChange = { scanSettingsViewModel.setGamma(it.toUInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Highlight
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.highlight))
                    Slider(
                        value = (scanSettingsUIState.scanSettingsState.highlight ?: 50u).toFloat(),
                        onValueChange = { scanSettingsViewModel.setHighlight(it.toUInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Noise Removal
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.noise_removal))
                    Slider(
                        value = (scanSettingsUIState.scanSettingsState.noiseRemoval ?: 50u).toFloat(),
                        onValueChange = { scanSettingsViewModel.setNoiseRemoval(it.toUInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Shadow
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.shadow))
                    Slider(
                        value = (scanSettingsUIState.scanSettingsState.shadow ?: 50u).toFloat(),
                        onValueChange = { scanSettingsViewModel.setShadow(it.toUInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Compression Factor
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(stringResource(R.string.compression_factor))
                    Slider(
                        value = (scanSettingsUIState.scanSettingsState.compressionFactor ?: 50u).toFloat(),
                        onValueChange = { scanSettingsViewModel.setCompressionFactor(it.toUInt()) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
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
