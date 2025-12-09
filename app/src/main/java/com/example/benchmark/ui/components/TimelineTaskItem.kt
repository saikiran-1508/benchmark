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
    val barColor = if (task.isCompleted) LightAccent else DarkAccent
    val textColor = if (task.isCompleted) SecondaryText else PrimaryText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(IntrinsicSize.Min)
    ) {
        // 1. Time Column (Now shows REAL Start Time)
        Column(
            modifier = Modifier.width(60.dp), // Slightly wider for "10:00 AM"
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = task.startTime, // <--- CHANGED HERE
                fontSize = 12.sp,
                color = SecondaryText,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. Vertical Line
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(DividerColor)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 3. Task Card
        Card(
            colors = CardDefaults.cardColors(containerColor = CardColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
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
                        text = "Duration: ${task.estimatedTime}",
                        fontSize = 12.sp,
                        color = SecondaryText
                    )
                }
            }
        }
    }
}