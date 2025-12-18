package com.example.benchmark

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
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
import androidx.lifecycle.viewmodel.compose.viewModel
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

        // 1. CREATE NOTIFICATION CHANNEL (Vital for Reminders)
        createNotificationChannel()

        setContent {
            MainApp()
        }
    }

    // This creates the "Pipe" for notifications to travel through
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Reminders"
            val descriptionText = "Notifications for scheduled tasks"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("benchmark_reminders", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@Composable
fun MainApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    // Smart Start: Skip login if user is already saved
    val startRoute = if (authViewModel.isUserLoggedIn()) Screen.Dashboard.route else Screen.Login.route

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            // Hide bottom bar on Login/Signup screens
            if (currentRoute != Screen.Login.route && currentRoute != Screen.SignUp.route) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Sign In
            composable(Screen.Login.route) {
                SignInScreen(
                    authViewModel = authViewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
                )
            }

            // 2. Sign Up
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    authViewModel = authViewModel,
                    onSignUpSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            // 3. Dashboard (Home)
            composable(Screen.Dashboard.route) {
                DashboardScreen()
            }

            // 4. Daily Focus
            composable(Screen.DailyFocus.route) {
                DailyFocusScreen()
            }

            // 5. Profile
            composable(Screen.Profile.route) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = {
                        authViewModel.signOut()
                        navController.navigate(Screen.Login.route) { popUpTo(0) }
                    }) { Text("Log Out") }
                }
            }
        }
    }
}