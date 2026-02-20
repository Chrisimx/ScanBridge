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

package io.github.chrisimx.scanbridge.data.ui

import io.github.chrisimx.esclkt.ScanSettings
import io.github.chrisimx.esclkt.ScannerCapabilities
import io.github.chrisimx.scanbridge.data.model.PaperFormat
import io.github.chrisimx.scanbridge.data.model.loadDefaultFormats
import kotlinx.serialization.Serializable


@Serializable
sealed class NumberValidationResult {
    data class Success(val value: Double) : NumberValidationResult()
    data class OutOfRange(val min: Double, val max: Double) : NumberValidationResult()
    data object NotANumber : NumberValidationResult()
}

@Serializable
data class ScanSettingsComposableData(
    val scanSettings: ScanSettings,
    val capabilities: ScannerCapabilities,
    val paperFormats: List<PaperFormat> = loadDefaultFormats(),
    val customMenuEnabled: Boolean = false,
    val widthString: String = "",
    val heightString: String = "",
    val maximumSize: Boolean = true
)
