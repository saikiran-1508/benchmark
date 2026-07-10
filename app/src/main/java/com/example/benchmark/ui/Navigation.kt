package com.example.benchmark.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.benchmark.AuthViewModel
import com.example.benchmark.TaskViewModel
import com.example.benchmark.ui.components.BottomNavBar
import com.example.benchmark.ui.screens.DailyFocusScreen
import com.example.benchmark.ui.screens.DashboardScreen
import com.example.benchmark.ui.screens.ProfileScreen
import com.example.benchmark.ui.screens.SignInScreen
import com.example.benchmark.ui.screens.SignUpScreen
import com.example.benchmark.ui.screens.TimetableScreen
import com.example.benchmark.ui.theme.BgColor

@Composable
fun AppNavigation(viewModel: TaskViewModel) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    // Hide the bottom bar on the login/signup screens
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isAuthScreen = currentRoute == "signin" || currentRoute == "signup"

    Scaffold(
        containerColor = BgColor,
        bottomBar = { if (!isAuthScreen) BottomNavBar(navController = navController) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            // Already signed in? Skip the login screen entirely.
            startDestination = if (authViewModel.isUserLoggedIn()) Screen.Dashboard.route else "signin",
            modifier = Modifier.padding(paddingValues)
        ) {
            // --- AUTH FLOW ---
            composable("signin") {
                SignInScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true } // clear auth screens from back stack
                        }
                    },
                    onNavigateToSignUp = { navController.navigate("signup") }
                )
            }
            composable("signup") {
                SignUpScreen(
                    authViewModel = authViewModel,
                    onSignUpSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            // --- MAIN APP ---
            composable(Screen.Dashboard.route) {
                DashboardScreen(navController = navController, viewModel = viewModel)
            }
            composable(Screen.Timetable.route) {
                TimetableScreen(viewModel = viewModel)
            }
            composable(Screen.DailyFocus.route) {
                DailyFocusScreen(viewModel = viewModel)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    authViewModel = authViewModel,
                    onSignOut = {
                        navController.navigate("signin") {
                            popUpTo(0) { inclusive = true } // sign out clears everything
                        }
                    }
                )
            }
        }
    }
}
