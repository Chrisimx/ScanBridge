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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import io.github.chrisimx.scanbridge.ScannerRoute
import io.github.chrisimx.scanbridge.ports.ScanBridgeLoggerFactory
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.delete
import scanbridge.composeui.generated.resources.edit_custom_scanner
import scanbridge.composeui.generated.resources.outline_edit_24
import scanbridge.composeui.generated.resources.print_symbol_desc
import scanbridge.composeui.generated.resources.round_print_36

@Composable
fun tintedPainterResource(drawableResource: DrawableResource, tint: Color): Painter {
    val basePainter = painterResource(drawableResource)

    return remember(basePainter, tint) {
        object : Painter() {
            override val intrinsicSize: Size
                get() = basePainter.intrinsicSize

            override fun DrawScope.onDraw() {
                with(basePainter) {
                    draw(
                        size = size,
                        colorFilter = ColorFilter.tint(tint)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
fun FoundScannerItem(
    scannerHandleString: String,
    protocolIdentifier: String,
    name: String,
    iconUrl: String?,
    navController: NavController,
    deleteScanner: (() -> Unit)? = null,
    editScanner: (() -> Unit)? = null,
    loggerFactory: ScanBridgeLoggerFactory? = koinInject()
) {
    val logger = loggerFactory?.withTag("FoundScannerItem")

    ElevatedCard(
        modifier = Modifier
            .defaultMinSize(minHeight = 60.dp)
            .widthIn(max = 700.dp)
            .padding(10.dp),
        onClick = {
            val sessionID = Uuid.random()
            navController.navigate(
                route = ScannerRoute(
                    name,
                    scannerHandleString,
                    protocolIdentifier,
                    sessionID.toString()
                )
            )
        }
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 80.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tintedPlaceholder = tintedPainterResource(
                Res.drawable.round_print_36,
                MaterialTheme.colorScheme.surfaceTint
            )

            val imageLoader: ImageLoader = koinInject(
                qualifier = named("scannerIconImageLoader")
            )

            if (iconUrl != null) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = stringResource(Res.string.print_symbol_desc),
                    imageLoader = imageLoader,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(17.dp),
                    placeholder = tintedPlaceholder,
                    error = tintedPlaceholder,
                    onError = { result ->
                        val throwable = result.result.throwable
                        throwable.printStackTrace()

                        println("Coil image load failed: ${throwable.message}")
                    }
                )
            } else {
                Icon(
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(17.dp),
                    painter = painterResource(Res.drawable.round_print_36),
                    tint = MaterialTheme.colorScheme.surfaceTint,
                    contentDescription = stringResource(Res.string.print_symbol_desc)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp)
            ) {
                Row {
                    Text(
                        name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    "$scannerHandleString ($protocolIdentifier)",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (deleteScanner != null && editScanner != null) {
                IconButton(
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                    onClick = {
                        editScanner.invoke()
                        logger?.info { "Edit button clicked for custom scanner: $name at $scannerHandleString" }
                    }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.outline_edit_24),
                        tint = MaterialTheme.colorScheme.surfaceTint,
                        contentDescription = stringResource(Res.string.edit_custom_scanner)
                    )
                }
                IconButton(
                    modifier = Modifier.padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                    onClick = {
                        deleteScanner.invoke()
                        logger?.info { "Delete button clicked for custom scanner: $name at $scannerHandleString" }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = stringResource(Res.string.delete)
                    )
                }
            }
        }
    }
}
