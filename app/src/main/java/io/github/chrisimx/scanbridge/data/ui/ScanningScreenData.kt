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

package io.github.chrisimx.scanbridge.data.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities
import java.io.File
import kotlinx.serialization.Serializable

enum class ScanRelativeRotation {
    Rotated,
    Original
}

data class ErrorDescription(val pretext: Int?, val icon: Int?, val text: String?)

fun ScanRelativeRotation.toggleRotation() = when (this) {
    ScanRelativeRotation.Rotated -> ScanRelativeRotation.Original
    ScanRelativeRotation.Original -> ScanRelativeRotation.Rotated
}

@Serializable
data class ScanMetadata(
    val filePath: String,
    val originalScanSettings: ScanSettings,
    val rotation: ScanRelativeRotation = ScanRelativeRotation.Original
)

data class ScanningScreenData(
    val esclClient: ESCLRequestClient,
    val sessionID: String,
    val confirmDialogShown: MutableState<Boolean> = mutableStateOf(false),
    val confirmPageDeleteDialogShown: MutableState<Boolean> = mutableStateOf(false),
    val error: MutableState<ErrorDescription?> = mutableStateOf(null),
    val scanSettingsVM: MutableState<ScanSettingsComposableViewModel?> = mutableStateOf(null),
    val capabilities: MutableState<ScannerCapabilities?> = mutableStateOf(null),
    val scanSettingsMenuOpen: MutableState<Boolean> = mutableStateOf(false),
    val scanJobRunning: MutableState<Boolean> = mutableStateOf(false),
    val scanJobCancelling: MutableState<Boolean> = mutableStateOf(false),
    val showExportOptions: MutableState<Boolean> = mutableStateOf(false),
    val showSaveOptions: MutableState<Boolean> = mutableStateOf(false),
    val exportOptionsPopupPosition: MutableState<Triple<Int, Int, Int>?> = mutableStateOf(null),
    val savePopupPosition: MutableState<Triple<Int, Int, Int>?> = mutableStateOf(null),
    val stateProgressStringRes: MutableState<Int?> = mutableStateOf(null),
    val stateCurrentScans: SnapshotStateList<ScanMetadata> = mutableStateListOf(),
    val createdTempFiles: MutableList<File> = mutableListOf(),
    val pagerState: PagerState = PagerState {
        (stateCurrentScans.size + if (scanJobRunning.value) 1 else 0).coerceAtLeast(1)
    },
    val sourceFileToSave: MutableState<File?> = mutableStateOf(null),
    val isRotating: MutableState<Boolean> = mutableStateOf(false)
) {
    fun toImmutable() = ImmutableScanningScreenData(
        esclClient,
        sessionID,
        confirmDialogShown,
        confirmPageDeleteDialogShown,
        error,
        scanSettingsVM,
        capabilities,
        scanSettingsMenuOpen,
        showExportOptions,
        showSaveOptions,
        exportOptionsPopupPosition,
        savePopupPosition,
        scanJobRunning,
        scanJobCancelling,
        stateProgressStringRes,
        sourceFileToSave,
        isRotating,
        createdTempFiles,
        pagerState,
        stateCurrentScans
    )
}

data class ImmutableScanningScreenData(
    val esclClient: ESCLRequestClient,
    val sessionID: String,
    private val confirmDialogShownState: State<Boolean>,
    private val confirmPageDeleteDialogShownState: State<Boolean>,
    private val errorState: State<ErrorDescription?>,
    private val scanSettingsVMState: State<ScanSettingsComposableViewModel?>,
    private val capabilitiesState: State<ScannerCapabilities?>,
    private val scanSettingsMenuOpenState: State<Boolean>,
    private val showExportOptionsState: State<Boolean>,
    private val showSaveOptionsState: State<Boolean>,
    private val exportOptionsPopupPositionState: State<Triple<Int, Int, Int>?>,
    private val saveOptionsPopupPositionState: State<Triple<Int, Int, Int>?>,
    private val scanJobRunningState: State<Boolean>,
    private val scanJobCancellingState: State<Boolean>,
    private val progressStringResState: State<Int?>,
    private val sourceFileToSaveState: State<File?>,
    private val isRotatingState: State<Boolean>,
    val createdTempFiles: List<File>,
    val pagerState: PagerState,
    val currentScansState: SnapshotStateList<ScanMetadata>
) {
    val confirmDialogShown by confirmDialogShownState
    val confirmPageDeleteDialogShown by confirmPageDeleteDialogShownState
    val scanSettingsVM by scanSettingsVMState
    val scanSettingsMenuOpen by scanSettingsMenuOpenState
    val scanJobRunning by scanJobRunningState
    val scanJobCancelling by scanJobCancellingState
    val progressStringResource by progressStringResState
    val capabilities by capabilitiesState
    val error by errorState
    val showExportOptions by showExportOptionsState
    val showSaveOptions by showSaveOptionsState
    val exportOptionsPopupPosition by exportOptionsPopupPositionState
    val saveOptionsPopupPosition by saveOptionsPopupPositionState
    val sourceFileToSave by sourceFileToSaveState
    val isRotating by isRotatingState
}
