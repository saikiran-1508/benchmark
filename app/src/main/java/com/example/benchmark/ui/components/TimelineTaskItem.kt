package com.example.benchmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.benchmark.Task
import com.example.benchmark.ui.theme.*

@Composable
fun TimelineTaskItem(
    task: Task,
    onToggleComplete: (Task) -> Unit = {},
    onToggleImportant: (Task) -> Unit = {},
    onDelete: (Task) -> Unit = {}
) {
    val barColor = if (task.id % 2 == 0) LightAccent else DarkAccent
    val textColor = if (task.isCompleted) SecondaryText else PrimaryText

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
                text = task.duration,
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

        // 3. Task Card with complete + delete actions
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onToggleComplete(task) }) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = if (task.isCompleted) "Mark incomplete" else "Mark complete",
                        tint = if (task.isCompleted) Color(0xFF4CAF50) else SecondaryText
                    )
                }
                Text(
                    text = task.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    modifier = Modifier.weight(1f)
                )
                // Star = important: the task joins the Important tab
                IconButton(onClick = { onToggleImportant(task) }) {
                    Icon(
                        imageVector = if (task.isImportant) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (task.isImportant) "Remove from Important" else "Mark Important",
                        tint = if (task.isImportant) Color(0xFFFFC107) else SecondaryText
                    )
                }
                IconButton(onClick = { onDelete(task) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = SecondaryText
                    )
                }
            }
        }
    }
}
