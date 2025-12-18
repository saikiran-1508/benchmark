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

// --- 1. THIS WAS MISSING OR INCORRECT ---
// This defines what a "Task" looks like for your UI.
// Make sure this block is here!
data class Task(
    val id: Int,
    val name: String,
    val duration: String,  // We use 'duration' now, not 'estimatedTime'
    val startTime: String,
    val day: String
)
// ----------------------------------------

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val taskDao = db.taskDao()

    // 2. Convert Database Entity -> UI Model
    // Now that 'Task' is defined above, this map function will work.
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

    // 3. Add Task (Saving as TaskEntity)
    fun addTask(context: Context, name: String, duration: String, startTime: String, day: String) {
        viewModelScope.launch {
            // Save to Database
            taskDao.insertTask(
                TaskEntity(
                    name = name,
                    duration = duration,
                    startTime = startTime,
                    day = day
                )
            )

            // Set the Alarm
            scheduleReminder(context, day, startTime, name)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleReminder(context: Context, day: String, startTime: String, taskName: String) {
        try {
            val format = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault())
            val dateString = "$day $startTime"
            val date = format.parse(dateString)
            val triggerTime = date?.time ?: return

            if (triggerTime < System.currentTimeMillis()) return

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("TASK_NAME", taskName)
            }

            val requestCode = (triggerTime / 1000).toInt()

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

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