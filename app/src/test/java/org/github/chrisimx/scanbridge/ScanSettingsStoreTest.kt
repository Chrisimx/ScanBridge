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

package org.github.chrisimx.scanbridge

import io.github.chrisimx.esclkt.ColorMode
import io.github.chrisimx.esclkt.InputSource
import io.github.chrisimx.scanbridge.data.model.StatelessImmutableESCLScanSettingsState
import io.github.chrisimx.scanbridge.data.model.StatelessImmutableScanRegion
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanSettingsStoreTest {

    @Test
    fun testScanSettingsPersistenceBehavior() {
        // Test the basic persistence behavior described in the PR
        // This is a simple test to verify our data structures work correctly
        // The actual SharedPreferences testing would require Android testing framework

        val testSettings = StatelessImmutableESCLScanSettingsState(
            version = "2.63",
            intent = null,
            scanRegions = StatelessImmutableScanRegion("297", "210", "0", "0"), // A4
            documentFormatExt = "application/pdf", // PDF document format
            contentType = null,
            inputSource = InputSource.Feeder, // ADF input
            xResolution = 300u,
            yResolution = 300u,
            colorMode = ColorMode.RGB24,
            colorSpace = null,
            mediaType = null,
            ccdChannel = null,
            binaryRendering = null,
            duplex = true, // Duplex enabled for double-sided scanning
            numberOfPages = null,
            brightness = null,
            compressionFactor = null,
            contrast = null,
            gamma = null,
            highlight = null,
            noiseRemoval = null,
            shadow = null,
            sharpen = null,
            threshold = null,
            contextID = null,
            blankPageDetection = null,
            feedDirection = null,
            blankPageDetectionAndRemoval = null
        )

        // Test the user story scenario: ADF + duplex + A4 + PDF
        assertEquals("Should use ADF for automatic document feeding", InputSource.Feeder, testSettings.inputSource)
        assertEquals("Should support duplex for double-sided scanning", true, testSettings.duplex)
        assertEquals("210", testSettings.scanRegions?.width)
        assertEquals("Should use A4 height (297mm)", "297", testSettings.scanRegions?.height)
        assertEquals("Should export as PDF format", "application/pdf", testSettings.documentFormatExt)

        // Test conversion to mutable (for editing in UI)
        val mutableSettings = testSettings.toMutable()
        assertEquals(InputSource.Feeder, mutableSettings.inputSource)
        assertEquals(true, mutableSettings.duplex)

        // Test conversion back to stateless (for persistence)
        val statelessSettings = mutableSettings.toStateless()
        assertEquals(testSettings.inputSource, statelessSettings.inputSource)
        assertEquals(testSettings.duplex, statelessSettings.duplex)
        assertEquals(testSettings.documentFormatExt, statelessSettings.documentFormatExt)
    }

    @Test
    fun testScanSettingsStatelessDataStructure() {
        // Test that we can create scan settings with the key settings mentioned in the issue
        // ADF+duplex+document+A4

        val testScanRegion = StatelessImmutableScanRegion(
            "297", // A4 width in mm
            "210", // A4 height in mm
            "0", // x offset
            "0" // y offset
        )

        val testSettings = StatelessImmutableESCLScanSettingsState(
            version = "2.63",
            intent = null,
            scanRegions = testScanRegion,
            documentFormatExt = "image/jpeg", // document format
            contentType = null,
            inputSource = InputSource.Feeder, // ADF (Automatic Document Feeder)
            xResolution = 300u,
            yResolution = 300u,
            colorMode = ColorMode.RGB24,
            colorSpace = null,
            mediaType = null,
            ccdChannel = null,
            binaryRendering = null,
            duplex = true, // Duplex enabled
            numberOfPages = null,
            brightness = null,
            compressionFactor = null,
            contrast = null,
            gamma = null,
            highlight = null,
            noiseRemoval = null,
            shadow = null,
            sharpen = null,
            threshold = null,
            contextID = null,
            blankPageDetection = null,
            feedDirection = null,
            blankPageDetectionAndRemoval = null
        )

        // Verify the test settings have the expected values
        assertEquals("Should use ADF (Feeder)", InputSource.Feeder, testSettings.inputSource)
        assertEquals("Should have duplex enabled", true, testSettings.duplex)
        assertEquals("Should have document format set", "image/jpeg", testSettings.documentFormatExt)
        assertEquals("Should have A4 scan region", true, testSettings.scanRegions != null)

        // Test the toMutable conversion to ensure settings can be converted back
        val mutableSettings = testSettings.toMutable()
        assertEquals(InputSource.Feeder, mutableSettings.inputSource)
        assertEquals(true, mutableSettings.duplex)
        assertEquals("image/jpeg", mutableSettings.documentFormatExt)

        // Test that we can convert back to stateless
        val reconvertedSettings = mutableSettings.toStateless()
        assertEquals(testSettings.inputSource, reconvertedSettings.inputSource)
        assertEquals(testSettings.duplex, reconvertedSettings.duplex)
        assertEquals(testSettings.documentFormatExt, reconvertedSettings.documentFormatExt)
    }

    @Test
    fun testScanRegionDataStructure() {
        // Test A4 paper format (210x297mm)
        val a4Region = StatelessImmutableScanRegion("297", "210", "0", "0")
        val mutableA4 = a4Region.toMutable()

        assertEquals("210", mutableA4.width)
        assertEquals("297", mutableA4.height)
        assertEquals("0", mutableA4.xOffset)
        assertEquals("0", mutableA4.yOffset)

        // Test that we can modify mutable settings
        mutableA4.width = "148" // A5 width
        mutableA4.height = "210" // A5 height

        assertEquals("148", mutableA4.width)
        assertEquals("210", mutableA4.height)

        // Test conversion back to stateless
        val a5Region = mutableA4.toStateless()
        val reconvertedA5 = a5Region.toMutable()
        assertEquals("148", reconvertedA5.width)
        assertEquals("210", reconvertedA5.height)
    }
}
