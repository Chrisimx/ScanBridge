package io.github.chrisimx.scanbridge

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import io.github.chrisimx.scanbridge.model.EditedCustomScanner
import org.jetbrains.compose.resources.StringResource

data class StartupTabDefinition(
    val nameResource: StringResource,
    val titleResource: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val fabActivated: Boolean,
    val screenComposable: @Composable (
        innerPadding: PaddingValues,
        navController: NavController,
        showCustomDialog: EditedCustomScanner?,
        setShowCustomDialog: (EditedCustomScanner?) -> Unit
    ) -> Unit
)

interface StartupTabsProvider {
    fun getStartupTabs(): List<StartupTabDefinition>
}
