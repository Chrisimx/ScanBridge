package io.github.chrisimx.scanbridge

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.discovery
import scanbridge.composeui.generated.resources.header_scannerbrowser
import scanbridge.composeui.generated.resources.settings
import scanbridge.composeui.generated.resources.support

class PlayStartupTabsProvider : StartupTabsProvider {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun getStartupTabs(): List<StartupTabDefinition> = listOf(
        StartupTabDefinition(
            Res.string.discovery,
            Res.string.header_scannerbrowser,
            Icons.Filled.Home,
            Icons.Outlined.Home,
            true,
            { innerPadding, navController, showCustomDialog, setShowCustomDialog ->
                ScannerBrowser(
                    innerPadding,
                    navController,
                    showCustomDialog,
                    setShowCustomDialog
                )
            }
        ),
        StartupTabDefinition(
            Res.string.settings,
            Res.string.settings,
            Icons.Filled.Settings,
            Icons.Outlined.Settings,
            false,
            { innerPadding, _, _, _ ->
                AppSettingsScreen(innerPadding)
            }
        ),
        StartupTabDefinition(
            Res.string.support,
            Res.string.support,
            BaselineHelp24,
            OutlineHelp24,
            false,
            { innerPadding, _, _, _ ->
                SupportScreen(innerPadding)
            }
        )
    )

}

val STARTUP_TABS_PROVIDER = PlayStartupTabsProvider()
