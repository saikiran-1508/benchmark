package com.example.benchmark.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.benchmark.Task
import com.example.benchmark.TaskViewModel
import com.example.benchmark.ui.components.VoiceManager
import com.example.benchmark.ui.components.VoiceStatus
import com.example.benchmark.ui.theme.BgColor
import com.example.benchmark.ui.theme.DarkAccent
import com.example.benchmark.ui.theme.SecondaryText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TimetableScreen(viewModel: TaskViewModel = viewModel()) {
    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // --- VOICE MANAGER ---
    val voiceManager = remember { VoiceManager(context, viewModel) }
    val voiceStatus by voiceManager.status.collectAsState()

    // --- PERMISSION ---
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) voiceManager.startConversation()
        }
    )

    // --- STATE ---
    var wakeUpHour by remember { mutableIntStateOf(6) }
    var sleepHour by remember { mutableIntStateOf(22) }
    var showSettings by remember { mutableStateOf(false) }

    // Copy/Paste State
    var copiedTask by remember { mutableStateOf<Task?>(null) }

    // Dialog State
    var showAddDialog by remember { mutableStateOf(false) }
    var clickedHour by remember { mutableIntStateOf(0) }

    val hourHeight = 80.dp
    val startPadding = 80.dp // Wider to allow easy pressing on time label
    val dbFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayString = dbFormatter.format(Calendar.getInstance().time)
    val todaysTasks = allTasks.filter { it.day == todayString }

    Scaffold(
        containerColor = BgColor,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (copiedTask == null) {
                FloatingActionButton(
                    onClick = {
                        if (voiceStatus == VoiceStatus.IDLE) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                voiceManager.startConversation()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        } else {
                            voiceManager.stopConversation()
                        }
                    },
                    containerColor = if(voiceStatus == VoiceStatus.IDLE) Color.Black else Color.Red,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(70.dp)
                ) {
                    Icon(imageVector = if (voiceStatus == VoiceStatus.IDLE) Icons.Default.Mic else Icons.Default.Stop, contentDescription = "Voice")
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { copiedTask = null },
                    containerColor = Color.Red, contentColor = Color.White
                ) { Text("Cancel Paste") }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {

            Column(modifier = Modifier.fillMaxSize()) {
                // HEADER
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Daily Grid", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (copiedTask != null) "Tap a slot to paste '${copiedTask?.name}'" else "Long press Time (Left) to Copy",
                            color = if (copiedTask != null) Color.Green else SecondaryText,
                            fontSize = 14.sp
                        )
                    }
                    Button(onClick = { showSettings = true }, colors = ButtonDefaults.buttonColors(containerColor = DarkAccent)) { Text("Settings", color = Color.Black) }
                }

                // GRID
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    for (hour in wakeUpHour..sleepHour) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(hourHeight)
                        ) {
                            // --- 1. TIME SECTION (LEFT) - COPY TRIGGER ---
                            TimeLabelSection(
                                hour = hour,
                                width = startPadding,
                                onLongClick = {
                                    // Logic: Find the task at this hour to copy it
                                    val taskAtHour = todaysTasks.find { getHourFromTime(it.startTime) == hour }
                                    if (taskAtHour != null) {
                                        copiedTask = taskAtHour
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Copied '${taskAtHour.name}'") }
                                    } else {
                                        coroutineScope.launch { snackbarHostState.showSnackbar("No task at $hour:00 to copy") }
                                    }
                                }
                            )

                            // --- 2. GRID AREA (RIGHT) - CLICK TO PASTE/ADD ---
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = startPadding)
                                    .clickable {
                                        if (copiedTask != null) {
                                            // Paste Logic
                                            val newTime = String.format("%d:00 %s", if(hour > 12) hour-12 else hour, if(hour>=12)"PM" else "AM")
                                            viewModel.addTask(context, copiedTask!!.name, copiedTask!!.duration, newTime, todayString, null)
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Pasted at $newTime") }
                                        } else {
                                            // Add Logic
                                            clickedHour = hour
                                            showAddDialog = true
                                        }
                                    }
                            ) {
                                Divider(color = Color.DarkGray.copy(alpha = 0.3f), modifier = Modifier.align(Alignment.TopStart))

                                // Render Tasks
                                todaysTasks.forEach { task ->
                                    if (getHourFromTime(task.startTime) == hour) {
                                        val offset = (getMinutesFromTime(task.startTime).toFloat() / 60f) * 80
                                        val height = parseDurationToHeight(task.duration, hourHeight)
                                        TaskBlock(task, offset.dp, height)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // AI OVERLAY
            AnimatedVisibility(
                visible = voiceStatus != VoiceStatus.IDLE && voiceStatus != VoiceStatus.ERROR,
                enter = fadeIn(), exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                    Text(
                        text = when(voiceStatus) {
                            VoiceStatus.SPEAKING -> "AI Speaking..."
                            VoiceStatus.LISTENING -> "Listening..."
                            VoiceStatus.PROCESSING -> "Thinking..."
                            else -> ""
                        },
                        color = Color.Cyan, fontSize = 28.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // DIALOGS
        if (showAddDialog) {
            TimetableAddDialog(
                initialTime = String.format("%d:00 %s", if(clickedHour > 12) clickedHour - 12 else clickedHour, if(clickedHour >= 12) "PM" else "AM"),
                onDismiss = { showAddDialog = false },
                onAdd = { name, duration, startTime ->
                    viewModel.addTask(context, name, duration, startTime, todayString, null)
                    showAddDialog = false
                }
            )
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Grid Settings") },
                text = {
                    Column {
                        Text("Wake Up: $wakeUpHour:00")
                        Slider(value = wakeUpHour.toFloat(), onValueChange = { wakeUpHour = it.toInt() }, valueRange = 4f..12f, steps = 1)
                        Text("Sleep: $sleepHour:00")
                        Slider(value = sleepHour.toFloat(), onValueChange = { sleepHour = it.toInt() }, valueRange = 16f..23f, steps = 1)
                    }
                },
                confirmButton = { Button(onClick = { showSettings = false }) { Text("Save") } }
            )
        }
    }
}

// --- COMPONENT: TIME LABEL (With Copy) ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimeLabelSection(hour: Int, width: Dp, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick // TRIGGER COPY
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = String.format("%02d:00", hour),
            color = SecondaryText,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

// --- COMPONENT: TASK BLOCK (Merged Time) ---
@Composable
fun TaskBlock(task: Task, topOffset: Dp, height: Dp) {
    // Calculate End Time logic
    val endLabel = calculateEndTime(task.startTime, task.duration)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkAccent.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .padding(start = 8.dp, end = 16.dp)
            .offset(y = topOffset)
            .fillMaxWidth()
            .height(height)
            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.Center) {
            Text(task.name, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            // SHOW RANGE: e.g. "3:00 PM - 5:00 PM"
            Text("${task.startTime} - $endLabel", color = Color.DarkGray, fontSize = 11.sp)
        }
    }
}

// --- HELPERS ---
fun calculateEndTime(start: String, duration: String): String {
    return try {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val date = sdf.parse(start) ?: return ""
        val cal = Calendar.getInstance()
        cal.time = date
        val numeric = duration.filter { it.isDigit() }.toIntOrNull() ?: 0
        if (duration.contains("m")) cal.add(Calendar.MINUTE, numeric) else cal.add(Calendar.HOUR, numeric)
        sdf.format(cal.time)
    } catch (e: Exception) { "" }
}

fun getHourFromTime(timeStr: String): Int {
    return try {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(timeStr) ?: Date()
        cal.get(Calendar.HOUR_OF_DAY)
    } catch (e: Exception) { 0 }
}

fun getMinutesFromTime(timeStr: String): Int {
    return try {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(timeStr) ?: Date()
        cal.get(Calendar.MINUTE)
    } catch (e: Exception) { 0 }
}

fun parseDurationToHeight(duration: String, hourHeight: Dp): Dp {
    val numeric = duration.filter { it.isDigit() }.toIntOrNull() ?: 60
    return if (duration.contains("m")) {
        (numeric.toFloat() / 60f * hourHeight.value).dp
    } else {
        (numeric.toFloat() * hourHeight.value).dp
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableAddDialog(initialTime: String, onDismiss: () -> Unit, onAdd: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf(initialTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Task Name") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = startTime, onValueChange = { startTime = it }, label = { Text("Time") }, singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Duration (e.g. 1h)") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onAdd(name, duration, startTime) }) { Text("Add") } }
    )
}