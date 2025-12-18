package com.example.benchmark.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.benchmark.TaskViewModel
import com.example.benchmark.ui.components.DashboardTopBar
import com.example.benchmark.ui.components.TimelineTaskItem
import com.example.benchmark.ui.theme.BgColor
import com.example.benchmark.ui.theme.ButtonColor
import com.example.benchmark.ui.theme.DarkAccent
import com.example.benchmark.ui.theme.SecondaryText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(viewModel: TaskViewModel = viewModel()) {
    // 1. DATA & CONTEXT (These were missing before)
    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 2. STATE: Selected Date & Anchor (Today)
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    val today = remember { Calendar.getInstance() }

    // 3. SCROLL STATE: Hoisted to allow auto-scrolling
    val dayListState = rememberLazyListState()

    // Formatter Helpers
    val headerFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault()) // "December 2025"
    val subtitleFormatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) // "Friday, Dec 12"
    val dbFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // "2025-12-12"

    // Filter Tasks by Selected Date
    val selectedDateString = dbFormatter.format(selectedDate.time)
    val todaysTasks = allTasks.filter { it.day == selectedDateString }

    var showAddDialog by remember { mutableStateOf(false) }

    // Calendar Picker Logic
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val newDate = Calendar.getInstance()
            newDate.set(year, month, dayOfMonth)

            // A. Highlight the new date
            selectedDate = newDate

            // B. Auto-scroll list to that date
            val diff = getDaysDifference(today, newDate)
            if (diff >= 0) {
                coroutineScope.launch {
                    dayListState.animateScrollToItem(diff)
                }
            }
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

            // --- HEADER SECTION ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    // Title: Month & Year
                    Text(
                        text = headerFormatter.format(selectedDate.time),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Subtitle: "Today" or specific date
                    Text(
                        text = if (isSameDay(selectedDate, today)) "Today" else subtitleFormatter.format(selectedDate.time),
                        color = SecondaryText,
                        fontSize = 14.sp
                    )
                }

                // Calendar Button
                IconButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.background(Color.DarkGray, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = Color.White)
                }
            }

            // --- SCROLLABLE DAY SELECTOR ---
            DaySelector(
                listState = dayListState,
                startDate = today,
                selectedDate = selectedDate,
                onDateSelected = { clickedDate -> selectedDate = clickedDate }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- TASK LIST ---
            if (todaysTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No tasks for ${if (isSameDay(selectedDate, today)) "Today" else subtitleFormatter.format(selectedDate.time)}",
                        color = SecondaryText
                    )
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

        // --- SMART ADD DIALOG (With Clock) ---
        if (showAddDialog) {
            SmartAddDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, duration, startTime ->
                    // IMPORTANT: Pass 'context' here so alarms can be set
                    viewModel.addTask(context, name, duration, startTime, selectedDateString)
                    showAddDialog = false
                }
            )
        }
    }
}

// --- COMPONENT: Infinite Day Selector ---
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
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    LazyRow(
        state = listState,
        flingBehavior = flingBehavior,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Render 365 Days (1 Year)
        items(365) { index ->
            val date = startDate.clone() as Calendar
            date.add(Calendar.DAY_OF_YEAR, index)

            val isSelected = isSameDay(date, selectedDate)

            // Logic: Red Sundays
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
                Text(
                    text = dayFormatter.format(date.time).take(1),
                    color = if (isSelected) Color.Black else dayNameColor,
                    fontSize = 12.sp,
                    fontWeight = fontWeight
                )
                Spacer(modifier = Modifier.height(4.dp))
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

// --- COMPONENT: Add Task Dialog with CLOCK PICKER ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAddDialog(onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }

    // Time Picker State
    var startTime by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(is24Hour = false)

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

    // CLOCK POPUP
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    startTime = sdf.format(cal.time)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select Start Time", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    // MAIN DIALOG
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Benchmark Task") },
        text = {
            Column {
                // Task Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Task Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Start Time (Clickable Box)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { },
                        label = { Text("Start Time") },
                        singleLine = true,
                        readOnly = true, // No typing
                        trailingIcon = { Icon(Icons.Default.DateRange, "Select Time") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false, // Keep it visually consistent
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black,
                            disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.Gray,
                            disabledTrailingIconColor = Color.Black
                        )
                    )
                    // Transparent overlay to capture click
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showTimePicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Duration
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration (e.g., 30m)") },
                    singleLine = true,
                    placeholder = {
                        if (!isDurationFocused && duration.isEmpty())
                            Text(text = aiSuggestion, color = Color.Gray)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isDurationFocused = it.isFocused }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(name, duration, startTime) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) { Text("Add", color = Color.White) }
        }
    )
}

// --- HELPER FUNCTIONS ---

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

fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun getDaysDifference(start: Calendar, end: Calendar): Int {
    val startMillis = start.timeInMillis
    val endMillis = end.timeInMillis
    val diff = endMillis - startMillis
    return (diff / (24 * 60 * 60 * 1000)).toInt()
}