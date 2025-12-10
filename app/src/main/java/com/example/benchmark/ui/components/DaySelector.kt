package com.example.benchmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
    startDate: Calendar,    // The first day to show in the list
    selectedDate: Calendar, // The day currently highlighted
    onDateSelected: (Calendar) -> Unit
) {
    // Generate the next 7 days starting from 'startDate'
    val daysToShow = (0..6).map { offset ->
        val date = startDate.clone() as Calendar
        date.add(Calendar.DAY_OF_YEAR, offset)
        date
    }

    val dayFormatter = SimpleDateFormat("EE", Locale.getDefault()) // "Mon"
    val dateFormatter = SimpleDateFormat("d", Locale.getDefault())  // "12"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        daysToShow.forEach { date ->
            val isSelected = isSameDay(date, selectedDate)

            // Visual Style
            val bgColor = if (isSelected) DarkAccent else Color.Transparent
            val textColor = if (isSelected) Color.Black else SecondaryText

            Column(
                modifier = Modifier
                    .width(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onDateSelected(date) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Day Name
                Text(
                    text = dayFormatter.format(date.time).take(1),
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Date Number
                Text(
                    text = dateFormatter.format(date.time),
                    color = textColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}