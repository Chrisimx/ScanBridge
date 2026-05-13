package io.github.chrisimx.scanbridge.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun platformDarkColorScheme(): ColorScheme? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val context = LocalContext.current
    dynamicDarkColorScheme(context)
} else {
    null
}

@Composable
actual fun platformLightColorScheme(): ColorScheme? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val context = LocalContext.current
    dynamicLightColorScheme(context)
} else {
    null
}
