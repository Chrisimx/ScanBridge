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

package io.github.chrisimx.scanbridge.uicomponents

import android.content.Context
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.data.ui.NumberValidationResult
fun NumberValidationResult.toHumanString(context: Context): String = when (this) {
    is NumberValidationResult.OutOfRange -> context.getString(R.string.error_state_not_in_allowed_range)
    NumberValidationResult.NotANumber -> context.getString(R.string.error_state_not_a_valid_number)
    is NumberValidationResult.Success -> context.getString(R.string.error_state_valid)
}

@Composable
fun ValidatedDimensionsTextEdit(
    text: String,
    context: Context,
    modifier: Modifier = Modifier,
    label: String,
    updateContent: (String) -> Unit,
    validationResult: NumberValidationResult
) {
    OutlinedTextField(
        modifier = modifier,
        value = text,
        onValueChange = { newValue: String ->
            updateContent(newValue)
        },
        supportingText = {
            if (validationResult !is NumberValidationResult.Success) {
                Text(
                    validationResult.toHumanString(
                        context
                    ),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        isError = validationResult !is NumberValidationResult.Success,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Decimal
        ),
        label = @Composable { Text(label, style = MaterialTheme.typography.labelMedium) },
        singleLine = true
    )
}
