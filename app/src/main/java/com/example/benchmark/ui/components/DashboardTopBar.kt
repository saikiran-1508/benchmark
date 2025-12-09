package com.example.benchmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.benchmark.ui.theme.PrimaryText
import com.example.benchmark.ui.theme.SecondaryText
import com.example.benchmark.ui.theme.SurfaceColor

@Composable
fun DashboardTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = PrimaryText)
        Text("Dashboard", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryText)

        // Profile Placeholder
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(SurfaceColor), // Updated to SurfaceColor
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Profile", tint = SecondaryText)
        }
    }
}