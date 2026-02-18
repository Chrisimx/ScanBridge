package io.github.chrisimx.scanbridge.util

import androidx.navigation.NavController
import timber.log.Timber

fun NavController.clearAndNavigateTo(route: Any) {
    val navController = this
    navController.navigate(route) {
        popUpTo(0) {
            inclusive = true
        }
        launchSingleTop = true
    }
    check(navController.previousBackStackEntry == null, { "clearAndNavigateTo did not correctly clear backstack!" })
}
