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
import androidx.core.content.edit
import io.github.chrisimx.scanbridge.data.model.CustomScanner
import io.ktor.http.Url
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import timber.log.Timber

object CustomScannerStore {
    private const val PREF_NAME = "custom_scanner_store"

    @OptIn(ExperimentalUuidApi::class)
    fun save(context: Context, scanners: List<CustomScanner>) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            clear()
            for (i in scanners.indices) {
                val scanner = scanners[i]
                putString("$i.name", scanner.name)
                putString("$i.url", scanner.url.toString())
                putString("$i.uuid", scanner.uuid.toString())
            }
            putInt("count", scanners.size)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun load(context: Context): List<CustomScanner> {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val scannerList = mutableListOf<CustomScanner>()

        val count = sharedPreferences.getInt("count", 0)

        for (i in 0 until count) {
            val name = sharedPreferences.getString("$i.name", null)
            val url = sharedPreferences.getString("$i.url", null)
            val uuid = sharedPreferences.getString("$i.uuid", null)

            if (name == null || url == null || uuid == null) {
                Timber.e("Custom Scanner information that should be in stored in shared preferences is missing!")
                continue
            }

            try {
                val scanner = CustomScanner(
                    name = name,
                    url = Url(url),
                    uuid = Uuid.parse(uuid)
                )
                scannerList.add(scanner)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Invalid URL for custom scanner: $url")
                continue
            }
        }
        return scannerList
    }
}
