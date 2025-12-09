package com.example.benchmark.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Home)
    object DailyFocus : Screen("daily_focus", "Today", Icons.Default.DateRange)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}