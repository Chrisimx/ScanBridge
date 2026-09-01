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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.chrisimx.scanbridge.buildinfo.ScanBridgeEdition
import io.github.chrisimx.scanbridge.theme.Poppins
import io.github.chrisimx.scanbridge.theme.gradientBrush
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.app_icon_desc
import scanbridge.composeui.generated.resources.app_name
import scanbridge.composeui.generated.resources.debug_build
import scanbridge.composeui.generated.resources.desktop_edition
import scanbridge.composeui.generated.resources.f_droid
import scanbridge.composeui.generated.resources.google_play
import scanbridge.composeui.generated.resources.icon_about_dialog
import scanbridge.composeui.generated.resources.ios

@Composable
fun ScanBridgeEdition.toLocalizedString(): String = when (this) {
    ScanBridgeEdition.FDROID -> stringResource(Res.string.f_droid)
    ScanBridgeEdition.PLAYSTORE -> stringResource(Res.string.google_play)
    ScanBridgeEdition.IOS -> stringResource(Res.string.ios)
    ScanBridgeEdition.DESKTOP -> stringResource(Res.string.desktop_edition)
}

@Composable
fun VersionComposable(versionName: String, versionCode: Int, gitCommitHash: String, edition: ScanBridgeEdition, debugBuild: Boolean) {
    Image(
        modifier = Modifier
            .size(200.dp)
            .padding(16.dp),
        painter = painterResource(Res.drawable.icon_about_dialog),
        contentDescription = stringResource(Res.string.app_icon_desc)
    )

    Text(
        stringResource(Res.string.app_name),
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
        editionNotice.add(stringResource(Res.string.debug_build))
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
