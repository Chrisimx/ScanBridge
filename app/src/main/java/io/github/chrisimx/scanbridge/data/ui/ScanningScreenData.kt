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
import kotlin.uuid.Uuid
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

data class ScanningScreenData(
    val esclClient: ESCLRequestClient,
    val sessionID: Uuid,
    val confirmDialogShown: MutableState<Boolean> = mutableStateOf(false),
    val confirmPageDeleteDialogShown: MutableState<Boolean> = mutableStateOf(false),
    val error: MutableState<ErrorDescription?> = mutableStateOf(null),
    val scanSettingsVM: MutableState<ScanSettingsComposableStateHolder?> = mutableStateOf(null),
    val capabilities: MutableState<ScannerCapabilities?> = mutableStateOf(null),
    val scanSettingsMenuOpen: MutableState<Boolean> = mutableStateOf(false),
    val showExportOptions: MutableState<Boolean> = mutableStateOf(false),
    val showSaveOptions: MutableState<Boolean> = mutableStateOf(false),
    val exportOptionsPopupPosition: MutableState<Triple<Int, Int, Int>?> = mutableStateOf(null),
    val savePopupPosition: MutableState<Triple<Int, Int, Int>?> = mutableStateOf(null),
    val stateProgressStringRes: MutableState<Int?> = mutableStateOf(null),
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
        stateProgressStringRes,
        sourceFileToSave,
        isRotating,
    )
}

data class ImmutableScanningScreenData(
    val esclClient: ESCLRequestClient,
    val sessionID: Uuid,
    private val confirmDialogShownState: State<Boolean>,
    private val confirmPageDeleteDialogShownState: State<Boolean>,
    private val errorState: State<ErrorDescription?>,
    private val scanSettingsVMState: State<ScanSettingsComposableStateHolder?>,
    private val capabilitiesState: State<ScannerCapabilities?>,
    private val scanSettingsMenuOpenState: State<Boolean>,
    private val showExportOptionsState: State<Boolean>,
    private val showSaveOptionsState: State<Boolean>,
    private val exportOptionsPopupPositionState: State<Triple<Int, Int, Int>?>,
    private val saveOptionsPopupPositionState: State<Triple<Int, Int, Int>?>,
    private val progressStringResState: State<Int?>,
    private val sourceFileToSaveState: State<File?>,
    private val isRotatingState: State<Boolean>,
) {
    val confirmDialogShown by confirmDialogShownState
    val confirmPageDeleteDialogShown by confirmPageDeleteDialogShownState
    val scanSettingsVM by scanSettingsVMState
    val scanSettingsMenuOpen by scanSettingsMenuOpenState
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
