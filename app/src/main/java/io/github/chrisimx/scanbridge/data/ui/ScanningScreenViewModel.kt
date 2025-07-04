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
import getTrustAllTM
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.scanbridge.logs.DebugInterceptor
import io.github.chrisimx.scanbridge.util.calculateDefaultESCLScanSettingsState
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class ScanningScreenViewModel(address: HttpUrl, timeout: UInt, withDebugInterceptor: Boolean, certificateValidationDisabled: Boolean) :
    ViewModel() {
    private val _scanningScreenData =
        ScanningScreenData(
            ESCLRequestClient(
                address,
                OkHttpClient.Builder().let {
                    if (withDebugInterceptor) {
                        it.addInterceptor(DebugInterceptor())
                    }
                    it.connectTimeout(timeout.toLong(), java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(timeout.toLong(), java.util.concurrent.TimeUnit.SECONDS)
                    if (certificateValidationDisabled) {
                        val (socketFactory, trustManager) = getTrustAllTM()
                        it.sslSocketFactory(socketFactory, trustManager)
                        it.hostnameVerifier { _, _ -> true }
                    }

                    it
                }.build()
            )
        )
    val scanningScreenData: ImmutableScanningScreenData
        get() = _scanningScreenData.toImmutable()

    fun addTempFile(file: File) {
        _scanningScreenData.createdTempFiles.add(file)
    }

    fun setShowExportOptionsPopup(show: Boolean) {
        _scanningScreenData.showExportOptions.value = show
    }

    fun setExportPopupPosition(x: Int, y: Int) {
        _scanningScreenData.exportOptionsPopupPosition.value = Pair(x, y)
    }

    fun removeTempFile(index: Int) {
        _scanningScreenData.createdTempFiles.removeAt(index)
    }

    fun setLoadingText(stringRes: Int?) {
        _scanningScreenData.stateProgressStringRes.value = stringRes
    }

    fun scrollToPage(pageNr: Int, scope: CoroutineScope) {
        scope.launch {
            _scanningScreenData.pagerState.animateScrollToPage(
                scanningScreenData.currentScansState.size
            )
        }
    }

    fun setScanSettingsMenuOpen(value: Boolean) {
        _scanningScreenData.scanSettingsMenuOpen.value = value
    }

    fun setConfirmDialogShown(value: Boolean) {
        _scanningScreenData.confirmDialogShown.value = value
    }

    fun setDeletePageDialogShown(value: Boolean) {
        _scanningScreenData.confirmPageDeleteDialogShown.value = value
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
                caps
            )
        )
    }

    fun addScan(path: String, settings: ScanSettings) {
        _scanningScreenData.stateCurrentScans.add(Pair(path, settings))
    }
    fun addScanAtIndex(path: String, settings: ScanSettings, index: Int) {
        _scanningScreenData.stateCurrentScans.add(index, Pair(path, settings))
    }

    fun swapTwoPages(index1: Int, index2: Int) {
        if (index1 < 0 ||
            index1 >= _scanningScreenData.stateCurrentScans.size ||
            index2 < 0 ||
            index2 >= _scanningScreenData.stateCurrentScans.size
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
}
