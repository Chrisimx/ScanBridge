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

import android.content.Context
import androidx.core.content.edit
import io.github.chrisimx.esclkt.BinaryRendering
import io.github.chrisimx.esclkt.CcdChannel
import io.github.chrisimx.esclkt.ColorMode
import io.github.chrisimx.esclkt.ContentType
import io.github.chrisimx.esclkt.FeedDirection
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.scanbridge.data.model.StatelessImmutableESCLScanSettingsState
import io.github.chrisimx.scanbridge.data.model.StatelessImmutableScanRegion
import timber.log.Timber

object ScanSettingsStore {
    private const val PREF_NAME = "scan_settings_store"

    fun save(context: Context, scanSettings: StatelessImmutableESCLScanSettingsState) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit {
            // Save all scan settings that we want to persist
            putString("version", scanSettings.version)
            scanSettings.documentFormatExt?.let { putString("documentFormatExt", it) }
            scanSettings.contentType?.let { putString("contentType", it.name) }
            scanSettings.inputSource?.let { putString("inputSource", it.name) }
            scanSettings.xResolution?.let { putInt("xResolution", it.toInt()) }
            scanSettings.yResolution?.let { putInt("yResolution", it.toInt()) }
            scanSettings.colorMode?.let { putString("colorMode", it.name) }
            scanSettings.colorSpace?.let { putString("colorSpace", it) }
            scanSettings.mediaType?.let { putString("mediaType", it) }
            scanSettings.ccdChannel?.let { putString("ccdChannel", it.name) }
            scanSettings.binaryRendering?.let { putString("binaryRendering", it.name) }
            scanSettings.duplex?.let { putBoolean("duplex", it) }
            scanSettings.numberOfPages?.let { putInt("numberOfPages", it.toInt()) }
            scanSettings.brightness?.let { putInt("brightness", it.toInt()) }
            scanSettings.compressionFactor?.let { putInt("compressionFactor", it.toInt()) }
            scanSettings.contrast?.let { putInt("contrast", it.toInt()) }
            scanSettings.gamma?.let { putInt("gamma", it.toInt()) }
            scanSettings.highlight?.let { putInt("highlight", it.toInt()) }
            scanSettings.noiseRemoval?.let { putInt("noiseRemoval", it.toInt()) }
            scanSettings.shadow?.let { putInt("shadow", it.toInt()) }
            scanSettings.sharpen?.let { putInt("sharpen", it.toInt()) }
            scanSettings.threshold?.let { putInt("threshold", it.toInt()) }
            scanSettings.contextID?.let { putString("contextID", it) }
            scanSettings.blankPageDetection?.let { putBoolean("blankPageDetection", it) }
            scanSettings.feedDirection?.let { putString("feedDirection", it.name) }
            scanSettings.blankPageDetectionAndRemoval?.let { putBoolean("blankPageDetectionAndRemoval", it) }
            
            // Save scan region if present
            scanSettings.scanRegions?.let { scanRegion ->
                putString("scanRegion_width", scanRegion.width)
                putString("scanRegion_height", scanRegion.height)
                putString("scanRegion_xOffset", scanRegion.xOffset)
                putString("scanRegion_yOffset", scanRegion.yOffset)
                putBoolean("scanRegion_exists", true)
            } ?: run {
                putBoolean("scanRegion_exists", false)
            }

            putBoolean("settings_saved", true)
        }
    }

    fun load(context: Context): StatelessImmutableESCLScanSettingsState? {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        if (!sharedPreferences.getBoolean("settings_saved", false)) {
            Timber.d("No saved scan settings found")
            return null
        }

        try {
            val scanRegion = if (sharedPreferences.getBoolean("scanRegion_exists", false)) {
                StatelessImmutableScanRegion(
                    sharedPreferences.getString("scanRegion_height", "max") ?: "max",
                    sharedPreferences.getString("scanRegion_width", "max") ?: "max",
                    sharedPreferences.getString("scanRegion_xOffset", "0") ?: "0",
                    sharedPreferences.getString("scanRegion_yOffset", "0") ?: "0"
                )
            } else null

            return StatelessImmutableESCLScanSettingsState(
                version = sharedPreferences.getString("version", "2.63") ?: "2.63",
                intent = null, // Intent is not typically persisted
                scanRegions = scanRegion,
                documentFormatExt = sharedPreferences.getString("documentFormatExt", null),
                contentType = sharedPreferences.getString("contentType", null)?.let { 
                    try { ContentType.valueOf(it) } catch (e: IllegalArgumentException) { null }
                },
                inputSource = sharedPreferences.getString("inputSource", null)?.let {
                    try { InputSource.valueOf(it) } catch (e: IllegalArgumentException) { null }
                },
                xResolution = if (sharedPreferences.contains("xResolution")) 
                    sharedPreferences.getInt("xResolution", 0).toUInt() else null,
                yResolution = if (sharedPreferences.contains("yResolution")) 
                    sharedPreferences.getInt("yResolution", 0).toUInt() else null,
                colorMode = sharedPreferences.getString("colorMode", null)?.let {
                    try { ColorMode.valueOf(it) } catch (e: IllegalArgumentException) { null }
                },
                colorSpace = sharedPreferences.getString("colorSpace", null),
                mediaType = sharedPreferences.getString("mediaType", null),
                ccdChannel = sharedPreferences.getString("ccdChannel", null)?.let {
                    try { CcdChannel.valueOf(it) } catch (e: IllegalArgumentException) { null }
                },
                binaryRendering = sharedPreferences.getString("binaryRendering", null)?.let {
                    try { BinaryRendering.valueOf(it) } catch (e: IllegalArgumentException) { null }
                },
                duplex = if (sharedPreferences.contains("duplex")) 
                    sharedPreferences.getBoolean("duplex", false) else null,
                numberOfPages = if (sharedPreferences.contains("numberOfPages"))
                    sharedPreferences.getInt("numberOfPages", 0).toUInt() else null,
                brightness = if (sharedPreferences.contains("brightness"))
                    sharedPreferences.getInt("brightness", 0).toUInt() else null,
                compressionFactor = if (sharedPreferences.contains("compressionFactor"))
                    sharedPreferences.getInt("compressionFactor", 0).toUInt() else null,
                contrast = if (sharedPreferences.contains("contrast"))
                    sharedPreferences.getInt("contrast", 0).toUInt() else null,
                gamma = if (sharedPreferences.contains("gamma"))
                    sharedPreferences.getInt("gamma", 0).toUInt() else null,
                highlight = if (sharedPreferences.contains("highlight"))
                    sharedPreferences.getInt("highlight", 0).toUInt() else null,
                noiseRemoval = if (sharedPreferences.contains("noiseRemoval"))
                    sharedPreferences.getInt("noiseRemoval", 0).toUInt() else null,
                shadow = if (sharedPreferences.contains("shadow"))
                    sharedPreferences.getInt("shadow", 0).toUInt() else null,
                sharpen = if (sharedPreferences.contains("sharpen"))
                    sharedPreferences.getInt("sharpen", 0).toUInt() else null,
                threshold = if (sharedPreferences.contains("threshold"))
                    sharedPreferences.getInt("threshold", 0).toUInt() else null,
                contextID = sharedPreferences.getString("contextID", null),
                blankPageDetection = if (sharedPreferences.contains("blankPageDetection"))
                    sharedPreferences.getBoolean("blankPageDetection", false) else null,
                feedDirection = sharedPreferences.getString("feedDirection", null)?.let {
                    try { FeedDirection.valueOf(it) } catch (e: IllegalArgumentException) { null }
                },
                blankPageDetectionAndRemoval = if (sharedPreferences.contains("blankPageDetectionAndRemoval"))
                    sharedPreferences.getBoolean("blankPageDetectionAndRemoval", false) else null
            )
        } catch (e: Exception) {
            Timber.e(e, "Error loading saved scan settings")
            return null
        }
    }

    fun clear(context: Context) {
        val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit { clear() }
        Timber.d("Scan settings cleared from persistent storage")
    }
}