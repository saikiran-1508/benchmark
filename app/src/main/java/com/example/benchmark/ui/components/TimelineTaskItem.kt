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
// --- FIX IS HERE: ---
import com.example.benchmark.Task  // Correct Import (Not .data.Task)
import com.example.benchmark.ui.theme.*

@Composable
fun TimelineTaskItem(task: Task) {
    val barColor = if (task.id % 2 == 0) LightAccent else DarkAccent // Simple logic for color variety
    val textColor = PrimaryText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(IntrinsicSize.Min)
    ) {
        // 1. Time Column
        Column(
            modifier = Modifier.width(60.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = task.startTime,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryText
            )
            Text(
                text = task.duration, // Show Duration
                fontSize = 10.sp,
                color = SecondaryText.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 2. Timeline Line & Dot
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Color.DarkGray.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
                    .align(Alignment.TopCenter)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 3. Task Card
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = task.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}