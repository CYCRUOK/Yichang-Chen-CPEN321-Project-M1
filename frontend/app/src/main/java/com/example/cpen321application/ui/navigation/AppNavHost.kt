package com.example.cpen321application.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cpen321application.BuildConfig
import com.example.cpen321application.auth.CredentialManagerGoogleAuthenticator
import com.example.cpen321application.auth.GoogleAuthenticator
import com.example.cpen321application.timer.IssTracker
import com.example.cpen321application.timer.WhereTheIssAtTracker
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
    authenticator: GoogleAuthenticator? = null,
    issTracker: IssTracker = WhereTheIssAtTracker(),
) {
    // Credential Manager needs the Activity context to show its system sheet,
    // so the real authenticator is created here rather than in MainActivity.
    val context = LocalContext.current
    val googleAuthenticator = authenticator ?: remember(context) {
        CredentialManagerGoogleAuthenticator(context, BuildConfig.GOOGLE_CLIENT_ID)
    }

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
            LoginScreen(
                onBack = { navController.popBackStack() },
                authenticator = googleAuthenticator,
            )
        }
        composable(Routes.LIVE_UPDATES) {
            LiveUpdatesScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TIMER) {
            TimerScreen(
                onBack = { navController.popBackStack() },
                issTracker = issTracker,
            )
        }
    }
}
