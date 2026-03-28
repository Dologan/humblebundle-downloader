package com.dologan.humblebrowser.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    // Activity-scoped ViewModel — persists for the lifetime of the activity
    val loginViewModel: LoginViewModel = hiltViewModel()
    val isLoggedIn by loginViewModel.isLoggedIn.collectAsState()

    // Capture the auth state at first composition only, so NavHost gets a stable startDestination.
    // NavHost ignores changes to startDestination after the first composition.
    val startDestination = remember { if (isLoggedIn) Routes.BROWSER else Routes.LOGIN }

    // React to auth state changes that happen after the NavHost is already composed
    // (e.g. session expiry or logout from deep within the back stack).
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

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
                onSignIn = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.BROWSER) { inclusive = true }
                    }
                },
                onLogout = {
                    loginViewModel.logout()
                    // LaunchedEffect above handles the navigation to LOGIN
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
