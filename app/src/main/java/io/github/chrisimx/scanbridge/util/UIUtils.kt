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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import io.github.chrisimx.scanbridge.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun snackBarError(
    error: String,
    scope: CoroutineScope,
    context: Context,
    snackbarHostState: SnackbarHostState,
    action: Boolean = true
) {
    scope.launch {
        val result = snackbarHostState.showSnackbar(
            context.getString(R.string.error_while_retrieving_page, error),
            if (action) context.getString(R.string.copy) else null,
            true
        )
        when (result) {
            SnackbarResult.ActionPerformed -> {
                val systemClipboard =
                    context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                systemClipboard.setPrimaryClip(
                    ClipData.newPlainText(
                        context.getString(R.string.error),
                        error
                    )
                )
            }

            SnackbarResult.Dismissed -> {}
        }
    }
}