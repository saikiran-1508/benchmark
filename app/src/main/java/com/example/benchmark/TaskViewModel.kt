package com.example.benchmark

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.benchmark.data.AppDatabase
import com.example.benchmark.data.TaskEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

// --- UI MODEL DEFINITION ---
data class Task(
    val id: Int,
    val name: String,
    val duration: String,
    val startTime: String,
    val day: String,
    val isCompleted: Boolean = false,
    val isImportant: Boolean = false,
    val soundUri: String? = null
)

/** Chronological order for the day's list ("9:00 AM" before "2:30 PM"). */
fun List<Task>.sortedByStartTime(): List<Task> {
    val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sortedBy { task ->
        runCatching { fmt.parse(task.startTime)?.time }.getOrNull() ?: Long.MAX_VALUE
    }
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val taskDao = db.taskDao()

    // StateFlow so both the UI and the voice assistant can read the
    // current schedule at any moment (tasks.value).
    val tasks: StateFlow<List<Task>> = taskDao.getAllTasks()
        .map { entities -> entities.map { it.toUiModel() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun tasksForDay(day: String): List<Task> = tasks.value.filter { it.day == day }

    // --- CONFLICT DETECTION ---
    // A task occupies [start, start + duration). No other task may begin or
    // run inside that window on the same day.
    private val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    private fun durationMinutes(duration: String): Int {
        val n = duration.filter { it.isDigit() }.toIntOrNull() ?: 60
        return if (duration.contains("m", ignoreCase = true)) n else n * 60
    }

    /** Returns the existing task that overlaps the proposed slot, or null if free. */
    fun findConflict(day: String, startTime: String, duration: String, excludeTaskId: Int = -1): Task? {
        val newStart = runCatching { timeFmt.parse(startTime)?.time }.getOrNull() ?: return null
        val newEnd = newStart + durationMinutes(duration.ifBlank { "1h" }) * 60_000L
        return tasks.value
            .filter { it.day == day && it.id != excludeTaskId && !it.isCompleted }
            .firstOrNull { existing ->
                val s = runCatching { timeFmt.parse(existing.startTime)?.time }.getOrNull()
                    ?: return@firstOrNull false
                val e = s + durationMinutes(existing.duration) * 60_000L
                newStart < e && s < newEnd
            }
    }

    fun conflictMessage(conflict: Task): String {
        val start = runCatching { timeFmt.parse(conflict.startTime)?.time }.getOrNull()
        val end = start?.let { timeFmt.format(java.util.Date(it + durationMinutes(conflict.duration) * 60_000L)) }
        return "'${conflict.name}' is already running until ${end ?: "later"}"
    }

    // --- ADD (returns false and warns if the slot is occupied) ---
    fun addTask(context: Context, name: String, duration: String, startTime: String, day: String, soundUri: String?): Boolean {
        if (name.isBlank()) return false

        val conflict = findConflict(day, startTime, duration)
        if (conflict != null) {
            Toast.makeText(context, "Slot busy: ${conflictMessage(conflict)}", Toast.LENGTH_LONG).show()
            return false
        }

        viewModelScope.launch {
            val newId = taskDao.insertTask(
                TaskEntity(
                    name = name.trim(),
                    duration = duration.ifBlank { "1h" },
                    startTime = startTime,
                    day = day,
                    soundUri = soundUri
                )
            )
            val scheduled = ReminderScheduler.schedule(context, newId.toInt(), day, startTime, name.trim(), soundUri)
            if (!scheduled && startTime.isNotBlank()) {
                // Don't fail silently: the user should know no alarm will ring
                Toast.makeText(context, "That time already passed — task saved without a reminder", Toast.LENGTH_LONG).show()
            }
        }
        return true
    }

    // --- DELETE ---
    fun deleteTask(context: Context, task: Task) {
        viewModelScope.launch {
            taskDao.deleteTaskById(task.id)
            ReminderScheduler.cancel(context, task.id)
            dismissAlarmUi(context, task.id)
        }
    }

    // --- RESCHEDULE (move to a new time and/or day) ---
    fun rescheduleTask(context: Context, task: Task, newStartTime: String, newDay: String = task.day) {
        viewModelScope.launch {
            taskDao.updateTask(task.toEntity().copy(startTime = newStartTime, day = newDay))
            ReminderScheduler.cancel(context, task.id)
            ReminderScheduler.schedule(context, task.id, newDay, newStartTime, task.name, task.soundUri)
        }
    }

    // --- COMPLETE / UNCOMPLETE ---
    fun toggleComplete(context: Context, task: Task) {
        viewModelScope.launch {
            val nowCompleted = !task.isCompleted
            taskDao.updateTask(task.toEntity().copy(isCompleted = nowCompleted))
            if (nowCompleted) {
                // A finished task doesn't need its reminder anymore
                ReminderScheduler.cancel(context, task.id)
                dismissAlarmUi(context, task.id)
            } else {
                ReminderScheduler.schedule(context, task.id, task.day, task.startTime, task.name, task.soundUri)
            }
        }
    }

    // --- STAR / UNSTAR (starred tasks appear in the Focus tab) ---
    fun toggleImportant(task: Task) {
        viewModelScope.launch {
            taskDao.updateTask(task.toEntity().copy(isImportant = !task.isImportant))
        }
    }

    // --- CLEAR EVERYTHING (cancels all reminders first) ---
    fun clearAllTasks(context: Context) {
        viewModelScope.launch {
            tasks.value.forEach { ReminderScheduler.cancel(context, it.id) }
            taskDao.clearAll()
        }
    }

    // If this task's alarm is ringing or its notification is showing, clear both
    private fun dismissAlarmUi(context: Context, taskId: Int) {
        if (AlarmSoundPlayer.ringingTaskId == taskId) AlarmSoundPlayer.stop()
        NotificationManagerCompat.from(context).cancel(taskId)
    }

    // --- MAPPERS ---
    private fun TaskEntity.toUiModel() = Task(id, name, duration, startTime, day, isCompleted, isImportant, soundUri)

    private fun Task.toEntity() = TaskEntity(
        id = id,
        name = name,
        duration = duration,
        startTime = startTime,
        day = day,
        isCompleted = isCompleted,
        isImportant = isImportant,
        soundUri = soundUri
    )
}
