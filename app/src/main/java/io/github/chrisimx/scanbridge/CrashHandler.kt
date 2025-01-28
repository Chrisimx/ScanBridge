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

package io.github.chrisimx.scanbridge

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import timber.log.Timber

class CrashHandler(val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        Timber.e(e, "Uncaught exception")

        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
        val dateTime = LocalDateTime.now().format(format)

        val crashDir = File(context.filesDir, "crashes")
        if (!crashDir.exists()) {
            if (!crashDir.mkdirs()) {
                Timber.e("Couldn't create crash directory")
                File(context.filesDir, "crash-$dateTime.log").writeText(e.stackTraceToString())
                return
            }
        }
        File(crashDir, "crash-$dateTime.log").writeText(e.stackTraceToString())

        defaultHandler?.uncaughtException(t, e)
    }
}
