package io.github.chrisimx.scanbridge.util

import androidx.navigation.NavController

fun NavController.clearAndNavigateTo(route: Any) {
    val navController = this
    navController.navigate(route) {
        popUpTo(navController.graph.startDestinationId) {
            inclusive = true
        }
        launchSingleTop = true
    }
}
