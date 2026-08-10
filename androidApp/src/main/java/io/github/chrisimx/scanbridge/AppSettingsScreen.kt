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

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import io.github.chrisimx.scanbridge.appsettings.AppSettingsViewModel
import io.github.chrisimx.scanbridge.buildinfo.ScanBridgeEdition
import io.github.chrisimx.scanbridge.datastore.*
import io.github.chrisimx.scanbridge.services.DebugLogService
import io.github.chrisimx.scanbridge.uicomponents.TitledCard
import io.github.chrisimx.scanbridge.uicomponents.dialog.SimpleTextDialog
import io.github.chrisimx.scanbridge.uicomponents.settings.CheckboxSetting
import io.github.chrisimx.scanbridge.uicomponents.settings.MoreInformationButton
import io.github.chrisimx.scanbridge.uicomponents.settings.UIntSetting
import io.github.chrisimx.scanbridge.uicomponents.settings.VersionComposable
import java.io.File
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.debug_log_explanation
import scanbridge.composeui.generated.resources.disable_cert_checks_desc
import scanbridge.composeui.generated.resources.initial_scan_settings
import scanbridge.composeui.generated.resources.pdf_export_setting_info
import scanbridge.composeui.generated.resources.preferred_initial_scan_settings_setting_desc
import scanbridge.composeui.generated.resources.remember_scan_settings_desc
import scanbridge.composeui.generated.resources.timeout_info

@Composable
fun DisableCertChecksSetting(onInformationRequested: (StringResource) -> Unit, checked: Boolean, setChecked: (Boolean) -> Unit) {
    CheckboxSetting(
        stringResource(R.string.disable_cert_checks),
        Res.string.disable_cert_checks_desc,
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
fun DebugOptions(debugLog: Boolean, onInformationRequested: (StringResource) -> Unit, setWriteDebugLog: (Boolean) -> Unit) {
    val context = LocalContext.current
    val debugLogService: DebugLogService = koinInject()

    val debugFileSaveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
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
        Box(
            modifier = Modifier
                .constrainAs(informationButton) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }
        ) {
            MoreInformationButton {
                onInformationRequested(Res.string.debug_log_explanation)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialScanSettingsAppSetting(
    preferredInitialScanSettingsPanelVisible: Boolean,
    setMenuVis: (Boolean) -> Unit,
    scanSettingsUIStateHolder: ScanSettingsComposableStateHolder,
    setHelpText: (StringResource) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(vertical = 10.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(horizontal = 10.dp),
            onClick = {
                setMenuVis(true)
            }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit"
                )
                Text(
                    text = stringResource(Res.string.initial_scan_settings)
                )
            }
        }

        MoreInformationButton {
            setHelpText(Res.string.preferred_initial_scan_settings_setting_desc)
        }
    }

    if (preferredInitialScanSettingsPanelVisible) {
        val screenHeight = LocalWindowInfo.current.containerDpSize.height
        ModalBottomSheet({ setMenuVis(false) }) {
            ScanSettingsUI(
                Modifier.heightIn(max = screenHeight * 0.8f),
                scanSettingsUIStateHolder
            )
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun AppSettingsScreen(innerPadding: PaddingValues) {
    val vm = koinInject<AppSettingsViewModel>()

    var information: StringResource? by remember {
        mutableStateOf(null)
    }
    val setInformationRequested = { it: StringResource -> information = it }

    val scrollState = rememberScrollState()

    val disableCertChecks by vm.disableCertChecks.collectAsState()
    val rememberScanSettings by vm.rememberScanSettings.collectAsState()
    val writeDebugLogs by vm.writeDebugLogs.collectAsState()

    val preferredInitialScanSettingsPanelVisible by vm.isPreferredInitialScanSettingsMenuVisible.collectAsState()

    val defaultPdfExportChunkSize = vm.getDefaultPdfExportChunkSize()
    val defaultScanningResponseTimeout = vm.getDefaultScanningResponseTimeout()

    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VersionComposable(
            vm.versionName,
            vm.versionCode,
            vm.gitCommitHash,
            vm.edition,
            vm.isDebugBuild
        )

        val isFdroidVariant = vm.edition == ScanBridgeEdition.FDROID
        val padding = if (isFdroidVariant) 16.dp else 10.dp
        FlowRow(
            modifier = Modifier.padding(vertical = padding),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            if (isFdroidVariant) {
                Button(
                    onClick = {
                        uriHandler.openUri("https://github.com/sponsors/Chrisimx")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6F61),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        modifier = Modifier.padding(end = 8.dp),
                        painter = painterResource(R.drawable.favorite_24px),
                        contentDescription = null
                    )
                    Text(stringResource(R.string.donate))
                }
            }
            OutlinedIconButton(onClick = {
                uriHandler.openUri("https://github.com/Chrisimx/ScanBridge")
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
                    disableCertChecks,
                    vm::setDisableCertValidation
                )

                CheckboxSetting(
                    stringResource(R.string.remember_scan_settings),
                    Res.string.remember_scan_settings_desc,
                    rememberScanSettings,
                    vm::setRememberScanSettings
                ) {
                    information = it
                }

                // Timeout setting
                UIntSetting(
                    { vm.getScanningResponseTimeout() },
                    defaultScanningResponseTimeout,
                    stringResource(R.string.timeout),
                    Res.string.timeout_info,
                    setInformationRequested,
                    vm::setScanningResponseTimeout
                )

                // PDF chunk size setting
                UIntSetting(
                    { vm.getPdfExportChunkSize() },
                    defaultPdfExportChunkSize,
                    stringResource(R.string.pdf_export_max_pages_per_pdf),
                    Res.string.pdf_export_setting_info,
                    setInformationRequested,
                    vm::setPdfExportChunkSize,
                    min = 1u,
                    max = UInt.MAX_VALUE
                )

                // Preferred initial scan settings
                InitialScanSettingsAppSetting(
                    preferredInitialScanSettingsPanelVisible,
                    vm::setPreferredInitialScanSettingsMenuVisibility,
                    vm.prefInitialScanSettingsUIStateHolder,
                    setInformationRequested
                )
            }

            TitledCard(
                title = stringResource(R.string.advanced)
            ) {
                DebugOptions(
                    writeDebugLogs,
                    setInformationRequested,
                    vm::setWriteDebugLogs
                )
            }
        }

        val currentInfo = information
        if (currentInfo != null) {
            SimpleTextDialog(
                org.jetbrains.compose.resources.stringResource(currentInfo),
                { information = null }
            )
        }
    }
}
