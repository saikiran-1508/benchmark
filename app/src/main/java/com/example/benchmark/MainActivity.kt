package com.example.benchmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable 
import androidx.navigation.compose.rememberNavController
import com.example.benchmark.ui.Screen
import com.example.benchmark.ui.components.BottomNavBar
import com.example.benchmark.ui.screens.DashboardScreen
import com.example.benchmark.ui.screens.DailyFocusScreen

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

    Scaffold(
        bottomBar = { BottomNavBar(navController = navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Dashboard Route
            composable(Screen.Dashboard.route) {
                // This calls your existing DashboardScreen (ensure it doesn't have its own Scaffold)
                DashboardScreen()
            }

            // 2. Daily Focus Route
            composable(Screen.DailyFocus.route) {
                DailyFocusScreen()
            }

            // 3. Profile Route (Placeholder)
            composable(Screen.Profile.route) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text("Profile Page", color = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}