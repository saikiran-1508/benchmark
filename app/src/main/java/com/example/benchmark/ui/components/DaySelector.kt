package com.example.benchmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.benchmark.ui.theme.*

@Composable
fun DaySelector() {
    val days = listOf("M", "T", "W", "T", "F")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEachIndexed { index, day ->
            // Simulating Wednesday (Index 2) being selected
            val isSelected = index == 2

            // LOGIC:
            // If selected -> Background is White (DarkAccent), Text is Black.
            // If not -> Background is Transparent, Text is Gray (SecondaryText).
            val bgColor = if (isSelected) DarkAccent else Color.Transparent
            val textColor = if (isSelected) Color.Black else SecondaryText

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}