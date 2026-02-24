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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.chrisimx.scanbridge.db.ScanBridgeDb
import io.github.chrisimx.scanbridge.db.entities.CustomScanner
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomScannerViewModel(application: Application, appDb: ScanBridgeDb) : AndroidViewModel(application) {
    private val customScannerDao = appDb.customScannerDao()
    private val _customScanners = customScannerDao.getAllFlow()
    val customScanners: StateFlow<List<CustomScanner>> = _customScanners
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf())

    fun addScanner(scanner: CustomScanner) {
        viewModelScope.launch {
            customScannerDao.insertAll(scanner)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun deleteScanner(scanner: CustomScanner) {
        viewModelScope.launch {
            customScannerDao.delete(scanner)
        }
    }
}
