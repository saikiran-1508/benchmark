package com.example.benchmark.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.benchmark.AuthViewModel
import com.example.benchmark.Task
import com.example.benchmark.TaskViewModel
import com.example.benchmark.ui.theme.BgColor
import com.example.benchmark.ui.theme.DarkAccent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ProfileScreen(
    viewModel: TaskViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    onSignOut: () -> Unit = {}
) {
    val allTasks by viewModel.tasks.collectAsState(initial = emptyList())
    val context = LocalContext.current

    // --- ANALYTICS ---
    val totalTasks = allTasks.size
    val deepWorkCount = allTasks.count { it.duration.contains("h") }
    val shallowWorkCount = totalTasks - deepWorkCount
    val deepWorkPercent = if (totalTasks > 0) (deepWorkCount.toFloat() / totalTasks) * 100f else 0f
    val shallowPercent = if (totalTasks > 0) (shallowWorkCount.toFloat() / totalTasks) * 100f else 0f
    val streak = remember(allTasks) { calculateStreak(allTasks) }

    // --- ACCOUNT INFO ---
    val email = authViewModel.userEmail ?: "Not signed in"
    val displayName = authViewModel.userName ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }

    // --- DIALOG STATE ---
    var showVoiceGuide by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. HEADER — the real signed-in account
        ProfileHeader(name = displayName, email = email)

        Spacer(modifier = Modifier.height(24.dp))

        // 2. STREAK — computed from actually completed tasks
        StreakCard(streakDays = streak)

        Spacer(modifier = Modifier.height(24.dp))

        // 3. PRODUCTIVITY PIE CHART
        Text(
            text = "Productivity Breakdown",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (totalTasks == 0) {
            Text(
                "Add a few tasks and your chart appears here.",
                color = Color.Gray, fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AnimatedPieChart(
                    data = listOf(deepWorkPercent, shallowPercent),
                    colors = listOf(Color.Cyan, Color.Magenta),
                    modifier = Modifier.size(150.dp)
                )
                Column {
                    LegendItem(color = Color.Cyan, text = "Deep Work ($deepWorkCount)")
                    Spacer(modifier = Modifier.height(8.dp))
                    LegendItem(color = Color.Magenta, text = "Quick Tasks ($shallowWorkCount)")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 4. SETTINGS — every row actually does something
        Text(
            text = "Settings & Help",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsOption(
            icon = Icons.Default.Notifications,
            title = "Notification Settings",
            subtitle = "Sound, vibration, and banner control",
            onClick = { openNotificationSettings(context) }
        )
        SettingsOption(
            icon = Icons.Default.Alarm,
            title = "Alarms & Reminders",
            subtitle = "Allow exact-time reminders",
            onClick = { openAlarmSettings(context) }
        )
        SettingsOption(
            icon = Icons.Default.Mic,
            title = "Voice Assistant Guide",
            subtitle = "See what you can say to the AI",
            onClick = { showVoiceGuide = true }
        )
        SettingsOption(
            icon = Icons.Default.Share,
            title = "Export Data (CSV)",
            subtitle = "Share your tasks as a spreadsheet",
            onClick = { exportTasksAsCsv(context, allTasks) }
        )
        SettingsOption(
            icon = Icons.Default.Delete,
            title = "Clear All Tasks",
            subtitle = "Deletes every task and its reminders",
            isDestructive = true,
            onClick = { showClearConfirm = true }
        )
        SettingsOption(
            icon = Icons.AutoMirrored.Filled.Logout,
            title = "Sign Out",
            subtitle = email,
            isDestructive = true,
            onClick = { showSignOutConfirm = true }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // --- VOICE GUIDE DIALOG ---
    if (showVoiceGuide) {
        VoiceGuideDialog(onDismiss = { showVoiceGuide = false })
    }

    // --- CLEAR DATA CONFIRMATION ---
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all tasks?") },
            text = { Text("This deletes all $totalTasks tasks and cancels their reminders. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllTasks(context)
                        showClearConfirm = false
                        Toast.makeText(context, "All tasks cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Delete Everything", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    // --- SIGN OUT CONFIRMATION ---
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text("Sign out?") },
            text = { Text("Your tasks stay on this device. You can sign back in anytime.") },
            confirmButton = {
                Button(onClick = {
                    showSignOutConfirm = false
                    authViewModel.signOut()
                    onSignOut()
                }) { Text("Sign Out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// --- HELPERS ---

/** Consecutive days (ending today or yesterday) that have at least one completed task. */
private fun calculateStreak(tasks: List<Task>): Int {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val completedDays = tasks.filter { it.isCompleted }.map { it.day }.toSet()
    if (completedDays.isEmpty()) return 0

    val cal = Calendar.getInstance()
    // Today without a completion yet shouldn't break yesterday's streak
    if (!completedDays.contains(fmt.format(cal.time))) cal.add(Calendar.DAY_OF_YEAR, -1)

    var streak = 0
    while (completedDays.contains(fmt.format(cal.time))) {
        streak++
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    return streak
}

private fun openNotificationSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(android.net.Uri.parse("package:${context.packageName}"))
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Couldn't open settings", Toast.LENGTH_SHORT).show()
    }
}

private fun openAlarmSettings(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        } else {
            Toast.makeText(context, "Exact alarms are already allowed on this device", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Couldn't open settings", Toast.LENGTH_SHORT).show()
    }
}

private fun exportTasksAsCsv(context: Context, tasks: List<Task>) {
    if (tasks.isEmpty()) {
        Toast.makeText(context, "No tasks to export yet", Toast.LENGTH_SHORT).show()
        return
    }
    val csv = buildString {
        appendLine("Name,Duration,Start Time,Day,Completed")
        tasks.forEach {
            appendLine("\"${it.name}\",${it.duration},${it.startTime},${it.day},${if (it.isCompleted) "Yes" else "No"}")
        }
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Benchmark Tasks Export")
        putExtra(Intent.EXTRA_TEXT, csv)
    }
    context.startActivity(Intent.createChooser(intent, "Export tasks via"))
}

// --- COMPONENTS ---

@Composable
private fun VoiceGuideDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎙️ Talk to your timetable") },
        text = {
            Column {
                Text("Tap the mic button, then say things like:", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(12.dp))
                VoiceExample("“Add gym at 6 PM for 1 hour”", "Creates the task + sets the alarm")
                VoiceExample("“Move gym to 7:30”", "Reschedules it")
                VoiceExample("“Delete gym”", "Removes it")
                VoiceExample("“Mark gym as done”", "Completes it, keeps your streak")
                VoiceExample("“Mark gym as important”", "Stars it into your Focus tab")
                VoiceExample("“What's my schedule?”", "Reads today aloud")
                VoiceExample("“Stop”", "Ends the conversation")
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "The assistant keeps listening after each command, so you can chain them.",
                    fontSize = 12.sp, color = Color.Gray
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Got it") } }
    )
}

@Composable
private fun VoiceExample(command: String, effect: String) {
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(command, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(effect, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun ProfileHeader(name: String, email: String) {
    val initials = name.split(" ", ".", "_").filter { it.isNotBlank() }
        .take(2).joinToString("") { it.first().uppercase() }
        .ifBlank { "?" }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.DarkGray)
                .border(2.dp, Color.Cyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(name, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(email, color = Color.Cyan, fontSize = 14.sp)
    }
}

@Composable
fun StreakCard(streakDays: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "Streak",
                tint = if (streakDays > 0) Color(0xFFFF5722) else Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Current Streak", color = Color.Gray, fontSize = 14.sp)
                Text(
                    if (streakDays > 0) "$streakDays Day${if (streakDays > 1) "s" else ""}"
                    else "Complete a task to start!",
                    color = Color.White, fontSize = if (streakDays > 0) 22.sp else 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// 📊 CUSTOM NATIVE PIE CHART (Uses Canvas)
@Composable
fun AnimatedPieChart(
    data: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing),
        label = "pieAnimation"
    )

    Canvas(modifier = modifier) {
        val total = data.sum().takeIf { it > 0f } ?: return@Canvas
        var startAngle = -90f

        data.forEachIndexed { index, value ->
            val sweepAngle = (value / total) * 360f * animatedProgress
            drawArc(
                color = colors.getOrElse(index) { Color.Gray },
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 40f, cap = StrokeCap.Butt),
                size = Size(size.width, size.height),
                topLeft = Offset(0f, 0f)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
fun SettingsOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = if (isDestructive) Color.Red else Color.Gray)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = if (isDestructive) Color.Red else Color.White, fontSize = 16.sp)
                if (subtitle != null) {
                    Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
    Divider(color = Color.DarkGray.copy(alpha = 0.3f))
}
