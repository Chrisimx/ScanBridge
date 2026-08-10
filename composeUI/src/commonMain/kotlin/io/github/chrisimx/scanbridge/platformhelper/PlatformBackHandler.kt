package io.github.chrisimx.scanbridge.platformhelper

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit
)
