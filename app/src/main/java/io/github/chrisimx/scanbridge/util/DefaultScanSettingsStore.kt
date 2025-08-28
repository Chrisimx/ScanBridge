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

package io.github.chrisimx.scanbridge.util

import android.content.Context
import androidx.core.content.edit
import io.github.chrisimx.scanbridge.data.model.StatelessImmutableESCLScanSettingsState
import kotlinx.serialization.json.Json
import timber.log.Timber

object DefaultScanSettingsStore {
    private const val APP_PREF_NAME = "scanbridge"

    private fun isRememberSettingsEnabled(context: Context): Boolean {
        val appPreferences = context.getSharedPreferences(APP_PREF_NAME, Context.MODE_PRIVATE)
        return appPreferences.getBoolean("remember_scan_settings", true)
    }

    fun save(context: Context, scanSettings: StatelessImmutableESCLScanSettingsState) {
        if (!isRememberSettingsEnabled(context)) {
            Timber.d("Scan settings persistence is disabled, skipping save")
            return
        }

        val sharedPreferences = context.getSharedPreferences(APP_PREF_NAME, Context.MODE_PRIVATE)
        val serializedSettings = Json.encodeToString(scanSettings)
        sharedPreferences.edit {
            putString("last_used_scan_settings", serializedSettings)
        }
    }

    fun load(context: Context): StatelessImmutableESCLScanSettingsState? {
        if (!isRememberSettingsEnabled(context)) {
            Timber.d("Scan settings persistence is disabled, returning null")
            return null
        }

        val sharedPreferences = context.getSharedPreferences(APP_PREF_NAME, Context.MODE_PRIVATE)
        val lastUsedScanSettings = sharedPreferences.getString("last_used_scan_settings", null)

        if (lastUsedScanSettings == null) {
            Timber.d("No saved scan settings found")
            return null
        }

        return Json.decodeFromString<StatelessImmutableESCLScanSettingsState>(lastUsedScanSettings)
    }

    fun clear(context: Context) {
        val sharedPreferences = context.getSharedPreferences(APP_PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit { remove("last_used_scan_settings") }
        Timber.d("Scan settings cleared from persistent storage")
    }
}
