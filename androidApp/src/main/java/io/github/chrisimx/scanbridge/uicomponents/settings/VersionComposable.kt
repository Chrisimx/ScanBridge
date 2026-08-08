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

package io.github.chrisimx.scanbridge.uicomponents.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.buildinfo.ScanBridgeEdition
import io.github.chrisimx.scanbridge.theme.Poppins
import io.github.chrisimx.scanbridge.theme.gradientBrush

@Composable
fun ScanBridgeEdition.toLocalizedString(): String = when (this) {
    ScanBridgeEdition.FDROID -> stringResource(R.string.f_droid)
    ScanBridgeEdition.PLAYSTORE -> stringResource(R.string.google_play)
    ScanBridgeEdition.IOS -> stringResource(R.string.ios)
}

@Composable
fun VersionComposable(versionName: String, versionCode: Int, gitCommitHash: String, edition: ScanBridgeEdition, debugBuild: Boolean) {
    Image(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp),
        painter = painterResource(R.drawable.icon_about_dialog),
        contentDescription = stringResource(id = R.string.app_icon_desc)
    )

    Text(
        stringResource(R.string.app_name),
        modifier = Modifier.padding(PaddingValues(4.dp)),
        fontFamily = Poppins(),
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        style = MaterialTheme.typography.labelLarge.copy(
            brush = gradientBrush
        )
    )

    Text(
        "${versionName.removeSuffix("-play")} ($versionCode, $gitCommitHash)",
        fontStyle = FontStyle.Normal,
        fontFamily = Poppins()
    )

    val editionNotice = mutableListOf<String>()

    editionNotice.add(edition.toLocalizedString())

    if (debugBuild) {
        editionNotice.add(stringResource(R.string.debug_build))
    }

    Text(
        editionNotice.joinToString(),
        fontStyle = FontStyle.Italic,
        fontFamily = Poppins()
    )
}

@Preview
@Composable
fun VersionComposablePreview() {
    Column(
        modifier = Modifier
            .width(300.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VersionComposable(
            "2.1.0",
            2100,
            "abdfefg",
            ScanBridgeEdition.FDROID,
            true
        )
    }
}
