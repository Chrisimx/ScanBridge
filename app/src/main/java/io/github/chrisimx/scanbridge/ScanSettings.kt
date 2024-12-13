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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.ScanIntentData
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.scanbridge.theme.ScanBridgeTheme
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class,
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
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScanSettingsUI(modifier: Modifier, capabilites: ScannerCapabilities, scanSettingsState: ScanSettingsState) {
    val inputSourceOptions = mutableListOf<Pair<String, InputSource>>()
    if (capabilites.platen != null) {
        inputSourceOptions.add(Pair("Platen", InputSource.Platen))
    }
    if (capabilites.adf != null) {
        inputSourceOptions.add(Pair("ADF", InputSource.Feeder))
    }

    assert(inputSourceOptions.isNotEmpty()) // The settings are useless if this is the case

    val duplexAdfSupported = capabilites.adf?.duplexCaps != null

    val selectedInputSourceCapabilities by remember {
        derivedStateOf {
            when (scanSettingsState.inputSource) {
                InputSource.Platen -> capabilites.platen!!.inputSourceCaps
                InputSource.Feeder -> if (scanSettingsState.duplex == true) capabilites.adf!!.duplexCaps!! else capabilites.adf!!.simplexCaps
                InputSource.Camera -> throw UnsupportedOperationException("Camera is not supported yet!")
                null -> capabilites.platen!!.inputSourceCaps // assumes default is Platen
            }
        }
    }

    val intentOptions by remember {
        derivedStateOf {
            selectedInputSourceCapabilities.supportedIntents
        }
    }
    val supportedScanResolutions by remember {
        derivedStateOf {
            selectedInputSourceCapabilities.settingProfiles[0].supportedResolutions
        }
    }

    Column( modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Input source:")
        Row(Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            SingleChoiceSegmentedButtonRow {
                inputSourceOptions.forEachIndexed { index, inputSourcePair ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = inputSourceOptions.size),
                        onClick = { scanSettingsState.inputSource = inputSourcePair.second },
                        selected = scanSettingsState.inputSource == inputSourcePair.second
                    ) {
                        Text(inputSourcePair.first)
                    }
                }
            }
            val duplexAvailable = duplexAdfSupported && scanSettingsState.inputSource == InputSource.Feeder
            ToggleButton(
                enabled = duplexAvailable,
                checked = scanSettingsState.duplex == true,
                onCheckedChange = { scanSettingsState.duplex = it},
            ) { Text("Duplex") }
        }
        Text("Used Resolution (dpi):")
        SingleChoiceSegmentedButtonRow {
            supportedScanResolutions.forEachIndexed { index, discreteResolution ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = supportedScanResolutions.size),
                    onClick = {
                        scanSettingsState.xResolution = discreteResolution.xResolution
                        scanSettingsState.yResolution = discreteResolution.yResolution
                              },
                    selected = scanSettingsState.xResolution == discreteResolution.xResolution && scanSettingsState.yResolution == discreteResolution.yResolution
                ) {
                    Text("${discreteResolution.xResolution}x${discreteResolution.yResolution}")
                }
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Intent:", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                FlowRow (Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    intentOptions.forEach { intentData ->
                        val name = when (intentData) {
                            is ScanIntentData.ScanIntentEnum -> intentData.scanIntent.name
                            is ScanIntentData.StringData -> intentData.string
                        }
                        InputChip(
                            onClick = {
                                scanSettingsState.intent = intentData
                            },
                            label = { Text(name) },
                            selected = scanSettingsState.intent == intentData,
                        )
                    }
                    InputChip(
                        onClick = {
                            scanSettingsState.intent = null
                        },
                        label = { Text("None") },
                        selected = scanSettingsState.intent == null,
                    )
                }
            }

        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Scan Region:", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

                FlowRow (Modifier.fillMaxWidth(),

                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    intentOptions.forEach { intentData ->
                        val name = when (intentData) {
                            is ScanIntentData.ScanIntentEnum -> intentData.scanIntent.name
                            is ScanIntentData.StringData -> intentData.string
                        }
                        InputChip(
                            onClick = {
                                scanSettingsState.intent = intentData
                            },
                            label = { Text(name) },
                            selected = scanSettingsState.intent == intentData,
                        )
                    }
                    InputChip(
                        onClick = {
                            scanSettingsState.intent = null
                        },
                        label = { Text("Custom") },
                        selected = scanSettingsState.intent == null,
                    )
                }
            }

        }
        val localHeight = remember { mutableStateOf("") }
        val localWidth = remember { mutableStateOf("") }
        Row (horizontalArrangement = Arrangement.SpaceEvenly){
            ValidatedDimensionsTextEdit(localWidth, modifier = Modifier.weight(1f),"Width (in mm)", scanSettingsState.scanRegions!!,
                ScanRegionState::width, selectedInputSourceCapabilities.minWidth.threeHundredthsOfInchToMM(), selectedInputSourceCapabilities.maxWidth.threeHundredthsOfInchToMM())
            ValidatedDimensionsTextEdit(localHeight, modifier = Modifier.weight(1f), "Height (in mm)", scanSettingsState.scanRegions!!,
                ScanRegionState::height, selectedInputSourceCapabilities.minHeight.threeHundredthsOfInchToMM(), selectedInputSourceCapabilities.maxHeight.threeHundredthsOfInchToMM())
        }
        Button(onClick = {
            localWidth.value = DecimalFormat.getInstance().format(selectedInputSourceCapabilities.maxWidth.threeHundredthsOfInchToMM())
            localHeight.value = DecimalFormat.getInstance().format(selectedInputSourceCapabilities.maxHeight.threeHundredthsOfInchToMM())
            Log.d("ScanSettings", localHeight.value)
        }) { Text("Set max") }
    }
}

@Composable
fun ValidatedDimensionsTextEdit(localContent: MutableState<String>, modifier: Modifier = Modifier, label: String, stateObject: ScanRegionState, stateProperty: KMutableProperty<String>, min: Double, max: Double) {
    val errorState = remember { mutableStateOf(ErrorState.NO_ERROR) }

    val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator
    val decimalNumberRegex = "^[+]?\\d*(${Regex.escape(decimalSeparator.toString())})?\\d+\$".toRegex()

    TextField(
        modifier = modifier,
        value = localContent.value,
        onValueChange = { newValue: String ->
            localContent.value = newValue

            val isValidNumber = newValue.matches(decimalNumberRegex)
            if (isValidNumber) {
                val newNumber = DecimalFormat.getInstance().parse(newValue).toDouble()
                if (newNumber > max || newNumber < min) {
                    errorState.value = ErrorState.NOT_WITHIN_ALLOWED_RANGE
                    return@TextField
                }
                errorState.value = ErrorState.NO_ERROR
                stateProperty.setter.call(stateObject, newValue)
                return@TextField
            } else {
                errorState.value = ErrorState.NOT_VALID_NUMBER
                Log.d("ScanSettings","Invalid Number")
                return@TextField
            }

        },
        supportingText = { if (errorState.value != ErrorState.NO_ERROR) Text(errorState.value.toHumanString())},
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
            val scanSettings = remember { ScanSettingsState(versionState = mutableStateOf(scannerCapabilities.interfaceVersion),
                scanRegionsState = mutableStateOf(ScanRegionState(mutableStateOf("0"),mutableStateOf("0"),mutableStateOf("0"),mutableStateOf("0")))) }
            ScanSettingsUI(Modifier.padding(innerPadding), scannerCapabilities, scanSettings)
        }
    }

}