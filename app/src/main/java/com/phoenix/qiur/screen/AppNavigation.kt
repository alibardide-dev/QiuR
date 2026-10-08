package com.phoenix.qiur.screen

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phoenix.qiur.screen.home.HomeRoute
import kotlinx.serialization.Serializable

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = NavDestinations.Home) {
        composable<NavDestinations.Home> {
            HomeRoute()
        }
    }
}

sealed interface NavDestinations {
    @Serializable
    data object Home: NavDestinations
}