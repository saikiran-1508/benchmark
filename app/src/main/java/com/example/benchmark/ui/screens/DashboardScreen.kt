package com.example.benchmark.ui.screens

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.widget.DatePicker
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Notifications
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.benchmark.TaskViewModel
import com.example.benchmark.sortedByStartTime
import com.example.benchmark.ui.Screen
import com.example.benchmark.ui.components.DashboardTopBar
import com.example.benchmark.ui.components.GeminiManager
import com.example.benchmark.ui.components.TimelineTaskItem
import com.example.benchmark.ui.components.VoiceOverlay
import com.example.benchmark.ui.components.VoiceStatus
import com.example.benchmark.ui.theme.BgColor
import com.example.benchmark.ui.theme.ButtonColor
import com.example.benchmark.ui.theme.DarkAccent
import com.example.benchmark.ui.theme.SecondaryText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(navController: NavController, viewModel: TaskViewModel = viewModel()) {
    // 1. DATA & CONTEXT
    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // --- VOICE ASSISTANT ---
    val voiceManager = remember { GeminiManager(context, viewModel) }
    val voiceStatus by voiceManager.status.collectAsState()
    val voiceTranscript by voiceManager.transcript.collectAsState()

    DisposableEffect(Unit) {
        onDispose { voiceManager.destroy() }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) voiceManager.startConversation() }

    // 2. STATE
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    val today = remember { Calendar.getInstance() }
    val dayListState = rememberLazyListState()

    // Formatters
    val headerFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val subtitleFormatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val dbFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val selectedDateString = dbFormatter.format(selectedDate.time)
    val todaysTasks = allTasks.filter { it.day == selectedDateString }.sortedByStartTime()

    var showAddDialog by remember { mutableStateOf(false) }

    // Calendar Picker
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val newDate = Calendar.getInstance()
            newDate.set(year, month, dayOfMonth)
            selectedDate = newDate

            val diff = getDaysDifference(today, newDate)
            if (diff >= 0) {
                coroutineScope.launch { dayListState.animateScrollToItem(diff) }
            }
        },
        selectedDate.get(Calendar.YEAR),
        selectedDate.get(Calendar.MONTH),
        selectedDate.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        containerColor = BgColor,
        topBar = {
            DashboardTopBar(onProfileClick = { navController.navigate(Screen.Profile.route) })
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Voice assistant — speak to add, move, or delete tasks
                PulsingMicButton(
                    status = voiceStatus,
                    onClick = {
                        if (voiceStatus == VoiceStatus.IDLE) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                voiceManager.startConversation()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        } else {
                            voiceManager.stopConversation()
                        }
                    }
                )
                AddTaskButton(onClick = { showAddDialog = true })
            }
        }
    ) { paddingValues ->

        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            // --- HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = headerFormatter.format(selectedDate.time),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isSameDay(selectedDate, today)) "Today" else subtitleFormatter.format(selectedDate.time),
                        color = SecondaryText,
                        fontSize = 14.sp
                    )
                }
                IconButton(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.background(Color.DarkGray, RoundedCornerShape(8.dp))
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = Color.White)
                }
            }

            // --- DAY SELECTOR ---
            DaySelector(
                listState = dayListState,
                startDate = today,
                selectedDate = selectedDate,
                onDateSelected = { clickedDate -> selectedDate = clickedDate }
            )

            // --- AI HINT LINE (rotates through example voice commands) ---
            AiHintLine()

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

        // --- VOICE OVERLAY ---
        VoiceOverlay(status = voiceStatus, transcript = voiceTranscript)

        // --- SMART ADD DIALOG (Updated) ---
        if (showAddDialog) {
            SmartAddDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { name, duration, startTime, soundUri ->
                    // Pass context and soundUri to ViewModel
                    viewModel.addTask(context, name, duration, startTime, selectedDateString, soundUri)
                    showAddDialog = false
                }
            )
        }
    }
}

// --- UPDATED DIALOG WITH RINGTONE PICKER ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAddDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String?) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }

    // Reminder State
    var hasReminder by remember { mutableStateOf(true) }
    var selectedSoundUri by remember { mutableStateOf<String?>(null) }
    var soundName by remember { mutableStateOf("Default Notification Sound") }

    // Ringtone Picker Logic
    val ringtoneLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                selectedSoundUri = uri.toString()
                val ringtone = RingtoneManager.getRingtone(context, uri)
                soundName = ringtone.getTitle(context) ?: "Custom Sound"
            }
        }
    }

    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(is24Hour = false)
    var aiSuggestion by remember { mutableStateOf("") }

    LaunchedEffect(name) {
        if (name.length > 3) {
            aiSuggestion = "Thinking..."
            delay(1000)
            aiSuggestion = "AI Est: 45m"
        } else {
            aiSuggestion = ""
        }
    }

    // Clock Popup
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
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } },
            text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { TimePicker(state = timePickerState) } }
        )
    }

    // Main Dialog
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Benchmark Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Task Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = startTime, onValueChange = { },
                        label = { Text("Start Time") }, singleLine = true, readOnly = true,
                        trailingIcon = { Icon(Icons.Default.DateRange, null) },
                        modifier = Modifier.fillMaxWidth(), enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Black, disabledBorderColor = Color.Gray,
                            disabledLabelColor = Color.Gray, disabledTrailingIconColor = Color.Black
                        )
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showTimePicker = true })
                }
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = duration, onValueChange = { duration = it },
                    label = { Text("Duration (e.g., 30m)") }, singleLine = true,
                    placeholder = { if (duration.isEmpty()) Text(text = aiSuggestion, color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick duration presets — one tap instead of typing
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    listOf("15m", "30m", "1h", "2h").forEach { preset ->
                        FilterChip(
                            selected = duration == preset,
                            onClick = { duration = preset },
                            label = { Text(preset) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Reminder Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Remind Me", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Switch(checked = hasReminder, onCheckedChange = { hasReminder = it })
                }

                // Sound Picker (Visible only if Reminder is ON)
                if (hasReminder) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f))
                            .clickable {
                                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                    // TYPE_ALL = alarms + ringtones + notification sounds
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound")
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                    putExtra(
                                        RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                                        android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                                    )
                                }
                                ringtoneLauncher.launch(intent)
                            }
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.DarkGray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Notification Sound", fontSize = 12.sp, color = Color.Gray)
                            Text(soundName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSound = if (hasReminder) selectedSoundUri else null
                    onAdd(name, duration, startTime, finalSound)
                },
                // No half-filled tasks: need at least a name and a start time
                enabled = name.isNotBlank() && startTime.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) { Text("Add", color = Color.White) }
        }
    )
}

// --- AI HINT LINE ---
// One line under the calendar that teaches users what the mic can do,
// cycling through real example commands.
@Composable
fun AiHintLine() {
    val hints = remember {
        listOf(
            "\"Add gym at 6 PM for 1 hour\"",
            "\"Move gym to 7:30\"",
            "\"Delete gym\"",
            "\"Mark gym as done\"",
            "\"What's my schedule?\""
        )
    }
    var hintIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            hintIndex = (hintIndex + 1) % hints.size
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text("🎙️", fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Tap the mic and say: ${hints[hintIndex]}",
            color = SecondaryText,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

// --- HELPER COMPONENT (Day Selector) ---
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(365) { index ->
            val date = startDate.clone() as Calendar
            date.add(Calendar.DAY_OF_YEAR, index)
            val isSelected = isSameDay(date, selectedDate)
            val isSunday = date.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            val dayNameColor = if (isSunday) Color(0xFFFF5252) else SecondaryText

            val bgColor = if (isSelected) DarkAccent else Color.Transparent
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
                Text(text = dayFormatter.format(date.time).take(1), color = if (isSelected) Color.Black else dayNameColor, fontSize = 12.sp, fontWeight = fontWeight)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = dateFormatter.format(date.time), color = if (isSelected) Color.Black else Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
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