package com.example.benchmark.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    data object Timetable : Screen("timetable", "Grid", Icons.Default.CalendarMonth)
    data object DailyFocus : Screen("dailyfocus", "Focus", Icons.Default.CenterFocusStrong)
    data object Profile : Screen("profile", "Profile", Icons.Default.Person)
}
