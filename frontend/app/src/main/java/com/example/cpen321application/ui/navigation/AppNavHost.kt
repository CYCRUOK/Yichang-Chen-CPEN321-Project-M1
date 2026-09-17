package com.example.cpen321application.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cpen321application.ui.home.HomeScreen
import com.example.cpen321application.ui.live.LiveUpdatesScreen
import com.example.cpen321application.ui.login.LoginScreen
import com.example.cpen321application.ui.timer.TimerScreen

/** Navigation routes. Each button on the home screen maps to exactly one route. */
object Routes {
    const val HOME = "home"
    const val LOGIN = "login"
    const val LIVE_UPDATES = "live_updates"
    const val TIMER = "timer"
}

@Composable
fun AppNavHost(
    apiBaseUrl: String,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                apiBaseUrl = apiBaseUrl,
                onLoginClick = { navController.navigate(Routes.LOGIN) },
                onLiveUpdatesClick = { navController.navigate(Routes.LIVE_UPDATES) },
                onTimerClick = { navController.navigate(Routes.TIMER) },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LIVE_UPDATES) {
            LiveUpdatesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TIMER) {
            TimerScreen(onBack = { navController.popBackStack() })
        }
    }
}
