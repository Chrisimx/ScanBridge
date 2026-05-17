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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.ImageLoader
import coil3.compose.AsyncImage
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.ScannerRoute
import io.github.chrisimx.scanbridge.model.ScannerHandle
import io.github.chrisimx.scanbridge.model.UrlScannerHandle
import io.ktor.http.Url
import java.util.*
import kotlin.uuid.Uuid
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import timber.log.Timber

@Composable
fun tintedPainterResource(
    id: Int,
    tint: Color,
): Painter {
    val basePainter = painterResource(id)

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

@Composable
fun FoundScannerItem(
    name: String,
    iconUrl: Url?,
    handle: ScannerHandle,
    navController: NavController,
    deleteScanner: (() -> Unit)? = null,
    editScanner: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = Modifier
            .defaultMinSize(minHeight = 60.dp)
            .widthIn(max = 700.dp)
            .padding(10.dp),
        onClick = {
            val sessionID = Uuid.random()
            // TODO: Use scanner handle instead of URL
            val url = (handle as UrlScannerHandle).url
            navController.navigate(route = ScannerRoute(name, url.toString(), sessionID.toString()))
        }
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 80.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tintedPlaceholder = tintedPainterResource(
                io.github.chrisimx.scanbridge.R.drawable.round_print_36,
                MaterialTheme.colorScheme.surfaceTint
            )

            val imageLoader: ImageLoader = koinInject(
                qualifier = named("scannerIconImageLoader")
            )

            if (iconUrl != null) {
                AsyncImage(
                    model = iconUrl.toString(),
                    contentDescription = stringResource(id = R.string.print_symbol_desc),
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
                    painter = painterResource(R.drawable.round_print_36),
                    tint = MaterialTheme.colorScheme.surfaceTint,
                    contentDescription = stringResource(id = R.string.print_symbol_desc)
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
                    "${handle.stringRepresentation} (${handle.protocol.protocolIdentifier})",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (deleteScanner != null && editScanner != null) {
                IconButton(
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                    onClick = {
                        editScanner.invoke()
                        Timber.i("Edit button clicked for custom scanner: $name at $handle")
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_edit_24),
                        tint = MaterialTheme.colorScheme.surfaceTint,
                        contentDescription = stringResource(id = R.string.edit_custom_scanner)
                    )
                }
                IconButton(
                    modifier = Modifier.padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                    onClick = {
                        deleteScanner.invoke()
                        Timber.i("Delete button clicked for custom scanner: $name at $handle")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = stringResource(id = R.string.delete)
                    )
                }
            }
        }
    }
}
