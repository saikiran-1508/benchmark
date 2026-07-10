package com.example.benchmark

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Single source of truth for scheduling/cancelling task reminders.
 * The task's database id is used as the PendingIntent request code,
 * so a reminder can always be cancelled or replaced later.
 */
object ReminderScheduler {

    const val EXTRA_TASK_NAME = "TASK_NAME"
    const val EXTRA_SOUND_URI = "SOUND_URI"

    /** Returns true if the reminder was actually scheduled (i.e. the time is in the future). */
    fun schedule(context: Context, taskId: Int, day: String, startTime: String, taskName: String, soundUri: String?): Boolean {
        return try {
            // Task times are stored as "yyyy-MM-dd" + "h:mm a"
            val format = SimpleDateFormat("yyyy-MM-dd h:mm a", Locale.getDefault())
            val triggerTime = format.parse("$day $startTime")?.time ?: return false

            // Don't schedule past events
            if (triggerTime < System.currentTimeMillis()) return false

            scheduleAtMillis(context, taskId, triggerTime, taskName, soundUri)
            true
        } catch (e: Exception) {
            Log.e("Benchmark", "Failed to schedule alarm for '$taskName'", e)
            false
        }
    }

    /** Schedule at an absolute time — used by snooze. */
    fun scheduleAtMillis(context: Context, taskId: Int, triggerAtMillis: Long, taskName: String?, soundUri: String?) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = buildPendingIntent(context, taskId, taskName, soundUri)

            // On Android 12+ the user can revoke exact-alarm access; fall back
            // to a windowed alarm instead of crashing with SecurityException.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, 60_000L, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("Benchmark", "Failed to schedule alarm for '$taskName'", e)
        }
    }

    fun cancel(context: Context, taskId: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(buildPendingIntent(context, taskId, taskName = null, soundUri = null))
        } catch (e: Exception) {
            Log.e("Benchmark", "Failed to cancel alarm $taskId", e)
        }
    }

    private fun buildPendingIntent(context: Context, taskId: Int, taskName: String?, soundUri: String?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            if (taskName != null) putExtra(EXTRA_TASK_NAME, taskName)
            if (soundUri != null) putExtra(EXTRA_SOUND_URI, soundUri)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
