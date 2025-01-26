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
import android.icu.text.DecimalFormatSymbols
import android.util.Log
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.util.toDoubleLocalized

enum class ErrorState {
    NOT_WITHIN_ALLOWED_RANGE,
    NOT_VALID_NUMBER,
    NO_ERROR
}

fun ErrorState.toHumanString(context: Context): String = when (this) {
    ErrorState.NOT_WITHIN_ALLOWED_RANGE -> context.getString(R.string.error_state_not_in_allowed_range)
    ErrorState.NOT_VALID_NUMBER -> context.getString(R.string.error_state_not_a_valid_number)
    ErrorState.NO_ERROR -> context.getString(R.string.error_state_valid)
}

@Composable
fun ValidatedDimensionsTextEdit(
    localContent: String,
    context: Context,
    modifier: Modifier = Modifier,
    label: String,
    updateContent: (String) -> Unit,
    updateDimensionState: (String) -> Unit,
    min: Double,
    max: Double
) {
    val errorState = remember { mutableStateOf(ErrorState.NO_ERROR) }

    val decimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator
    val decimalNumberRegex =
        "^[+]?\\d*(${Regex.escape(decimalSeparator.toString())})?\\d+\$".toRegex()

    OutlinedTextField(
        modifier = modifier,
        value = localContent,
        onValueChange = { newValue: String ->
            updateContent(newValue)

            val isValidNumber = newValue.matches(decimalNumberRegex)
            if (isValidNumber) {
                val newNumber = newValue.toDoubleLocalized()
                if (newNumber > max || newNumber < min) {
                    errorState.value = ErrorState.NOT_WITHIN_ALLOWED_RANGE
                    return@OutlinedTextField
                }
                errorState.value = ErrorState.NO_ERROR
                updateDimensionState(newValue)

                return@OutlinedTextField
            } else {
                errorState.value = ErrorState.NOT_VALID_NUMBER
                Log.d("ScanSettings", "Invalid Number")
                return@OutlinedTextField
            }
        },
        supportingText = {
            if (errorState.value != ErrorState.NO_ERROR) {
                Text(
                    errorState.value.toHumanString(
                        context
                    ),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        isError = errorState.value != ErrorState.NO_ERROR,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Decimal
        ),
        label = @Composable { Text(label, style = MaterialTheme.typography.labelMedium) },
        singleLine = true
    )
}
