package com.example.benchmark.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.benchmark.TaskViewModel
import com.example.benchmark.ui.components.* // Imports DaySelector, DashboardTopBar, TimelineTaskItem
import com.example.benchmark.ui.theme.BgColor
import com.example.benchmark.ui.theme.ButtonColor
import com.example.benchmark.ui.theme.SecondaryText
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(viewModel: TaskViewModel = viewModel()) {
    // 1. Collect all tasks from the database
    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())

    // 2. State: Tracks the currently selected day (Full Name). Defaults to Sunday.
    var currentDay by remember { mutableStateOf("Sunday") }

    // 3. Logic: Filter the list to show ONLY tasks for the selected day
    val todaysTasks = allTasks.filter { it.day == currentDay }

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgColor, // Black Background
        topBar = { DashboardTopBar() },
        floatingActionButton = {
            AddTaskButton(onClick = { showAddDialog = true })
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

            // 4. The Day Selector (Passes the full name back on click)
            DaySelector(
                selectedDay = currentDay,
                onDaySelected = { newDay -> currentDay = newDay }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 5. The List
            if (todaysTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Shows "No tasks for Wednesday yet"
                    Text("No tasks for $currentDay yet.", color = SecondaryText)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(todaysTasks) { task ->
                        TimelineTaskItem(task)
                    }
                }
            }
        }

        // 6. The Dialog
        if (showAddDialog) {
            SmartAddDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, duration, startTime ->
                    // Important: We add the task specifically for the 'currentDay'
                    viewModel.addTask(name, duration, startTime, currentDay)
                    showAddDialog = false
                }
            )
        }
    }
}

// --- Local Components for Dashboard ---

@Composable
fun AddTaskButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = ButtonColor, // White
        contentColor = Color.Black,   // Black Text
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(140.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Add Task", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }
}

@Composable
fun SmartAddDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") } // Start Time State

    var aiSuggestion by remember { mutableStateOf("") }
    var isDurationFocused by remember { mutableStateOf(false) }

    // AI Simulation
    LaunchedEffect(name) {
        if (name.length > 3) {
            aiSuggestion = "Thinking..."
            delay(1000)
            aiSuggestion = "AI Est: 45m"
        } else {
            aiSuggestion = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Benchmark Task") },
        text = {
            Column {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Start Time (New)
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    label = { Text("Start Time (e.g. 10:00 AM)") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Duration (Smart)
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration") },
                    singleLine = true,
                    placeholder = {
                        if (!isDurationFocused && duration.isEmpty()) {
                            Text(text = aiSuggestion, color = Color.Gray)
                        }
                    },
                    modifier = Modifier.onFocusChanged { isDurationFocused = it.isFocused }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, duration, startTime) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Add", color = Color.Black)
            }
        }
    )
}