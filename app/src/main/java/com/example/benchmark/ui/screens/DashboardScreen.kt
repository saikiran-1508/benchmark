package com.example.benchmark.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.benchmark.TaskViewModel
import com.example.benchmark.ui.components.* import com.example.benchmark.ui.theme.BgColor
import com.example.benchmark.ui.theme.ButtonColor
import com.example.benchmark.ui.theme.SecondaryText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: TaskViewModel = viewModel()) {
    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val context = LocalContext.current

    // 1. STATE:
    // 'selectedDate' is what filters the list (The Highlighted Day)
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    // 'visibleStartDate' is the first day shown on the slide bar (The Anchor)
    // Initially, this is Real Today.
    var visibleStartDate by remember { mutableStateOf(Calendar.getInstance()) }

    val displayFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val dbFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Filter tasks based on the highlight
    val selectedDateString = dbFormatter.format(selectedDate.time)
    val todaysTasks = allTasks.filter { it.day == selectedDateString }

    var showAddDialog by remember { mutableStateOf(false) }

    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val newDate = Calendar.getInstance()
            newDate.set(year, month, dayOfMonth)

            // When using Calendar Picker, we update BOTH:
            // 1. Highlight the new date
            selectedDate = newDate
            // 2. Move the slide bar so this date is the FIRST one shown
            visibleStartDate = newDate.clone() as Calendar
        },
        selectedDate.get(Calendar.YEAR),
        selectedDate.get(Calendar.MONTH),
        selectedDate.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        containerColor = BgColor,
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

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayFormatter.format(selectedDate.time),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.background(Color.DarkGray, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = Color.White)
                }
            }

            // The Sliding Bar
            // We pass 'visibleStartDate' as the anchor, and 'selectedDate' as the highlight
            DaySelector(
                startDate = visibleStartDate,
                selectedDate = selectedDate,
                onDateSelected = { clickedDate ->
                    // When clicking the bar, we ONLY move the highlight, not the bar itself
                    selectedDate = clickedDate
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (todaysTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No tasks for this day.", color = SecondaryText)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(todaysTasks) { task ->
                        TimelineTaskItem(task)
                    }
                }
            }
        }

        if (showAddDialog) {
            SmartAddDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, duration, startTime ->
                    viewModel.addTask(name, duration, startTime, selectedDateString)
                    showAddDialog = false
                }
            )
        }
    }
}

// ... AddTaskButton and SmartAddDialog remain unchanged ...
// (Ensure they are present in your file)
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
    var startTime by remember { mutableStateOf("") }
    var aiSuggestion by remember { mutableStateOf("") }
    var isDurationFocused by remember { mutableStateOf(false) }

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
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Task Name") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Start Time") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = duration, onValueChange = { duration = it }, label = { Text("Duration") }, singleLine = true,
                    placeholder = { if (!isDurationFocused && duration.isEmpty()) Text(text = aiSuggestion, color = Color.Gray) },
                    modifier = Modifier.onFocusChanged { isDurationFocused = it.isFocused }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, duration, startTime) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) { Text("Add", color = Color.Black) }
        }
    )
}