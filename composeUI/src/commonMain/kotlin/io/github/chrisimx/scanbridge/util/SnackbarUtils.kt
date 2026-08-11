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

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.Clipboard
import io.github.chrisimx.scanbridge.clipboard.toClipEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.copy
import scanbridge.composeui.generated.resources.error_while_retrieving_page

fun String.truncate(maxLength: Int): String = if (this.length <= maxLength) {
    this
} else {
    this.take(maxLength.coerceAtLeast(1) - 1) + "…"
}

enum class SnackbarType(val containerColor: Color, val contentColor: Color = Color.White) {
    SUCCESS(containerColor = Color(0xFF4CAF50)),
    ERROR(containerColor = Color(0xFFF44336)),
    WARNING(containerColor = Color(0xFFFF9800)),
    DEFAULT(containerColor = Color.DarkGray)
}

data class CustomSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val duration: SnackbarDuration = SnackbarDuration.Short,
    override val withDismissAction: Boolean = false,
    val type: SnackbarType = SnackbarType.DEFAULT
) : SnackbarVisuals

 fun snackbarErrorRetrievingPage(
    error: String,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    action: Boolean = true,
    clipboard: Clipboard
) {
     scope.launch {
         snackBarError(
             getString(Res.string.error_while_retrieving_page, error.truncate(128)),
             scope,
             snackbarHostState,
             action,
             error,
             clipboard
         )
     }
}

suspend fun SnackbarHostState.showCustomSnackbar(
    message: String,
    type: SnackbarType = SnackbarType.DEFAULT,
    actionLabel: String? = null,
    duration: SnackbarDuration = SnackbarDuration.Short,
    withDismissAction: Boolean = false
): SnackbarResult = showSnackbar(
    CustomSnackbarVisuals(
        message = message,
        actionLabel = actionLabel,
        duration = duration,
        withDismissAction = withDismissAction,
        type = type
    )
)

fun snackBarError(
    error: String,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    action: Boolean = true,
    copyData: String? = null,
    clipboard: Clipboard
) {
    scope.launch {
        val result = snackbarHostState.showCustomSnackbar(
            error,
            SnackbarType.ERROR,
            if (action) getString(Res.string.copy) else null,
            SnackbarDuration.Indefinite,
            true
        )
        when (result) {
            SnackbarResult.ActionPerformed -> {
                clipboard.setClipEntry((copyData ?: error).toClipEntry())
            }

            SnackbarResult.Dismissed -> {}
        }
    }
}
