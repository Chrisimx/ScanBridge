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

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import io.github.chrisimx.scanbridge.data.model.CustomScanner
import io.github.chrisimx.scanbridge.util.CustomScannerStore
import kotlin.uuid.ExperimentalUuidApi

class CustomScannerViewModel(application: Application) : AndroidViewModel(application) {
    private val context = getApplication<Application>()

    val customScanners = mutableStateListOf<CustomScanner>()

    init {
        val loadedScanners = CustomScannerStore.load(context)
        customScanners.addAll(loadedScanners)
    }

    fun addScanner(scanner: CustomScanner) {
        customScanners.add(scanner)
        CustomScannerStore.save(context, customScanners)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun deleteScanner(scanner: CustomScanner) {
        customScanners.removeIf { it.uuid == scanner.uuid }
        CustomScannerStore.save(context, customScanners)
    }
}
