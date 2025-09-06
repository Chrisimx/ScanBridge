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

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.chrisimx.scanbridge.R

@Composable
fun ImageToolbar(
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onSwapWithLeft: () -> Unit,
    onSwapWithRight: () -> Unit,
    onAnnotate: () -> Unit,
    onRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row {
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.delete_current_page)
                )
            }
            IconButton(onClick = onShare) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = stringResource(R.string.share_current_page)
                )
            }
            IconButton(onClick = onSwapWithLeft) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.swap_with_previous_page)
                )
            }
            IconButton(onClick = onSwapWithRight) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = stringResource(R.string.swap_with_next_page)
                )
            }
            IconButton(onClick = onAnnotate) {
                Icon(
                    painterResource(R.drawable.outline_edit_24),
                    contentDescription = stringResource(R.string.annotate_current_page)
                )
            }
            IconButton(onClick = onRotate) {
                Icon(
                    painterResource(R.drawable.baseline_rotate_right_24),
                    contentDescription = stringResource(R.string.rotate_right)
                )
            }
        }
    }
}