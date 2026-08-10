package io.github.chrisimx.scanbridge.clipboard

import androidx.compose.ui.platform.ClipEntry

expect fun String.toClipEntry(): ClipEntry
