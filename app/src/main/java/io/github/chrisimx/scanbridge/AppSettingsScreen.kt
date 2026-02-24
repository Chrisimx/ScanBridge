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

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.net.toUri
import io.github.chrisimx.scanbridge.datastore.*
import io.github.chrisimx.scanbridge.proto.ScanBridgeSettings
import io.github.chrisimx.scanbridge.proto.chunkSizePdfExportOrNull
import io.github.chrisimx.scanbridge.proto.rememberScanSettingsOrNull
import io.github.chrisimx.scanbridge.proto.scanningResponseTimeoutOrNull
import io.github.chrisimx.scanbridge.services.DebugLogService
import io.github.chrisimx.scanbridge.services.FileDebugLogService
import io.github.chrisimx.scanbridge.uicomponents.TitledCard
import io.github.chrisimx.scanbridge.uicomponents.dialog.SimpleTextDialog
import io.github.chrisimx.scanbridge.uicomponents.settings.CheckboxSetting
import io.github.chrisimx.scanbridge.uicomponents.settings.MoreInformationButton
import io.github.chrisimx.scanbridge.uicomponents.settings.UIntSetting
import io.github.chrisimx.scanbridge.uicomponents.settings.VersionComposable
import java.io.File
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.compose.activity.koinActivityInject
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun DisableCertChecksSetting(
    onInformationRequested: (Int) -> Unit,
    checked: Boolean,
    setChecked: (Boolean) -> Unit,
) {
    CheckboxSetting(
        stringResource(R.string.disable_cert_checks),
        R.string.disable_cert_checks_desc,
        checked,
        setChecked
    ) {
        onInformationRequested(it)
    }
}

fun exportDebugLog(context: Context, debugLogService: DebugLogService, saveDebugLogLauncher: ActivityResultLauncher<Intent>) {
    debugLogService.flush()

    val debugDir = File(context.filesDir, "debug")
    val debugFile = File(debugDir, "debug.txt")

    if (debugFile.exists()) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TITLE, "debug_log.txt") // Suggested file name

        saveDebugLogLauncher.launch(intent)
    }
}

@Composable
fun DebugOptions(
    debugLog: Boolean,
    onInformationRequested: (Int) -> Unit,
    setWriteDebugLog: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val debugLogService: DebugLogService = koinInject()

    val debugFileSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {  result ->
        if (result.resultCode == RESULT_OK) {
            val uri = result.data?.data
            uri?.let {
                debugLogService.saveToFile(it)
            }
        }
    }

    ConstraintLayout(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .toggleable(
                value = debugLog,
                onValueChange = {
                    setWriteDebugLog(it)
                },
                role = Role.Checkbox
            )
    ) {
        val (checkbox, content, informationButton) = createRefs()

        Checkbox(
            checked = debugLog,
            onCheckedChange = null,
            modifier = Modifier
                .constrainAs(checkbox) {
                    start.linkTo(parent.start)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
        )
        Text(
            text = stringResource(R.string.debug_log),
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
            MoreInformationButton {
                onInformationRequested(R.string.debug_log_explanation)
            }
        }
    }
    OutlinedButton(
        onClick = { debugLogService.clear() }
    ) {
        Text(stringResource(R.string.clear_debug_log))
    }

    OutlinedButton(
        onClick = { exportDebugLog(context, debugLogService, debugFileSaveLauncher) }
    ) {
        Text(stringResource(R.string.export_debug_log))
    }
}

@ExperimentalMaterial3Api
@Composable
fun AppSettingsScreen(innerPadding: PaddingValues) {
    val context = LocalContext.current

    val appSettingsStore = context.appSettingsStore
    val appSettings by appSettingsStore.data.collectAsState(
        ScanBridgeSettings.getDefaultInstance()
    )
    var information: Int? by remember {
        mutableStateOf(null)
    }
    val setInformationRequested = { it: Int -> information = it }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VersionComposable()

        val isFdroidVariant = BuildConfig.FLAVOR == "fdroid"
        val padding = if (isFdroidVariant) 16.dp else 10.dp
        FlowRow(
            modifier = Modifier.padding(vertical = padding),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            if (isFdroidVariant) {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, "https://github.com/sponsors/Chrisimx".toUri())
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6F61),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        modifier = Modifier.padding(end = 8.dp),
                        painter = painterResource(R.drawable.favorite_24px),
                        contentDescription = stringResource(R.string.donate)
                    )
                    Text(stringResource(R.string.donate))
                }
            }
            OutlinedIconButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, "https://github.com/Chrisimx/ScanBridge".toUri())
                context.startActivity(intent)
            }) {
                Icon(painterResource(R.drawable.github_mark), contentDescription = stringResource(R.string.source_code))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(14.dp))

        FlowRow {
            TitledCard(
                title = stringResource(R.string.settings)
            ) {
                DisableCertChecksSetting(
                    setInformationRequested,
                    appSettings.disableCertChecks,
                    { coroutineScope.launch { appSettingsStore.setDisableCertCheck(it)}  }
                )

                CheckboxSetting(
                    stringResource(R.string.remember_scan_settings),
                    R.string.remember_scan_settings_desc,
                    appSettings.rememberScanSettingsOrNull?.value ?: true,
                    { coroutineScope.launch { appSettingsStore.setRememberScanSettings(it) } }
                ) {
                    information = it
                }

                // Timeout setting
                UIntSetting(
                    {
                        appSettingsStore.data.firstOrNull()?.scanningResponseTimeout?.value?.toUInt() ?: 25u
                    },
                    25u,
                    stringResource(R.string.timeout),
                    R.string.timeout_info,
                    setInformationRequested,
                    { coroutineScope.launch { appSettingsStore.setTimeoutSetting(it) } },
                )

                // PDF chunk size setting
                UIntSetting(
                    {
                        appSettingsStore.data.firstOrNull()?.chunkSizePdfExport?.value?.toUInt() ?: 50u
                    },
                    50u,
                    stringResource(R.string.pdf_export_max_pages_per_pdf),
                    R.string.pdf_export_setting_info,
                    setInformationRequested,
                    { coroutineScope.launch { appSettingsStore.setChunkSize(it) } },
                    min = 1u,
                    max = UInt.MAX_VALUE
                )
            }

            TitledCard(
                title = stringResource(R.string.advanced)
            ) {
                DebugOptions(
                    appSettings.writeDebug,
                    setInformationRequested,
                    { coroutineScope.launch { appSettingsStore.setWriteDebugLog(it) }}
                )
            }
        }

        val currentInfo = information
        if (currentInfo != null) {
            SimpleTextDialog(
                stringResource(currentInfo),
                { information = null }
            )
        }
    }
}
