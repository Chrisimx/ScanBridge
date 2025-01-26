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

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import io.github.chrisimx.scanbridge.theme.Poppins
import io.github.chrisimx.scanbridge.theme.Teal1
import io.github.chrisimx.scanbridge.theme.gradientBrush

@Composable
fun VersionComposable() {
    val context = LocalContext.current

    Image(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp),
        painter = painterResource(R.drawable.icon_about_dialog),
        contentDescription = stringResource(id = R.string.app_icon_desc)
    )

    Text(
        stringResource(R.string.app_name),
        modifier = Modifier.padding(PaddingValues(4.dp)),
        fontFamily = Poppins,
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        style = MaterialTheme.typography.labelLarge.copy(
            brush = gradientBrush
        )
    )

    Text(
        "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}, ${BuildConfig.GIT_COMMIT_HASH})",
        fontStyle = FontStyle.Normal,
        fontFamily = Poppins
    )

    if (BuildConfig.DEBUG) {
        Text(
            stringResource(R.string.debug_build),
            fontStyle = FontStyle.Italic,
            fontFamily = Poppins
        )
    }
}

@Composable
fun AutoDeleteTempFiles(
    sharedPreferences: SharedPreferences,
    automaticCleanup: Boolean,
    onInformationRequested: (String) -> Unit,
    setAutomaticCleanup: (Boolean) -> Unit
) {
    ConstraintLayout(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .toggleable(
                value = automaticCleanup,
                onValueChange = {
                    sharedPreferences
                        .edit()
                        .putBoolean("auto_cleanup", it)
                        .apply()
                    setAutomaticCleanup(it)
                },
                role = Role.Checkbox
            )
    ) {
        val (checkbox, content, informationButton) = createRefs()

        Checkbox(
            checked = automaticCleanup,
            onCheckedChange = null,
            modifier = Modifier
                .constrainAs(checkbox) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
        )
        Text(
            text = stringResource(R.string.auto_cleanup),
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
        val context = LocalContext.current
        Box(
            modifier = Modifier
                .constrainAs(informationButton) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }
        ) {
            IconButton(onClick = {
                onInformationRequested(
                    context.getString(
                        R.string.auto_cleanup_explanation,
                        context.getString(R.string.app_name)
                    )
                )
            }) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.auto_cleanup_info_desc)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun AppSettingsScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current

    val sharedPreferences = remember {
        context.getSharedPreferences("scanbridge", MODE_PRIVATE)
    }

    var automaticCleanup by remember {
        mutableStateOf(
            sharedPreferences.getBoolean(
                "auto_cleanup",
                false
            )
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VersionComposable()

        HorizontalDivider(modifier = Modifier.padding(14.dp))

        var information: String? by remember {
            mutableStateOf(null)
        }

        ElevatedCard(modifier = Modifier.padding(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.settings),
                    modifier = Modifier.padding(bottom = 16.dp),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = Teal1
                )

                AutoDeleteTempFiles(
                    sharedPreferences,
                    automaticCleanup,
                    { information = it },
                    { automaticCleanup = it }
                )
            }
        }

        if (information != null) {
            Dialog(onDismissRequest = { information = null }) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            information!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
