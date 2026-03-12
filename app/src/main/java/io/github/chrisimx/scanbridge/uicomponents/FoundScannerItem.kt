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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.github.chrisimx.scanbridge.R
import io.github.chrisimx.scanbridge.ScannerRoute
import java.util.*
import kotlin.uuid.Uuid
import timber.log.Timber

@Composable
fun FoundScannerItem(
    name: String,
    address: String,
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
            navController.navigate(route = ScannerRoute(name, address, sessionID.toString()))
        }
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 80.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(17.dp),
                painter = painterResource(R.drawable.round_print_36),
                tint = MaterialTheme.colorScheme.surfaceTint,
                contentDescription = stringResource(id = R.string.print_symbol_desc)
            )
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
                    address,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            if (deleteScanner != null) {
                IconButton(
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 8.dp),
                    onClick = {
                        editScanner!!.invoke()
                        Timber.i("Edit button clicked for custom scanner: $name at $address")
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.outline_edit_24),
                        tint = MaterialTheme.colorScheme.surfaceTint,
                        contentDescription = stringResource(id = R.string.custom)
                    )
                }
                IconButton(
                    modifier = Modifier.padding(end = 8.dp, top = 8.dp, bottom = 8.dp),
                    onClick = {
                        deleteScanner.invoke()
                        Timber.i("Delete button clicked for custom scanner: $name at $address")
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
