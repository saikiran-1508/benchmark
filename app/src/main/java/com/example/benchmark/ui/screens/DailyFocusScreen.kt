package com.example.benchmark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.benchmark.TaskViewModel
import com.example.benchmark.ui.components.TimelineTaskItem
import com.example.benchmark.ui.theme.BgColor
import com.example.benchmark.ui.theme.PrimaryText
import com.example.benchmark.ui.theme.SecondaryText

@Composable
fun DailyFocusScreen(viewModel: TaskViewModel = viewModel()) {
    // 1. Get the data from the Database
    val taskList by viewModel.tasks.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor) // Black Background
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Today's Tasks",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Focus on what matters now.",
            fontSize = 14.sp,
            color = SecondaryText,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // The List
        if (taskList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No tasks for today yet.", color = SecondaryText)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp) // Space for bottom bar
            ) {
                items(taskList) { task ->
                    TimelineTaskItem(task)
                }
            }
        }
    }
}