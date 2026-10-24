package com.phoenix.qiur.screen

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phoenix.qiur.screen.about.AboutRoute
import com.phoenix.qiur.screen.home.HomeRoute
import com.phoenix.qiur.screen.setting.SettingRoute
import kotlinx.serialization.Serializable

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = NavDestinations.Home) {
        composable<NavDestinations.Home> {
            HomeRoute(
                navigateToAbout = { navController.navigate(NavDestinations.About) },
                navigateToSetting = { navController.navigate(NavDestinations.Setting) }
            )
        }
        composable<NavDestinations.Setting>  { SettingRoute() }
        composable<NavDestinations.About>  { AboutRoute { navController.popBackStack() } }
    }
}

sealed interface NavDestinations {
    @Serializable
    data object Home: NavDestinations
    @Serializable
    data object Setting: NavDestinations
    @Serializable
    data object About: NavDestinations
}