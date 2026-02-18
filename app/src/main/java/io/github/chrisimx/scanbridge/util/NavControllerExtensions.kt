package io.github.chrisimx.scanbridge.util

import androidx.navigation.NavController

fun NavController.clearAndNavigateTo(route: Any) {
    val navController = this
    navController.navigate(route) {
        popUpTo(0) {
            inclusive = true
        }
        launchSingleTop = true
    }
    assert(navController.previousBackStackEntry == null,
        { "clearAndNavigateTo did not correctly clear backstack! Please report this."}
    )
}
