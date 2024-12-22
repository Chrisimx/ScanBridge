/*
 *     Copyright (C) 2024 Christian Nagel and contributors
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
import android.content.Context.CLIPBOARD_SERVICE
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.InputChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.chrisimx.esclkt.DiscreteResolution
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.InputSourceCaps
import io.github.chrisimx.esclkt.ScanIntentData
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import java.io.ByteArrayInputStream

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class,
    ExperimentalFoundationApi::class
)

enum class ErrorState {
    NOT_WITHIN_ALLOWED_RANGE,
    NOT_VALID_NUMBER,
    NO_ERROR
}

fun ErrorState.toHumanString(): String {
    return when (this) {
        ErrorState.NOT_WITHIN_ALLOWED_RANGE -> "Not in allowed range"
        ErrorState.NOT_VALID_NUMBER -> "Not a valid number"
        ErrorState.NO_ERROR -> "Valid"
    }
}

data class ScanSettingsComposableData(
    val scanSettingsState: MutableESCLScanSettingsState,
    val capabilities: ScannerCapabilities,
) {
    val inputSourceOptions: List<Pair<String, InputSource>> = capabilities.getInputSourceOptions()
    val paperFormats: List<PaperFormat> = loadDefaultFormats()
    val duplexAdfSupported = capabilities.adf?.duplexCaps != null

    val customMenuEnabledState = mutableStateOf(false)
    var customMenuEnabled by customMenuEnabledState

    val widthTextFieldState = mutableStateOf("")
    var widthTextFieldString by widthTextFieldState
    val heightTextFieldState = mutableStateOf("")
    var heightTextFieldString by heightTextFieldState

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

    fun toImmutable(): ImmutableScanSettingsComposableData {
        return ImmutableScanSettingsComposableData(
            scanSettingsState.toImmutable(),
            capabilities,
            inputSourceOptions,
            paperFormats,
            duplexAdfSupported,
            widthTextFieldState,
            heightTextFieldState,
            customMenuEnabledState,
            selectedInputSourceCapabilitiesState,
            intentOptionsState,
            supportedScanResolutionsState
        )
    }
}

data class ImmutableScanSettingsComposableData(
    val scanSettingsState: ImmutableESCLScanSettingsState,
    val capabilities: ScannerCapabilities,
    val inputSourceOptions: List<Pair<String, InputSource>>,
    val paperFormats: List<PaperFormat>,
    val duplexAdfSupported: Boolean,
    private val widthTextFieldState: State<String>,
    private val heightTextFieldState: State<String>,
    private val customMenuEnabledState: State<Boolean>,
    private val selectedInputSourceCapabilitiesState: State<InputSourceCaps>,
    private val intentOptionsState: State<List<ScanIntentData>>,
    private val supportedScanResolutionsState: State<List<DiscreteResolution>>
) {
    val customMenuEnabled by customMenuEnabledState
    val widthTextFieldString by widthTextFieldState
    val heightTextFieldString by heightTextFieldState
    val selectedInputSourceCapabilities by selectedInputSourceCapabilitiesState
    val intentOptions by intentOptionsState
    val supportedScanResolutions by supportedScanResolutionsState
}

class ScanSettingsViewModel(
    scanSettingsState: MutableESCLScanSettingsState,
    capabilities: ScannerCapabilities
) : ViewModel() {
    private val _scanSettingsComposableData =
        ScanSettingsComposableData(scanSettingsState, capabilities)
    val scanSettingsComposableData: ImmutableScanSettingsComposableData
        get() = _scanSettingsComposableData.toImmutable()


    fun setDuplex(duplex: Boolean) {
        _scanSettingsComposableData.scanSettingsState.duplex = duplex
    }

    fun setInputSourceOptions(inputSource: InputSource) {
        _scanSettingsComposableData.scanSettingsState.inputSource = inputSource
    }

    fun setResolution(xResolution: UInt, yResolution: UInt) {
        _scanSettingsComposableData.scanSettingsState.xResolution = xResolution
        _scanSettingsComposableData.scanSettingsState.yResolution = yResolution
    }

    fun setIntent(intent: ScanIntentData?) {
        _scanSettingsComposableData.scanSettingsState.intent = intent
    }

    fun setCustomMenuEnabled(enabled: Boolean) {
        _scanSettingsComposableData.customMenuEnabled = enabled
    }

    fun setWidthTextFieldContent(width: String) {
        _scanSettingsComposableData.widthTextFieldString = width
    }

    fun setHeightTextFieldContent(width: String) {
        _scanSettingsComposableData.heightTextFieldString = width
    }

    fun setRegionDimension(width: String, height: String) {
        if (_scanSettingsComposableData.scanSettingsState.scanRegions == null) {
            _scanSettingsComposableData.scanSettingsState.scanRegions = MutableScanRegionState(
                widthState = mutableStateOf(width),
                heightState = mutableStateOf(height),
                xOffsetState = mutableStateOf("0"),
                yOffsetState = mutableStateOf("0")
            )
            return // We don't want to set the width and height twice
        }
        _scanSettingsComposableData.scanSettingsState.scanRegions!!.width = width.toString()
        _scanSettingsComposableData.scanSettingsState.scanRegions!!.height = height.toString()
    }

    fun setOffset(xOffset: String, yOffset: String) {
        if (_scanSettingsComposableData.scanSettingsState.scanRegions == null) {
            _scanSettingsComposableData.scanSettingsState.scanRegions = MutableScanRegionState(
                widthState = mutableStateOf("0"),
                heightState = mutableStateOf("0"),
                xOffsetState = mutableStateOf(xOffset),
                yOffsetState = mutableStateOf(yOffset)
            )
            return // We don't want to set the width and height twice
        }
        _scanSettingsComposableData.scanSettingsState.scanRegions!!.xOffset = xOffset.toString()
        _scanSettingsComposableData.scanSettingsState.scanRegions!!.yOffset = yOffset.toString()
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanSettingsUI(modifier: Modifier, scanSettingsViewModel: ScanSettingsViewModel = viewModel()) {
    val scanSettingsUIState = scanSettingsViewModel.scanSettingsComposableData

    assert(scanSettingsUIState.inputSourceOptions.isNotEmpty()) // The settings are useless if this is the case

    Column(
        modifier
            .fillMaxWidth()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Input source:")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SingleChoiceSegmentedButtonRow {
                scanSettingsUIState.inputSourceOptions.forEachIndexed { index, inputSourcePair ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = scanSettingsUIState.inputSourceOptions.size
                        ),
                        onClick = { scanSettingsViewModel.setInputSourceOptions(inputSourcePair.second) },
                        selected = scanSettingsUIState.scanSettingsState.inputSource == inputSourcePair.second
                    ) {
                        Text(inputSourcePair.first)
                    }
                }
            }
            val duplexAvailable =
                scanSettingsUIState.duplexAdfSupported && scanSettingsUIState.scanSettingsState.inputSource == InputSource.Feeder
            ToggleButton(
                enabled = duplexAvailable,
                checked = scanSettingsUIState.scanSettingsState.duplex == true,
                onCheckedChange = { scanSettingsViewModel.setDuplex(it) },
            ) { Text("Duplex") }
        }
        Text("Used Resolution (dpi):")
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
                    selected = scanSettingsUIState.scanSettingsState.xResolution == discreteResolution.xResolution && scanSettingsUIState.scanSettingsState.yResolution == discreteResolution.yResolution
                ) {
                    if (discreteResolution.xResolution == discreteResolution.yResolution)
                        Text("${discreteResolution.xResolution}")
                    else
                        Text("${discreteResolution.xResolution}x${discreteResolution.yResolution}")
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Intent:", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

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
                            selected = scanSettingsUIState.scanSettingsState.intent == intentData,
                        )
                    }
                    InputChip(
                        onClick = {
                            scanSettingsViewModel.setIntent(null)
                        },
                        label = { Text("None") },
                        selected = scanSettingsUIState.scanSettingsState.intent == null,
                    )
                }
            }

        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Scan Region:",
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
                                    "ScanSettings",
                                    "New region state: ${scanSettingsUIState.scanSettingsState.scanRegions}"
                                )
                            },
                            label = { Text(paperFormat.name) },
                            selected = scanSettingsUIState.scanSettingsState.scanRegions?.width == paperFormat.width.toMillimeters().value.toString()
                                    && scanSettingsUIState.scanSettingsState.scanRegions?.height == paperFormat.height.toMillimeters().value.toString()
                                    && !scanSettingsUIState.customMenuEnabled,
                        )
                    }
                    InputChip(
                        onClick = {
                            scanSettingsViewModel.setCustomMenuEnabled(false)
                            scanSettingsViewModel.setRegionDimension("max", "max")
                        },
                        label = { Text("Maximum size") },
                        selected = scanSettingsUIState.scanSettingsState.scanRegions?.width == "max" && !scanSettingsUIState.customMenuEnabled,
                    )
                    InputChip(
                        selected = scanSettingsUIState.customMenuEnabled,
                        onClick = { scanSettingsViewModel.setCustomMenuEnabled(true) },
                        label = { Text("Custom") }
                    )
                }
                if (scanSettingsUIState.customMenuEnabled) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                        ValidatedDimensionsTextEdit(
                            scanSettingsUIState.widthTextFieldString,
                            modifier = Modifier.weight(1f),
                            "Width (in mm)",
                            { scanSettingsViewModel.setWidthTextFieldContent(it) },
                            {
                                scanSettingsViewModel.setRegionDimension(
                                    it,
                                    scanSettingsUIState.heightTextFieldString
                                )
                            },
                            min = scanSettingsUIState.selectedInputSourceCapabilities.minWidth.toMillimeters().value,
                            max = scanSettingsUIState.selectedInputSourceCapabilities.maxWidth.toMillimeters().value
                        )
                        ValidatedDimensionsTextEdit(
                            scanSettingsUIState.heightTextFieldString,
                            modifier = Modifier.weight(1f),
                            "Height (in mm)",
                            { scanSettingsViewModel.setHeightTextFieldContent(it) },
                            {
                                scanSettingsViewModel.setRegionDimension(
                                    scanSettingsUIState.widthTextFieldString,
                                    it
                                )
                            },
                            min = scanSettingsUIState.selectedInputSourceCapabilities.minWidth.toMillimeters().value,
                            max = scanSettingsUIState.selectedInputSourceCapabilities.maxWidth.toMillimeters().value
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
                    "Scan settings",
                    scanSettingsString
                )
            )
        }) {
            Text("Copy current scanner options in eSCLKt format")
        }
    }
}

@Composable
fun ValidatedDimensionsTextEdit(
    localContent: String,
    modifier: Modifier = Modifier,
    label: String,
    updateContent: (String) -> Unit,
    updateDimensionState: (String) -> Unit,
    min: Double,
    max: Double
) {
    val errorState = remember { mutableStateOf(ErrorState.NO_ERROR) }

    val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator
    val decimalNumberRegex =
        "^[+]?\\d*(${Regex.escape(decimalSeparator.toString())})?\\d+\$".toRegex()

    TextField(
        modifier = modifier,
        value = localContent,
        onValueChange = { newValue: String ->
            updateContent(newValue)

            val isValidNumber = newValue.matches(decimalNumberRegex)
            if (isValidNumber) {
                val newNumber = DecimalFormat.getInstance().parse(newValue).toDouble()
                if (newNumber > max || newNumber < min) {
                    errorState.value = ErrorState.NOT_WITHIN_ALLOWED_RANGE
                    return@TextField
                }
                errorState.value = ErrorState.NO_ERROR
                updateDimensionState(newValue)

                return@TextField
            } else {
                errorState.value = ErrorState.NOT_VALID_NUMBER
                Log.d("ScanSettings", "Invalid Number")
                return@TextField
            }

        },
        supportingText = { if (errorState.value != ErrorState.NO_ERROR) Text(errorState.value.toHumanString()) },
        isError = errorState.value != ErrorState.NO_ERROR,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Decimal
        ),
        label = @Composable { Text(label) },
        singleLine = true
    )

}

val scannerCapabilities =
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<scan:ScannerCapabilities\n        xmlns:scan=\"http://schemas.hp.com/imaging/escl/2011/05/03\"\n        xmlns:pwg=\"http://www.pwg.org/schemas/2010/12/sm\"\n        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n        xsi:schemaLocation=\"http://schemas.hp.com/imaging/escl/2011/05/03 ../../schemas/eSCL-1_92.xsd\">\n    <pwg:Version>2.62</pwg:Version>\n    <pwg:MakeAndModel>Brother MFC-L8690CDW series</pwg:MakeAndModel>\n    <pwg:SerialNumber>...</pwg:SerialNumber>\n    <scan:UUID>00000000-0000-1000-8000-0018d7024a10</scan:UUID>\n    <scan:AdminURI>http://192.168.200.122/net/net/airprint.html</scan:AdminURI>\n    <scan:IconURI>http://192.168.200.122/icons/device-icons-128.png</scan:IconURI>\n    <scan:Platen>\n        <scan:PlatenInputCaps>\n            <scan:MinWidth>16</scan:MinWidth>\n            <scan:MaxWidth>2550</scan:MaxWidth>\n            <scan:MinHeight>16</scan:MinHeight>\n            <scan:MaxHeight>3507</scan:MaxHeight>\n            <scan:MaxScanRegions>1</scan:MaxScanRegions>\n            <scan:SettingProfiles>\n                <scan:SettingProfile>\n                    <scan:ColorModes>\n                        <scan:ColorMode>BlackAndWhite1</scan:ColorMode>\n                        <scan:ColorMode>Grayscale8</scan:ColorMode>\n                        <scan:ColorMode>RGB24</scan:ColorMode>\n                    </scan:ColorModes>\n                    <scan:DocumentFormats>\n                        <pwg:DocumentFormat>application/pdf</pwg:DocumentFormat>\n                        <pwg:DocumentFormat>image/jpeg</pwg:DocumentFormat>\n                        <scan:DocumentFormatExt>application/pdf</scan:DocumentFormatExt>\n                        <scan:DocumentFormatExt>image/jpeg</scan:DocumentFormatExt>\n                    </scan:DocumentFormats>\n                    <scan:SupportedResolutions>\n                        <scan:DiscreteResolutions>\n                            <scan:DiscreteResolution>\n                                <scan:XResolution>100</scan:XResolution>\n                                <scan:YResolution>100</scan:YResolution>\n                            </scan:DiscreteResolution>\n                            <scan:DiscreteResolution>\n                                <scan:XResolution>200</scan:XResolution>\n                                <scan:YResolution>200</scan:YResolution>\n                            </scan:DiscreteResolution>\n                            <scan:DiscreteResolution>\n                                <scan:XResolution>300</scan:XResolution>\n                                <scan:YResolution>300</scan:YResolution>\n                            </scan:DiscreteResolution>\n                            <scan:DiscreteResolution>\n                                <scan:XResolution>600</scan:XResolution>\n                                <scan:YResolution>600</scan:YResolution>\n                            </scan:DiscreteResolution>\n                        </scan:DiscreteResolutions>\n                    </scan:SupportedResolutions>\n                    <scan:ColorSpaces>\n                        <scan:ColorSpace>CMYK</scan:ColorSpace>\n                        <scan:ColorSpace>YCC</scan:ColorSpace>\n                        <scan:ColorSpace>sRGB</scan:ColorSpace>\n                    </scan:ColorSpaces>\n                    <scan:CcdChannels>\n                        <scan:CcdChannel>Red</scan:CcdChannel>\n                        <scan:CcdChannel>Green</scan:CcdChannel>\n                        <scan:CcdChannel>Blue</scan:CcdChannel>\n                        <scan:CcdChannel>NTSC</scan:CcdChannel>\n                        <scan:CcdChannel>GrayCcd</scan:CcdChannel>\n                        <scan:CcdChannel>GrayCcdEmulated</scan:CcdChannel>\n                    </scan:CcdChannels>\n                    <scan:BinaryRenderings>\n                        <scan:BinaryRendering>Halftone</scan:BinaryRendering>\n                        <scan:BinaryRendering>Threshold</scan:BinaryRendering>\n                    </scan:BinaryRenderings>\n                </scan:SettingProfile>\n            </scan:SettingProfiles>\n            <scan:SupportedIntents>\n                <scan:Intent>Document</scan:Intent>\n                <scan:Intent>TextAndGraphic</scan:Intent>\n                <scan:Intent>Photo</scan:Intent>\n                <scan:Intent>Preview</scan:Intent>\n            </scan:SupportedIntents>\n            <scan:EdgeAutoDetection>\n                <scan:SupportedEdge>TopEdge</scan:SupportedEdge>\n                <scan:SupportedEdge>LeftEdge</scan:SupportedEdge>\n                <scan:SupportedEdge>BottomEdge</scan:SupportedEdge>\n                <scan:SupportedEdge>RightEdge</scan:SupportedEdge>\n            </scan:EdgeAutoDetection>\n            <scan:MaxOpticalXResolution>2400</scan:MaxOpticalXResolution>\n            <scan:MaxOpticalYResolution>2400</scan:MaxOpticalYResolution>\n            <scan:RiskyLeftMargin>0</scan:RiskyLeftMargin>\n            <scan:RiskyRightMargin>0</scan:RiskyRightMargin>\n            <scan:RiskyTopMargin>0</scan:RiskyTopMargin>\n            <scan:RiskyBottomMargin>0</scan:RiskyBottomMargin>\n            <scan:MaxPhysicalWidth>2550</scan:MaxPhysicalWidth>\n            <scan:MaxPhysicalHeight>3507</scan:MaxPhysicalHeight>\n        </scan:PlatenInputCaps>\n    </scan:Platen>\n    <scan:Adf>\n        <scan:AdfSimplexInputCaps>\n            <scan:MinWidth>16</scan:MinWidth>\n            <scan:MaxWidth>2550</scan:MaxWidth>\n            <scan:MinHeight>16</scan:MinHeight>\n            <scan:MaxHeight>4200</scan:MaxHeight>\n            <scan:MaxScanRegions>1</scan:MaxScanRegions>\n            <scan:SettingProfiles>\n                <scan:SettingProfile>\n                    <scan:ColorModes>\n                        <scan:ColorMode>BlackAndWhite1</scan:ColorMode>\n                        <scan:ColorMode>Grayscale8</scan:ColorMode>\n                        <scan:ColorMode>RGB24</scan:ColorMode>\n                    </scan:ColorModes>\n                    <scan:DocumentFormats>\n                        <pwg:DocumentFormat>application/pdf</pwg:DocumentFormat>\n                        <pwg:DocumentFormat>image/jpeg</pwg:DocumentFormat>\n                        <scan:DocumentFormatExt>application/pdf</scan:DocumentFormatExt>\n                        <scan:DocumentFormatExt>image/jpeg</scan:DocumentFormatExt>\n                    </scan:DocumentFormats>\n                    <scan:SupportedResolutions>\n                        <scan:DiscreteResolutions>\n                            <scan:DiscreteResolution>\n                                <scan:XResolution>100</scan:XResolution>\n                                <scan:YResolution>100</scan:YResolution>\n                            </scan:DiscreteResolution>\n                            <scan:DiscreteResolution>\n                                <scan:XResolution>200</scan:XResolution>\n                                <scan:YResolution>200</scan:YResolution>\n                            </scan:DiscreteResolution>\n                            <scan:DiscreteResolution>\n                                <scan:XResolution>300</scan:XResolution>\n                                <scan:YResolution>300</scan:YResolution>\n                            </scan:DiscreteResolution>\n                            <scan:DiscreteResolution>\n                                <scan:XResolution>600</scan:XResolution>\n                                <scan:YResolution>600</scan:YResolution>\n                            </scan:DiscreteResolution>\n                        </scan:DiscreteResolutions>\n                    </scan:SupportedResolutions>\n                    <scan:ColorSpaces>\n                        <scan:ColorSpace>CMYK</scan:ColorSpace>\n                        <scan:ColorSpace>YCC</scan:ColorSpace>\n                        <scan:ColorSpace>sRGB</scan:ColorSpace>\n                    </scan:ColorSpaces>\n                    <scan:CcdChannels>\n                        <scan:CcdChannel>Red</scan:CcdChannel>\n                        <scan:CcdChannel>Green</scan:CcdChannel>\n                        <scan:CcdChannel>Blue</scan:CcdChannel>\n                        <scan:CcdChannel>NTSC</scan:CcdChannel>\n                        <scan:CcdChannel>GrayCcd</scan:CcdChannel>\n                        <scan:CcdChannel>GrayCcdEmulated</scan:CcdChannel>\n                    </scan:CcdChannels>\n                    <scan:BinaryRenderings>\n                        <scan:BinaryRendering>Halftone</scan:BinaryRendering>\n                        <scan:BinaryRendering>Threshold</scan:BinaryRendering>\n                    </scan:BinaryRenderings>\n                </scan:SettingProfile>\n            </scan:SettingProfiles>\n            <scan:SupportedIntents>\n                <scan:Intent>Document</scan:Intent>\n                <scan:Intent>TextAndGraphic</scan:Intent>\n                <scan:Intent>Photo</scan:Intent>\n                <scan:Intent>Preview</scan:Intent>\n            </scan:SupportedIntents>\n            <scan:EdgeAutoDetection>\n                <scan:SupportedEdge>TopEdge</scan:SupportedEdge>\n                <scan:SupportedEdge>LeftEdge</scan:SupportedEdge>\n                <scan:SupportedEdge>BottomEdge</scan:SupportedEdge>\n                <scan:SupportedEdge>RightEdge</scan:SupportedEdge>\n            </scan:EdgeAutoDetection>\n            <scan:MaxOpticalXResolution>2400</scan:MaxOpticalXResolution>\n            <scan:MaxOpticalYResolution>2400</scan:MaxOpticalYResolution>\n            <scan:RiskyLeftMargin>0</scan:RiskyLeftMargin>\n            <scan:RiskyRightMargin>0</scan:RiskyRightMargin>\n            <scan:RiskyTopMargin>0</scan:RiskyTopMargin>\n            <scan:RiskyBottomMargin>0</scan:RiskyBottomMargin>\n            <scan:MaxPhysicalWidth>2550</scan:MaxPhysicalWidth>\n            <scan:MaxPhysicalHeight>4200</scan:MaxPhysicalHeight>\n        </scan:AdfSimplexInputCaps>\n        <scan:AdfDuplexInputCaps>\n            <scan:MinWidth>16</scan:MinWidth>\n            <scan:MaxWidth>2550</scan:MaxWidth>\n            <scan:MinHeight>16</scan:MinHeight>\n            <scan:MaxHeight>4200</scan:MaxHeight>\n            <scan:MaxScanRegions>1</scan:MaxScanRegions>\n            <scan:SettingProfiles>\n                <scan:SettingProfile>\n                    <scan:ColorModes>\n                        <scan:ColorMode>BlackAndWhite1</scan:ColorMode>\n                        <scan:ColorMode>Grayscale8</scan:ColorMode>\n                        <scan:ColorMode>RGB24</scan:ColorMode>\n                    </scan:ColorModes>\n                    <scan:DocumentFormats>\n                        <pwg:DocumentFormat>application/pdf</pwg:DocumentFormat>\n                        <pwg:DocumentFormat>image/jpeg</pwg:DocumentFormat>\n                        <scan:DocumentFormatExt>application/pdf</scan:DocumentFormatExt>\n                        <scan:DocumentFormatExt>image/jpeg</scan:DocumentFormatExt>\n                    </scan:DocumentFormats>\n                    <scan:SupportedResolutions>\n                        <scan:DiscreteResolutions>\n                            <scan:DiscreteResolution>\n                                <scan:XResolution>100</scan:XResolution>\n                                <scan:YResolution>100</scan:YResolution>\n                            </scan:DiscreteResolution>\n                            <scan:DiscreteResolution>\n                                <scan:XResolution>200</scan:XResolution>\n                                <scan:YResolution>200</scan:YResolution>\n                            </scan:DiscreteResolution>\n                            <scan:DiscreteResolution>\n                                <scan:XResolution>300</scan:XResolution>\n                                <scan:YResolution>300</scan:YResolution>\n                            </scan:DiscreteResolution>\n                            <scan:DiscreteResolution>\n                                <scan:XResolution>600</scan:XResolution>\n                                <scan:YResolution>600</scan:YResolution>\n                            </scan:DiscreteResolution>\n                        </scan:DiscreteResolutions>\n                    </scan:SupportedResolutions>\n                    <scan:ColorSpaces>\n                        <scan:ColorSpace>CMYK</scan:ColorSpace>\n                        <scan:ColorSpace>YCC</scan:ColorSpace>\n                        <scan:ColorSpace>sRGB</scan:ColorSpace>\n                    </scan:ColorSpaces>\n                    <scan:CcdChannels>\n                        <scan:CcdChannel>Red</scan:CcdChannel>\n                        <scan:CcdChannel>Green</scan:CcdChannel>\n                        <scan:CcdChannel>Blue</scan:CcdChannel>\n                        <scan:CcdChannel>NTSC</scan:CcdChannel>\n                        <scan:CcdChannel>GrayCcd</scan:CcdChannel>\n                        <scan:CcdChannel>GrayCcdEmulated</scan:CcdChannel>\n                    </scan:CcdChannels>\n                    <scan:BinaryRenderings>\n                        <scan:BinaryRendering>Halftone</scan:BinaryRendering>\n                        <scan:BinaryRendering>Threshold</scan:BinaryRendering>\n                    </scan:BinaryRenderings>\n                </scan:SettingProfile>\n            </scan:SettingProfiles>\n            <scan:SupportedIntents>\n                <scan:Intent>Document</scan:Intent>\n                <scan:Intent>TextAndGraphic</scan:Intent>\n                <scan:Intent>Photo</scan:Intent>\n                <scan:Intent>Preview</scan:Intent>\n            </scan:SupportedIntents>\n            <scan:EdgeAutoDetection>\n                <scan:SupportedEdge>TopEdge</scan:SupportedEdge>\n                <scan:SupportedEdge>LeftEdge</scan:SupportedEdge>\n                <scan:SupportedEdge>BottomEdge</scan:SupportedEdge>\n                <scan:SupportedEdge>RightEdge</scan:SupportedEdge>\n            </scan:EdgeAutoDetection>\n            <scan:MaxOpticalXResolution>1200</scan:MaxOpticalXResolution>\n            <scan:MaxOpticalYResolution>2400</scan:MaxOpticalYResolution>\n            <scan:RiskyLeftMargin>0</scan:RiskyLeftMargin>\n            <scan:RiskyRightMargin>0</scan:RiskyRightMargin>\n            <scan:RiskyTopMargin>0</scan:RiskyTopMargin>\n            <scan:RiskyBottomMargin>0</scan:RiskyBottomMargin>\n            <scan:MaxPhysicalWidth>2550</scan:MaxPhysicalWidth>\n            <scan:MaxPhysicalHeight>4200</scan:MaxPhysicalHeight>\n        </scan:AdfDuplexInputCaps>\n        <scan:FeederCapacity>20</scan:FeederCapacity>\n        <scan:AdfOptions>\n            <scan:AdfOption>DetectPaperLoaded</scan:AdfOption>\n            <scan:AdfOption>Duplex</scan:AdfOption>\n        </scan:AdfOptions>\n    </scan:Adf>\n    <scan:StoredJobRequestSupport>\n        <scan:MaxStoredjobRequests>0</scan:MaxStoredjobRequests>\n        <scan:TimeoutInSeconds>0</scan:TimeoutInSeconds>\n    </scan:StoredJobRequestSupport>\n</scan:ScannerCapabilities>"

@Preview(showBackground = true)
@Composable
fun ScanSettingsPreview() {
    val testScannerCapabilities = ByteArrayInputStream(scannerCapabilities.toByteArray())
    ScanBridgeTheme(darkTheme = true) {
        Scaffold { innerPadding ->
            val scannerCapabilities = ScannerCapabilities.fromXML(testScannerCapabilities)
            val scanSettings = remember {
                MutableESCLScanSettingsState(
                    versionState = mutableStateOf(scannerCapabilities.interfaceVersion),
                    scanRegionsState = mutableStateOf(
                        MutableScanRegionState(
                            mutableStateOf("0"),
                            mutableStateOf("0"),
                            mutableStateOf("0"),
                            mutableStateOf("0")
                        )
                    )
                )
            }
            ScanSettingsUI(
                Modifier.padding(innerPadding),
                viewModel {
                    ScanSettingsViewModel(scanSettings, scannerCapabilities)
                })
        }
    }

}