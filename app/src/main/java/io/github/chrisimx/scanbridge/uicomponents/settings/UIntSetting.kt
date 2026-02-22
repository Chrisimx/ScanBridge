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

package io.github.chrisimx.scanbridge.uicomponents.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import timber.log.Timber

@Composable
fun UIntSettingBase(
    initialTimeout: UInt,
    settingName: String,
    placeholder: String,
    help: Int,
    onHelpRequested: (Int) -> Unit,
    changeSetting: (UInt) -> Unit,
    min: UInt,
    max: UInt,
) {
    ConstraintLayout(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        val (checkbox, informationButton) = createRefs()

        var timeoutValue by remember { mutableStateOf(initialTimeout.toString()) }

        OutlinedTextField(
            value = timeoutValue,
            onValueChange = {
                timeoutValue = it
                if (it.isNotEmpty()) {
                    try {
                        val value = it.toUInt()
                        if (value in min..max) {
                            changeSetting(value)
                        } else {
                            Timber.w("Value out of range: $value. Ignored")
                        }
                    } catch (_: NumberFormatException) {
                        Timber.w("Invalid setting value. Ignored")
                    }
                }
            },
            label = {
                Text(
                    settingName,
                    style = MaterialTheme.typography.labelMedium
                )
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            ),
            singleLine = true,
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .constrainAs(checkbox) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                }
                .padding(end = 0.dp)
        )
        Box(
            modifier = Modifier
                .constrainAs(informationButton) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }
        ) {
            MoreInformationButton {
                onHelpRequested(help)
            }
        }
    }
}

@Composable
fun UIntSetting(
    initialTimeout: UInt?,
    default: UInt,
    settingName: String,
    help: Int,
    onHelpRequested: (Int) -> Unit,
    setSetting: (UInt) -> Unit,
    min: UInt = 0u,
    max: UInt = UInt.MAX_VALUE
) {
    UIntSettingBase(
        initialTimeout ?: default,
        settingName,
        default.toString(),
        help,
        onHelpRequested,
        setSetting,
        min,
        max,
    )
}

/*@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Preview
@Composable
fun TimeoutOptionPreview() {
    ScanBridgeTheme {
        Scaffold {
            UIntSettingBase("Timeout", "25", "Timeout", {}, {}, 0u, 100u)
        }
    }
}
*/
