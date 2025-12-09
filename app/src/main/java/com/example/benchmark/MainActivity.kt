package com.example.benchmark

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.example.benchmark.ui.components.*
import com.example.benchmark.ui.theme.BgColor
import com.example.benchmark.ui.theme.ButtonColor
import com.example.benchmark.ui.theme.SecondaryText
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DashboardScreen()
        }
    }
}

@Composable
fun DashboardScreen(viewModel: TaskViewModel = viewModel()) {
    val taskList by viewModel.tasks.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BgColor,
        topBar = { DashboardTopBar() },
        floatingActionButton = {
            // Updated Button: Simpler Text
            AddTaskButton(onClick = { showAddDialog = true })
        }
    ) { paddingValues ->

        Column(modifier = Modifier.padding(paddingValues)) {

            DaySelector()

            Spacer(modifier = Modifier.height(16.dp))

            if (taskList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tap + to add a benchmark task", color = SecondaryText)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(taskList) { task ->
                        TimelineTaskItem(task)
                    }
                }
            }
        }

        if (showAddDialog) {
            SmartAddDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, time ->
                    viewModel.addTask(name, time)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun AddTaskButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = ButtonColor,
        contentColor = Color.Black,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(140.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Add Task", // Renamed as requested
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Add, contentDescription = "Add Task")
        }
    }
}

@Composable
fun SmartAddDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }

    // Logic for AI Suggestion
    var aiSuggestion by remember { mutableStateOf("") }
    var isTimeFieldFocused by remember { mutableStateOf(false) }

    // This "Simulates" the AI thinking when you stop typing
    LaunchedEffect(name) {
        if (name.length > 3) {
            aiSuggestion = "Thinking..."
            delay(1000) // Fake Network Delay
            aiSuggestion = "AI Est: 45m" // This will be real Gemini data soon
        } else {
            aiSuggestion = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Benchmark Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Time") },
                    singleLine = true,
                    // The Magic: Show Placeholder ONLY if not focused and text is empty
                    placeholder = {
                        if (!isTimeFieldFocused && time.isEmpty()) {
                            Text(text = aiSuggestion, color = Color.Gray)
                        }
                    },
                    modifier = Modifier
                        .onFocusChanged { focusState ->
                            isTimeFieldFocused = focusState.isFocused
                        }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // If user didn't type time, but AI suggested one, use AI time?
                    // For now, let's require manual input or explicit acceptance
                    onAdd(name, time)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Add", color = Color.Black)
            }
        }
    )
}