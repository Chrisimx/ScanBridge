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
import io.github.chrisimx.scanbridge.data.model.CustomPrinter
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import okhttp3.HttpUrl.Companion.toHttpUrl
import timber.log.Timber

object CustomPrinterStore {
    private const val PREF_NAME = "custom_printer_store"

    @OptIn(ExperimentalUuidApi::class)
    fun save(context: Context, printers: List<CustomPrinter>) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            clear()
            for (i in printers.indices) {
                val printer = printers[i]
                putString("$i.name", printer.name)
                putString("$i.url", printer.url.toString())
                putString("$i.uuid", printer.uuid.toString())
            }
            putInt("count", printers.size)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun load(context: Context): List<CustomPrinter> {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val printerList = mutableListOf<CustomPrinter>()

        val count = sharedPreferences.getInt("count", 0)

        for (i in 0 until count) {
            val name = sharedPreferences.getString("$i.name", null)
            val url = sharedPreferences.getString("$i.url", null)
            val uuid = sharedPreferences.getString("$i.uuid", null)

            if (name == null || url == null || uuid == null) {
                Timber.e("Custom Printer information that should be in stored in shared preferences is missing!")
                continue
            }

            try {
                val printer = CustomPrinter(
                    name = name,
                    url = url.toHttpUrl(),
                    uuid = Uuid.parse(uuid)
                )
                printerList.add(printer)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Invalid URL for custom printer: $url")
                continue
            }
        }
        return printerList
    }
}