package com.example.benchmark

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.benchmark.data.AppDatabase
import com.example.benchmark.data.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// --- UI MODEL DEFINITION ---
data class Task(
    val id: Int,
    val name: String,
    val duration: String,
    val startTime: String,
    val day: String
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val taskDao = db.taskDao()

    // Convert Database Entity -> UI Model
    val tasks: Flow<List<Task>> = taskDao.getAllTasks().map { entities ->
        entities.map { entity ->
            Task(
                id = entity.id,
                name = entity.name,
                duration = entity.duration,
                startTime = entity.startTime,
                day = entity.day
            )
        }
    }

    // --- ADD TASK (Now accepts soundUri) ---
    fun addTask(context: Context, name: String, duration: String, startTime: String, day: String, soundUri: String?) {
        viewModelScope.launch {
            // 1. Save to Database
            taskDao.insertTask(
                TaskEntity(
                    name = name,
                    duration = duration,
                    startTime = startTime,
                    day = day
                )
            )

            // 2. Set the Alarm with the custom sound
            scheduleReminder(context, day, startTime, name, soundUri)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleReminder(context: Context, day: String, startTime: String, taskName: String, soundUri: String?) {
        try {
            // Parse the time string (e.g. "2025-12-25 10:30 AM")
            val format = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault())
            val dateString = "$day $startTime"
            val date = format.parse(dateString)
            val triggerTime = date?.time ?: return

            // Don't schedule past events
            if (triggerTime < System.currentTimeMillis()) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Create the Intent for the Receiver
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("TASK_NAME", taskName)
                // Pass the sound URI if it exists
                if (soundUri != null) {
                    putExtra("SOUND_URI", soundUri)
                }
            }

            // Generate a unique ID for this alarm based on time
            val requestCode = (triggerTime / 1000).toInt()

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Set the Exact Alarm
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )

        } catch (e: Exception) {
            Log.e("Benchmark", "Failed to schedule alarm", e)
        }
    }
}