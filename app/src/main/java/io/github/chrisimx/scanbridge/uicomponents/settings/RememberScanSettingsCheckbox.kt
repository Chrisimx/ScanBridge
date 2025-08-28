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

import android.content.Context.MODE_PRIVATE
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.edit
import io.github.chrisimx.scanbridge.util.ScanSettingsStore

@Composable
fun RememberScanSettingsCheckbox(
    settingsName: String,
    settingsText: String,
    helpText: String,
    default: Boolean = true,
    onInformationRequested: (String) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("scanbridge", MODE_PRIVATE)
    var checked by remember { mutableStateOf(sharedPreferences.getBoolean(settingsName, default)) }

    ConstraintLayout(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .toggleable(
                value = checked,
                onValueChange = {
                    sharedPreferences
                        .edit {
                            putBoolean(settingsName, it)
                        }
                    checked = it
                    
                    // Clear saved scan settings when user disables the setting
                    if (!it) {
                        ScanSettingsStore.clear(context)
                    }
                },
                role = Role.Checkbox
            )
    ) {
        val (checkbox, content, informationButton) = createRefs()

        Checkbox(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier
                .constrainAs(checkbox) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
        )
        Text(
            text = settingsText,
            modifier = Modifier
                .constrainAs(content) {
                    start.linkTo(checkbox.end, 12.dp)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(informationButton.start, 12.dp)
                    width = Dimension.fillToConstraints
                },
            style = MaterialTheme.typography.bodyMedium
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
                onInformationRequested(
                    helpText
                )
            }
        }
    }
}