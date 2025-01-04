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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
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

@Composable
fun FoundScannerItem(name: String, address: String, navController: NavController) {
    ElevatedCard(modifier = Modifier
        .defaultMinSize(minHeight = 60.dp)
        .padding(10.dp),
        onClick = {
            navController.navigate(route = ScannerRoute(name, address))
        }) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 80.dp)
                .fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(17.dp),
                painter = painterResource(R.drawable.round_print_36),
                tint = MaterialTheme.colorScheme.surfaceTint,
                contentDescription = stringResource(id = R.string.print_symbol_desc)
            )
            Column {
                Text(
                    name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    address,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}