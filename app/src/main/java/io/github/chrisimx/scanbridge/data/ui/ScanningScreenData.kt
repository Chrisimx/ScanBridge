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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities

data class ScanningScreenData(
    val esclClient: ESCLRequestClient,
    val confirmDialogShown: MutableState<Boolean> = mutableStateOf(false),
    val errorString: MutableState<String?> = mutableStateOf(null),
    val scanSettingsVM: MutableState<ScanSettingsComposableViewModel?> = mutableStateOf(null),
    val capabilities: MutableState<ScannerCapabilities?> = mutableStateOf(null),
    val scanSettingsMenuOpen: MutableState<Boolean> = mutableStateOf(false),
    val scanJobRunning: MutableState<Boolean> = mutableStateOf(false),
    val stateExportRunning: MutableState<Boolean> = mutableStateOf(false),
    val stateCurrentScans: SnapshotStateList<Pair<String, ScanSettings>> = mutableStateListOf()
) {
    fun toImmutable() = ImmutableScanningScreenData(
        esclClient,
        confirmDialogShown,
        errorString,
        scanSettingsVM,
        capabilities,
        scanSettingsMenuOpen,
        scanJobRunning,
        stateExportRunning,
        stateCurrentScans
    )
}

data class ImmutableScanningScreenData(
    val esclClient: ESCLRequestClient,
    private val confirmDialogShownState: State<Boolean>,
    private val errorStringState: State<String?>,
    private val scanSettingsVMState: State<ScanSettingsComposableViewModel?>,
    private val capabilitiesState: State<ScannerCapabilities?>,
    private val scanSettingsMenuOpenState: State<Boolean>,
    private val scanJobRunningState: State<Boolean>,
    private val exportRunningState: State<Boolean>,
    val currentScansState: SnapshotStateList<Pair<String, ScanSettings>>,
) {
    val confirmDialogShown by confirmDialogShownState
    val scanSettingsVM by scanSettingsVMState
    val scanSettingsMenuOpen by scanSettingsMenuOpenState
    val scanJobRunning by scanJobRunningState
    val exportRunning by exportRunningState
    val capabilities by capabilitiesState
    val errorString by errorStringState
}