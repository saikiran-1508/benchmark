package com.example.benchmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    // Observe the current screen to decide visibility of the Bottom Bar
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = BgColor, // Global Black Background
        bottomBar = {
            // LOGIC: Hide Bottom Bar on BOTH SignIn and SignUp screens
            if (currentRoute != Screen.Login.route && currentRoute != Screen.SignUp.route) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->

        // The Container for all your pages
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route, // App starts at Sign In
            modifier = Modifier.padding(innerPadding)
        ) {

            // 1. Sign In Screen
            composable(Screen.Login.route) {
                SignInScreen(
                    onLoginSuccess = {
                        // Navigate to Dashboard & Clear History
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
                    onSignUpSuccess = {
                        // Navigate to Dashboard & Clear History
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack() // Go back to Sign In
                    }
                )
            }

            // 3. Dashboard (Home)
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }

            // 4. Today's Tasks
            composable(Screen.DailyFocus.route) {
                DailyFocusScreen()
            }

            // 5. Profile (Placeholder)
            composable(Screen.Profile.route) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Profile Page Coming Soon", color = Color.White)
                }
            }
        }
    }
}