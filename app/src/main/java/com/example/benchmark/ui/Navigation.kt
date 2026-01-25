package com.example.benchmark.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    // Auth Routes
    object Login : Screen("login", "Login", Icons.Default.Person)
    object SignUp : Screen("signup", "Sign Up", Icons.Default.Person)

    // Main Tabs
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Timetable : Screen("timetable", "Week", Icons.Default.DateRange) // <--- NEW
    object DailyFocus : Screen("daily_focus", "Focus", Icons.Default.PlayArrow)
    object Profile : Screen("profile", "Profile", Icons.Default.AccountCircle)
}