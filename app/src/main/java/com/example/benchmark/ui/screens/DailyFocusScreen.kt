package com.example.benchmark.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.benchmark.TaskViewModel
import com.example.benchmark.sortedByStartTime
import com.example.benchmark.ui.components.TimelineTaskItem
import com.example.benchmark.ui.theme.BgColor
import com.example.benchmark.ui.theme.PrimaryText
import com.example.benchmark.ui.theme.SecondaryText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DailyFocusScreen(viewModel: TaskViewModel = viewModel()) {
    // 1. Get the data from the Database (only today's tasks)
    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val todayString = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
    }
    // Focus = only today's tasks the user starred as important
    val taskList = allTasks.filter { it.day == todayString && it.isImportant }.sortedByStartTime()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor) // Black Background
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "Focus",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryText,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Only your ⭐ important tasks live here.",
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Nothing in Focus yet", color = PrimaryText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap the ☆ star on any task in Home,\nor say \"mark gym as important\".",
                        color = SecondaryText, fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp) // Space for bottom bar
            ) {
                items(taskList) { task ->
                    TimelineTaskItem(
                        task = task,
                        onToggleComplete = { viewModel.toggleComplete(context, it) },
                        onToggleImportant = { viewModel.toggleImportant(it) },
                        onDelete = { viewModel.deleteTask(context, it) }
                    )
                }
            }
        }
    }
}