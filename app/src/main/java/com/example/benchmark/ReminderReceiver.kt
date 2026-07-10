package com.example.benchmark

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.benchmark.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STOP = "com.example.benchmark.action.STOP_ALARM"
        const val ACTION_DONE = "com.example.benchmark.action.MARK_DONE"
        const val ACTION_SNOOZE = "com.example.benchmark.action.SNOOZE"
        const val EXTRA_TASK_ID = "TASK_ID"
        const val SNOOZE_MINUTES = 10
    }

    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.i("BenchmarkAlarm", "Receiver fired: action=${intent.action} task=${intent.getStringExtra(ReminderScheduler.EXTRA_TASK_NAME)} id=${intent.getIntExtra(EXTRA_TASK_ID, -1)}")
        when (intent.action) {
            // "Stop" button / notification swiped: silence the alarm
            ACTION_STOP -> {
                AlarmSoundPlayer.stop()
                NotificationManagerCompat.from(context).cancel(intent.getIntExtra(EXTRA_TASK_ID, 0))
            }

            // "Snooze" button: silence now, ring again in 10 minutes
            ACTION_SNOOZE -> {
                AlarmSoundPlayer.stop()
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
                NotificationManagerCompat.from(context).cancel(taskId)
                ReminderScheduler.scheduleAtMillis(
                    context,
                    taskId,
                    System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L,
                    intent.getStringExtra(ReminderScheduler.EXTRA_TASK_NAME),
                    intent.getStringExtra(ReminderScheduler.EXTRA_SOUND_URI)
                )
            }

            // "Done" button: silence + mark the task completed in the database
            ACTION_DONE -> {
                AlarmSoundPlayer.stop()
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, 0)
                NotificationManagerCompat.from(context).cancel(taskId)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        AppDatabase.getDatabase(context).taskDao().markCompleted(taskId)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            else -> showAlarm(context, intent)
        }
    }

    private fun showAlarm(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra(ReminderScheduler.EXTRA_TASK_NAME) ?: "Benchmark Task"
        val soundUriString = intent.getStringExtra(ReminderScheduler.EXTRA_SOUND_URI)
        val taskId = intent.getIntExtra(EXTRA_TASK_ID, 0)

        val soundUri: Uri = if (!soundUriString.isNullOrEmpty()) {
            Uri.parse(soundUriString)
        } else {
            // Alarm-style default, not the soft notification "ding"
            android.provider.Settings.System.DEFAULT_ALARM_ALERT_URI
                ?: android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
        }

        // Tapping the body opens the app (full-screen intent also pops it over the lockscreen)
        val contentIntent = PendingIntent.getActivity(
            context,
            taskId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getBroadcast(
            context,
            taskId * 10 + 1,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_STOP).putExtra(EXTRA_TASK_ID, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = PendingIntent.getBroadcast(
            context,
            taskId * 10 + 2,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_DONE).putExtra(EXTRA_TASK_ID, taskId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            taskId * 10 + 3,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_SNOOZE)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(ReminderScheduler.EXTRA_TASK_NAME, taskName)
                .putExtra(ReminderScheduler.EXTRA_SOUND_URI, soundUriString),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, BenchmarkApp.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm_notification)
            .setContentTitle("⏰ $taskName")
            .setContentText("It's time! Tap Done when finished.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // show on lockscreen
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true) // pop over the lockscreen like an alarm clock
            .setOngoing(true) // can't be swiped away while ringing — use the buttons
            .setDeleteIntent(stopIntent) // if it does get dismissed, kill the sound
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .addAction(android.R.drawable.ic_popup_reminder, "Snooze ${SNOOZE_MINUTES}m", snoozeIntent)
            .addAction(android.R.drawable.ic_menu_agenda, "Done", doneIntent)

        // Wrapped in try/catch: a rejected notification must never kill the
        // ringing alarm below (some OEM skins throw on notifications they block)
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(taskId, builder.build())
                android.util.Log.i("BenchmarkAlarm", "Notification posted for '$taskName' (id=$taskId), enabled=${notificationManager.areNotificationsEnabled()}")
            } else {
                android.util.Log.w("BenchmarkAlarm", "POST_NOTIFICATIONS not granted — notification skipped")
            }
        } catch (e: Exception) {
            android.util.Log.e("BenchmarkAlarm", "Failed to post notification", e)
        }

        // Ring + vibrate continuously (like an incoming call) until the user
        // presses Stop / Snooze / Done, or the 60s safety timeout ends it.
        AlarmSoundPlayer.start(context, soundUri, taskId)
    }
}
