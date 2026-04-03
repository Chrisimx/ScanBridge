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

package io.github.chrisimx.scanbridge.stores

import android.content.Context
import com.google.protobuf.StringValue
import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.scanbridge.data.ui.ScanSettingsStateData
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.datastore.updateSettings
import io.github.chrisimx.scanbridge.proto.lastUsedScanSettingsOrNull
import io.github.chrisimx.scanbridge.proto.lastUsedScanSettingsUiStateOrNull
import io.github.chrisimx.scanbridge.proto.rememberScanSettingsOrNull
import io.github.chrisimx.scanbridge.util.ScanSettingsJson
import kotlinx.coroutines.flow.first
import timber.log.Timber

object DefaultScanSettingsStore {
    private suspend fun isRememberSettingsEnabled(context: Context): Boolean {
        val appPreferences = context.appSettingsStore.data.first()
        return appPreferences.rememberScanSettingsOrNull?.value ?: true
    }

    suspend fun save(context: Context, scanSettings: ScanSettings, uiStateData: ScanSettingsStateData?) {
        if (!isRememberSettingsEnabled(context)) {
            Timber.d("Scan settings persistence is disabled, skipping save")
            return
        }

        val serializedSettings = ScanSettingsJson.json.encodeToString(scanSettings)
        val serializedUiState = runCatching {
            uiStateData.let {
                ScanSettingsJson.json.encodeToString(uiStateData)
            }
        }.getOrNull()
        Timber.d("Saving default scan settings: $serializedSettings, $serializedUiState")
        context.appSettingsStore.updateSettings {
            lastUsedScanSettings = StringValue.of(serializedSettings)
            if (serializedUiState != null) {
                lastUsedScanSettingsUiState = StringValue.of(serializedUiState)
            } else {
                clearLastUsedScanSettingsUiState()
            }
        }
    }

    suspend fun load(context: Context): Pair<ScanSettings?, ScanSettingsStateData?> {
        if (!isRememberSettingsEnabled(context)) {
            Timber.d("Scan settings persistence is disabled, returning null")
            return null to null
        }

        val appSettings = context.appSettingsStore.data.first()
        val lastUsedScanSettings = appSettings.lastUsedScanSettingsOrNull?.value
        val lastUsedScanSettingsUiState = appSettings.lastUsedScanSettingsUiStateOrNull?.value

        if (lastUsedScanSettings == null) {
            Timber.d("No saved scan settings found")
            return null to null
        }

        try {
            val json = ScanSettingsJson.json
            val lastUsedScanSettingsDecoded = json.decodeFromString<ScanSettings>(lastUsedScanSettings)
            val lastUsedScanSettingsUIStateDecoded = lastUsedScanSettingsUiState?.let {
                json.decodeFromString<ScanSettingsStateData>(it)
            }
            Timber.d("Loaded default scan settings $lastUsedScanSettings, $lastUsedScanSettingsUIStateDecoded")
            return lastUsedScanSettingsDecoded to lastUsedScanSettingsUIStateDecoded
        } catch (_: Exception) {
            Timber.e("JSON in last_used_scan_settings is invalid. Not used!")
            return null to null
        }
    }

    suspend fun clear(context: Context) {
        val appSettingsStore = context.appSettingsStore

        appSettingsStore.updateSettings {
            clearLastUsedScanSettings()
        }

        Timber.d("Scan settings cleared from persistent storage")
    }
}
