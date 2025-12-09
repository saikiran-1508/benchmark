package com.example.benchmark.ui.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.benchmark.ui.Screen
import com.example.benchmark.ui.theme.BgColor
import com.example.benchmark.ui.theme.ButtonColor

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(Screen.Dashboard, Screen.DailyFocus, Screen.Profile)

    NavigationBar(
        containerColor = BgColor, // Black background
        contentColor = ButtonColor // White icons
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black, // Icon turns black when selected
                    selectedTextColor = Color.White,
                    indicatorColor = Color.White,    // White circle background when selected
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                ),
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}