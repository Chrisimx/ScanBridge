package io.github.chrisimx.scanbridge

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
val STARTUP_TABS = listOf(
    StartupScreen(
        io.github.chrisimx.scanbridge.R.string.discovery,
        io.github.chrisimx.scanbridge.R.string.header_scannerbrowser,
        Icons.Filled.Home,
        Icons.Outlined.Home,
        true,
        { innerPadding, navController, showCustomDialog, setShowCustomDialog, statefulScannerMap, statefulScannerMapSecure ->
            ScannerBrowser(
                innerPadding,
                navController,
                showCustomDialog,
                setShowCustomDialog,
                statefulScannerMap,
                statefulScannerMapSecure
            )
        }
    ),
    StartupScreen(
        io.github.chrisimx.scanbridge.R.string.settings,
        io.github.chrisimx.scanbridge.R.string.settings,
        Icons.Filled.Settings,
        Icons.Outlined.Settings,
        false,
        { innerPadding, _, _, _, _, _ ->
            AppSettingsScreen(innerPadding)
        }
    ),
    StartupScreen(
        R.string.support,
        R.string.support,
        BaselineHelp24,
        OutlineHelp24,
        false,
        { innerPadding, _, _, _, _, _ ->
            SupportScreen(innerPadding)
        }
    )
)
