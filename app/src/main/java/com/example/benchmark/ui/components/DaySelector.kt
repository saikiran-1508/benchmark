package com.example.benchmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
fun DaySelector(
    selectedDay: String,
    onDaySelected: (String) -> Unit
) {
    // We use a list of Pairs: "Unique ID" to "Display Label"
    // Order changed to start with SUNDAY as you requested.
    val days = listOf(
        "Sunday"    to "S",
        "Monday"    to "M",
        "Tuesday"   to "T",
        "Wednesday" to "W",
        "Thursday"  to "T",
        "Friday"    to "F",
        "Saturday"  to "S"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        days.forEach { (fullDayName, label) ->
            // Logic: Compare the FULL NAME ("Tuesday"), not the label ("T")
            val isSelected = (fullDayName == selectedDay)

            val bgColor = if (isSelected) DarkAccent else Color.Transparent
            val textColor = if (isSelected) Color.Black else SecondaryText

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(bgColor)
                    .clickable { onDaySelected(fullDayName) }, // Pass full name back
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label, // Show only the letter
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}