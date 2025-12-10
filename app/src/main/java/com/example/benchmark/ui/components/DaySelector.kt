package com.example.benchmark.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.benchmark.ui.screens.isSameDay
import com.example.benchmark.ui.theme.DarkAccent
import com.example.benchmark.ui.theme.SecondaryText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DaySelector(
    listState: LazyListState,
    startDate: Calendar,
    selectedDate: Calendar,
    onDateSelected: (Calendar) -> Unit
) {
    val dayFormatter = SimpleDateFormat("EE", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("d", Locale.getDefault())

    // Enables the "Snap" effect so scrolling stops exactly on a day
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LazyRow(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Render 365 Days (1 Year into the future)
        items(365) { index ->
            val date = startDate.clone() as Calendar
            date.add(Calendar.DAY_OF_YEAR, index)

            val isSelected = isSameDay(date, selectedDate)

            // Logic: Mark Sundays as Red
            val isSunday = date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            val dayNameColor = if (isSunday) Color(0xFFFF5252) else SecondaryText

            val bgColor = if (isSelected) DarkAccent else Color.Transparent
            val textColor = if (isSelected) Color.Black else SecondaryText
            val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

            Column(
                modifier = Modifier
                    .width(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onDateSelected(date) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Day Name (M, T, W...)
                Text(
                    text = dayFormatter.format(date.time).take(1),
                    color = if (isSelected) Color.Black else dayNameColor, // Keep selected black, otherwise Red/Gray
                    fontSize = 12.sp,
                    fontWeight = fontWeight
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Date Number (12, 13...)
                Text(
                    text = dateFormatter.format(date.time),
                    color = if (isSelected) Color.Black else Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}