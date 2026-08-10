package io.github.chrisimx.scanbridge

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.chrisimx.scanbridge.uicomponents.FullScreenError
import org.jetbrains.compose.resources.stringResource
import scanbridge.composeui.generated.resources.Res
import scanbridge.composeui.generated.resources.internet_connection_needed
import scanbridge.composeui.generated.resources.twotone_wifi_find_24

enum class SupportScreenStates {
    SetupScreen,
    SignupScreen,
    SuccessScreen
}

@Composable
fun SupportScreen(innerPadding: PaddingValues) {
    val isInternetConnected by rememberInternetConnectivityState()
    var currentState by remember { mutableStateOf(SupportScreenStates.SetupScreen) }

    AnimatedContent(isInternetConnected) {
        if (!it) {
            FullScreenError(
                Res.drawable.twotone_wifi_find_24,
                stringResource(Res.string.internet_connection_needed)
            )
            return@AnimatedContent
        } else {
            AnimatedContent(currentState) {
                when (it) {
                    SupportScreenStates.SetupScreen -> AccountSetupScreen(Modifier.padding(innerPadding)) {
                        currentState = SupportScreenStates.SignupScreen
                    }

                    SupportScreenStates.SignupScreen -> SignupScreen(Modifier.padding(innerPadding), onBack = {
                        currentState = SupportScreenStates.SetupScreen
                    }, onSuccess = {
                        currentState = SupportScreenStates.SuccessScreen
                    })

                    SupportScreenStates.SuccessScreen -> SuccessSigningUpScreen(Modifier.padding(innerPadding), {
                        currentState = SupportScreenStates.SetupScreen
                    })
                }
            }
        }
    }
}
