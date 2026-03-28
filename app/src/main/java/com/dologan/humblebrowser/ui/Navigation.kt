package com.dologan.humblebrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dologan.humblebrowser.ui.auth.LoginScreen
import com.dologan.humblebrowser.ui.auth.LoginViewModel
import com.dologan.humblebrowser.ui.browser.BrowserScreen
import com.dologan.humblebrowser.ui.settings.SettingsScreen

object Routes {
    const val LOGIN = "login"
    const val BROWSER = "browser"
    const val SETTINGS = "settings"
}

@Composable
fun HumbleBrowserNavHost() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

    val startDestination = if (isLoggedIn) Routes.BROWSER else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.BROWSER) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.BROWSER) {
            BrowserScreen(
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onLogout = {
                    loginViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.BROWSER) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
