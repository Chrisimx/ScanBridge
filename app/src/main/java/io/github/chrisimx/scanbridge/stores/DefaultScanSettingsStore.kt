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
import io.github.chrisimx.scanbridge.datastore.appSettingsStore
import io.github.chrisimx.scanbridge.datastore.updateSettings
import io.github.chrisimx.scanbridge.proto.lastUsedScanSettingsOrNull
import io.github.chrisimx.scanbridge.proto.rememberScanSettingsOrNull
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import timber.log.Timber

object DefaultScanSettingsStore {
    private const val APP_PREF_NAME = "scanbridge"

    private suspend fun isRememberSettingsEnabled(context: Context): Boolean {
        val appPreferences = context.appSettingsStore.data.first()
        return appPreferences.rememberScanSettingsOrNull?.value ?: true
    }

    suspend fun save(context: Context, scanSettings: ScanSettings) {
        if (!isRememberSettingsEnabled(context)) {
            Timber.d("Scan settings persistence is disabled, skipping save")
            return
        }

        val serializedSettings = Json.encodeToString(scanSettings)
        context.appSettingsStore.updateSettings {
            lastUsedScanSettings = StringValue.of(serializedSettings)
        }
    }

    suspend fun load(context: Context): ScanSettings? {
        if (!isRememberSettingsEnabled(context)) {
            Timber.d("Scan settings persistence is disabled, returning null")
            return null
        }

        val appSettings = context.appSettingsStore.data
        val lastUsedScanSettings = appSettings.first().lastUsedScanSettingsOrNull?.value

        if (lastUsedScanSettings == null) {
            Timber.d("No saved scan settings found")
            return null
        }

        try {
            val json = Json { ignoreUnknownKeys = true }
            return json.decodeFromString<ScanSettings>(lastUsedScanSettings)
        } catch (_: Exception) {
            Timber.e("JSON in last_used_scan_settings is invalid. Not used!")
            return null
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
