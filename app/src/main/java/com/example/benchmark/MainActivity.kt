package com.example.benchmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel // Import this!
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.benchmark.ui.Screen
import com.example.benchmark.ui.components.BottomNavBar
import com.example.benchmark.ui.screens.DailyFocusScreen
import com.example.benchmark.ui.screens.DashboardScreen
import com.example.benchmark.ui.screens.SignInScreen
import com.example.benchmark.ui.screens.SignUpScreen
import com.example.benchmark.ui.theme.BgColor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainApp()
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()

    // 1. Initialize the Auth Logic Engine
    val authViewModel: AuthViewModel = viewModel()

    // 2. Decide where to start: If logged in -> Dashboard. If not -> Login.
    val startRoute = if (authViewModel.isUserLoggedIn()) Screen.Dashboard.route else Screen.Login.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            if (currentRoute != Screen.Login.route && currentRoute != Screen.SignUp.route) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = startRoute, // Use the smart start route
            modifier = Modifier.padding(innerPadding)
        ) {

            // 1. Sign In Screen
            composable(Screen.Login.route) {
                SignInScreen(
                    authViewModel = authViewModel, // PASS THE VIEWMODEL HERE
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = {
                        navController.navigate(Screen.SignUp.route)
                    }
                )
            }

            // 2. Sign Up Screen
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    authViewModel = authViewModel, // PASS THE VIEWMODEL HERE
                    onSignUpSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            // 3. Dashboard
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }

            // 4. Today's Tasks
            composable(Screen.DailyFocus.route) {
                DailyFocusScreen()
            }

            // 5. Profile (With Logout Button for Testing)
            composable(Screen.Profile.route) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = {
                        // Log out and go back to Login screen
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) // Clear all history
                        }
                    }) {
                        Text("Log Out")
                    }
                }
            }
        }
    }
}