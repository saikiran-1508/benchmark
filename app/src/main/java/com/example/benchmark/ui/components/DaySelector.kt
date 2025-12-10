package com.example.benchmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.benchmark.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DaySelector(
    startDate: Calendar,    // This is "Today" (The anchor, index 0)
    selectedDate: Calendar, // The currently highlighted day
    onDateSelected: (Calendar) -> Unit
) {
    // 1. LazyListState: Controls the scroll position
    val listState = rememberLazyListState()

    // Formatter Helpers
    val dayFormatter = SimpleDateFormat("EE", Locale.getDefault()) // "Mon"
    val dateFormatter = SimpleDateFormat("d", Locale.getDefault())  // "12"

    // 2. Infinite Scrollable Row
    LazyRow(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp) // Space between items
    ) {
        // We create a very large number of items (e.g., 365 days = 1 year)
        // Since it's "Lazy", it only creates the ones on screen. Efficient!
        items(365) { index ->

            // Calculate the date for this specific slot (Today + index)
            val date = startDate.clone() as Calendar
            date.add(Calendar.DAY_OF_YEAR, index)

            val isSelected = isSameDay(date, selectedDate)

            // Visual Style
            val bgColor = if (isSelected) DarkAccent else Color.Transparent
            val textColor = if (isSelected) Color.Black else SecondaryText
            val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

            Column(
                modifier = Modifier
                    .width(50.dp) // Fixed width for uniformity
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onDateSelected(date) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Day Name (e.g., "M")
                Text(
                    text = dayFormatter.format(date.time).take(1),
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = fontWeight
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Date Number (e.g., "12")
                Text(
                    text = dateFormatter.format(date.time),
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Helper: Checks if two calendars represent the same day
fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}