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

import androidx.lifecycle.ViewModel
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.scanbridge.util.calculateDefaultESCLScanSettingsState
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class ScanningScreenViewModel(
    address: HttpUrl,
) : ViewModel() {
    private val _scanningScreenData =
        ScanningScreenData(
            ESCLRequestClient(address, OkHttpClient.Builder().build())
        )
    val scanningScreenData: ImmutableScanningScreenData
        get() = _scanningScreenData.toImmutable()

    fun setScanSettingsMenuOpen(value: Boolean) {
        _scanningScreenData.scanSettingsMenuOpen.value = value
    }

    fun setConfirmDialogShown(value: Boolean) {
        _scanningScreenData.confirmDialogShown.value = value
    }

    fun setScanJobRunning(value: Boolean) {
        _scanningScreenData.scanJobRunning.value = value
    }

    fun setError(value: String?) {
        _scanningScreenData.errorString.value = value
    }

    fun setScannerCapabilities(caps: ScannerCapabilities) {
        _scanningScreenData.capabilities.value = caps
        _scanningScreenData.scanSettingsVM.value = ScanSettingsComposableViewModel(
            ScanSettingsComposableData(
                caps.calculateDefaultESCLScanSettingsState(),
                caps,
            )
        )
    }

    fun addScan(path: String, settings: ScanSettings) {
        _scanningScreenData.stateCurrentScans.add(Pair(path, settings))
    }

    fun swapTwoPages(index1: Int, index2: Int) {
        if (index1 < 0 || index1 >= _scanningScreenData.stateCurrentScans.size
            || index2 < 0 || index2 >= _scanningScreenData.stateCurrentScans.size
        ) {
            return
        }
        val tmp = _scanningScreenData.stateCurrentScans[index1]
        _scanningScreenData.stateCurrentScans[index1] =
            _scanningScreenData.stateCurrentScans[index2]
        _scanningScreenData.stateCurrentScans[index2] = tmp
    }

    fun removeScanAtIndex(index: Int) {
        if (index < 0 || index >= _scanningScreenData.stateCurrentScans.size) {
            return
        }
        _scanningScreenData.stateCurrentScans.removeAt(index)
    }

    fun setExportRunning(running: Boolean) {
        _scanningScreenData.stateExportRunning.value = running
    }
}