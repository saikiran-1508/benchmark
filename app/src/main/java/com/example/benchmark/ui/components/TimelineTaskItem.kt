package com.example.benchmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.benchmark.data.Task
import com.example.benchmark.ui.theme.*

@Composable
fun TimelineTaskItem(task: Task) {
    // Determine bar color based on status (for future use)
    // For now, active tasks are Black (DarkAccent), completed could be LightAccent
    val barColor = if (task.isCompleted) LightAccent else DarkAccent
    val textColor = if (task.isCompleted) SecondaryText else PrimaryText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(IntrinsicSize.Min)
    ) {
        // 1. Time Column
        Column(
            modifier = Modifier.width(50.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "10 AM", // Placeholder
                fontSize = 12.sp,
                color = SecondaryText,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. The Vertical Line (UPDATED to use DividerColor)
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(DividerColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 3. The Task Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardColor),
            // Added a subtle border for contrast in B&W theme
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Flatter look for B&W
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                // Indicator Bar (UPDATED to use dynamic grayscale color)
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(barColor)
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = task.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Est: ${task.estimatedTime}",
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }
            }
        }
    }
}