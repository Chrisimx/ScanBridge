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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.application
import getTrustAllTM
import io.github.chrisimx.esclkt.ESCLRequestClient
import io.github.chrisimx.esclkt.Inches
import io.github.chrisimx.esclkt.LengthUnit
import io.github.chrisimx.esclkt.Millimeters
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.esclkt.ThreeHundredthsOfInch
import io.github.chrisimx.esclkt.millimeters
import io.github.chrisimx.scanbridge.data.model.Session
import io.github.chrisimx.scanbridge.logs.DebugInterceptor
import io.github.chrisimx.scanbridge.util.DefaultScanSettingsStore
import io.github.chrisimx.scanbridge.util.calculateDefaultESCLScanSettingsState
import io.github.chrisimx.scanbridge.util.getInputSourceCaps
import io.github.chrisimx.scanbridge.util.getInputSourceOptions
import java.io.File
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import timber.log.Timber

class ScanningScreenViewModel(
    application: Application,
    address: HttpUrl,
    timeout: UInt,
    withDebugInterceptor: Boolean,
    certificateValidationDisabled: Boolean,
    sessionID: String
) : AndroidViewModel(application) {
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
            ),
            sessionID
        )
    val scanningScreenData: ImmutableScanningScreenData
        get() = _scanningScreenData.toImmutable()

    val json = Json {
        serializersModule = SerializersModule {
            polymorphic(LengthUnit::class) {
                subclass(Inches::class)
                subclass(Millimeters::class)
                subclass(ThreeHundredthsOfInch::class)
            }
        }
        classDiscriminator = "type"
        prettyPrint = false
    }

    fun addTempFile(file: File) {
        _scanningScreenData.createdTempFiles.add(file)
        saveSessionFile()
    }

    fun setShowExportOptionsPopup(show: Boolean) {
        _scanningScreenData.showExportOptions.value = show
    }

    fun setExportPopupPosition(x: Int, y: Int) {
        _scanningScreenData.exportOptionsPopupPosition.value = Pair(x, y)
    }

    fun removeTempFile(index: Int) {
        _scanningScreenData.createdTempFiles.removeAt(index)
        saveSessionFile()
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
        val storedSession = loadSessionFile()

        if (storedSession != null) {
            scanningScreenData.currentScansState.addAll(storedSession.scannedPages)
            _scanningScreenData.scanSettingsVM.value = ScanSettingsComposableViewModel(
                ScanSettingsComposableData(storedSession.scanSettings?.toMutable() ?: caps.calculateDefaultESCLScanSettingsState(), caps),
                onSettingsChanged = { saveScanSettings() }
            )
        } else {
            // Try to load saved scan settings first, fallback to defaults if none exist
            val savedSettings = DefaultScanSettingsStore.load(application.applicationContext)
            val initialSettings = if (savedSettings != null) {
                try {
                    // Merge saved settings with current capabilities to ensure compatibility
                    val mutableSettings = savedSettings.toMutable()
                    
                    // Validate that the saved input source is still supported
                    val supportedInputSources = caps.getInputSourceOptions()
                    if (mutableSettings.inputSource != null && 
                        !supportedInputSources.contains(mutableSettings.inputSource)) {
                        Timber.w("Saved input source ${mutableSettings.inputSource} not supported by current scanner, falling back to default")
                        mutableSettings.inputSource = supportedInputSources.firstOrNull() ?: InputSource.Platen
                    }
                    
                    // Validate duplex setting - only allow if ADF supports duplex
                    if (mutableSettings.duplex == true && 
                        (mutableSettings.inputSource != InputSource.Feeder || caps.adf?.duplexCaps == null)) {
                        Timber.w("Duplex not supported with current input source, disabling duplex")
                        mutableSettings.duplex = false
                    }

                    val selectedInputSourceCaps = caps.getInputSourceCaps(mutableSettings.inputSource ?: InputSource.Platen, mutableSettings.duplex ?: false)

                    if (!selectedInputSourceCaps.supportedIntents.contains(mutableSettings.intent)) {
                        mutableSettings.intent = selectedInputSourceCaps.supportedIntents.first()
                    }

                    if (mutableSettings.scanRegions != null) {
                        val storedWidthThreeHOfInch = mutableSettings.scanRegions!!.width.toDoubleOrNull()
                        val storedHeightThreeHOfInch = mutableSettings.scanRegions!!.width.toDoubleOrNull()

                        val maxWidth = selectedInputSourceCaps.maxWidth.toMillimeters().value
                        val minWidth = selectedInputSourceCaps.minWidth.toMillimeters().value

                        val maxHeight = selectedInputSourceCaps.maxHeight.toMillimeters().value
                        val minHeight = selectedInputSourceCaps.minHeight.toMillimeters().value

                        if (storedWidthThreeHOfInch != null && (storedWidthThreeHOfInch > maxWidth || storedWidthThreeHOfInch < minWidth) ) {
                            mutableSettings.scanRegions!!.width = "max"
                        }
                        if (storedHeightThreeHOfInch != null && (storedHeightThreeHOfInch > maxHeight || storedHeightThreeHOfInch < minHeight) ) {
                            mutableSettings.scanRegions!!.height = "max"
                        }
                    }

                    mutableSettings
                } catch (e: Exception) {
                    Timber.e(e, "Error applying saved settings, using defaults")
                    caps.calculateDefaultESCLScanSettingsState()
                }
            } else {
                caps.calculateDefaultESCLScanSettingsState()
            }
            
            _scanningScreenData.scanSettingsVM.value = ScanSettingsComposableViewModel(
                ScanSettingsComposableData(
                    initialSettings,
                    caps
                ),
                onSettingsChanged = { saveScanSettings() }
            )
            val sessionFile = application.applicationInfo.dataDir + "/files/" + scanningScreenData.sessionID + ".session"
            addTempFile(File(sessionFile))
            saveSessionFile()
        }
    }

    fun addScan(path: String, settings: ScanSettings) {
        _scanningScreenData.stateCurrentScans.add(Pair(path, settings))
        saveSessionFile()
    }
    fun addScanAtIndex(path: String, settings: ScanSettings, index: Int) {
        _scanningScreenData.stateCurrentScans.add(index, Pair(path, settings))
        saveSessionFile()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun saveSessionFile(): String {
        val path = application.applicationInfo.dataDir + "/files/" + scanningScreenData.sessionID + ".session"
        val file = File(path)

        val currentSessionState = Session(
            scanningScreenData.sessionID,
            scanningScreenData.currentScansState.toList(),
            scanningScreenData.scanSettingsVM?.getMutableScanSettingsComposableData()?.scanSettingsState?.toStateless(),
            scanningScreenData.createdTempFiles.map { it.absolutePath }
        )

        val fos = file.outputStream()

        json.encodeToStream(currentSessionState, fos)
        fos.close()
        return path
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun loadSessionFile(): Session? {
        val path = application.applicationInfo.dataDir + "/files/" + scanningScreenData.sessionID + ".session"
        val file = File(path)

        if (!file.exists()) {
            Timber.d("Could not find session file at $path")
            return null
        }

        val inputStream = file.inputStream()

        val storedSession = json.decodeFromStream<Session>(inputStream)

        inputStream.close()

        return storedSession
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
        saveSessionFile()
    }

    fun removeScanAtIndex(index: Int) {
        if (index < 0 || index >= _scanningScreenData.stateCurrentScans.size) {
            return
        }
        _scanningScreenData.stateCurrentScans.removeAt(index)
        saveSessionFile()
    }

    fun saveScanSettings() {
        scanningScreenData.scanSettingsVM?.getMutableScanSettingsComposableData()?.scanSettingsState?.toStateless()?.let { settings ->
            DefaultScanSettingsStore.save(application.applicationContext, settings)
            Timber.d("Scan settings saved to persistent storage")
        }
    }

    fun clearSavedScanSettings() {
        DefaultScanSettingsStore.clear(application.applicationContext)
        Timber.d("Saved scan settings cleared")
    }
}
