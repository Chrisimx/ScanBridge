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
import android.icu.text.DecimalFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.chrisimx.esclkt.JobState
import io.github.chrisimx.scanbridge.R

fun JobState?.toJobStateString(context: Context): String = when (this) {
    JobState.Canceled -> context.getString(R.string.job_canceled)
    JobState.Aborted -> context.getString(R.string.job_aborted_because_error)
    JobState.Completed -> context.getString(R.string.job_completed_successfully)
    JobState.Pending -> context.getString(R.string.job_still_pending)
    JobState.Processing -> context.getString(R.string.job_pages_still_processed)
    null -> context.getString(R.string.job_state_cannot_be_retrieved)
}

fun String.toDoubleLocalized(): Double? = runCatching {
    DecimalFormat.getInstance().parse(this).toDouble()
}.getOrNull()

fun Double.toStringLocalized(): String = DecimalFormat.getInstance().format(this)

@Composable
fun UIInputSourceType.toReadableString(): String = when (this) {
    UIInputSourceType.PLATEN ->
        stringResource(R.string.platen)

    UIInputSourceType.ADF -> stringResource(R.string.adf)
}

/**
 * Returns the caller object if it is contained in the provided list; otherwise, returns the first
 * element of the list or null if the list is empty.
 *
 * @param list The list to search for the caller object.
 * @return The caller object if it is contained in the list, the first element of the list if not contained,
 *         or null if the list is empty.
 */
fun <T> T.takeIfContainedElseFirstOrNull(list: List<T>): T? = if (list.contains(this)) {
    this
} else {
    list.firstOrNull()
}
