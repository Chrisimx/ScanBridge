/*
 *     Copyright (C) 2024 Christian Nagel and contributors
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

import io.github.chrisimx.scanbridge.data.model.loadDefaultFormats
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PaperFormatTest {

    @Test
    fun loadDefault() {
        val expectedSizes = listOf(
            Pair(841, 1189), // A0
            Pair(594, 841), // A1
            Pair(420, 594), // A2
            Pair(297, 420), // A3
            Pair(210, 297), // A4
            Pair(148, 210), // A5
            Pair(105, 148), // A6
            Pair(74, 105), // A7
            Pair(52, 74), // A8
            Pair(37, 52), // A9
            Pair(26, 37) // A10
        )

        val paperFormats = loadDefaultFormats()

        for (i in paperFormats.indices) {
            val expected = expectedSizes[i]
            val actual = paperFormats[i]

            assertEquals(expected.first.toDouble(), actual.width.toMillimeters().value)
            assertEquals(expected.second.toDouble(), actual.height.toMillimeters().value)
        }

        println(paperFormats)
        assert(paperFormats.size == 11)
    }
}
