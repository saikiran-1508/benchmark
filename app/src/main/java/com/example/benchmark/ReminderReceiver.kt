package com.example.benchmark

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1. Get data from the intent
        val taskName = intent.getStringExtra("TASK_NAME") ?: "Benchmark Task"
        val soundUriString = intent.getStringExtra("SOUND_URI")

        // 2. Build the Sound URI
        val soundUri = if (!soundUriString.isNullOrEmpty()) {
            Uri.parse(soundUriString)
        } else {
            // Fallback to default notification sound
            android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
        }

        // 3. Create the Notification
        // We use a specific channel ID (created in Step 3)
        val builder = NotificationCompat.Builder(context, "benchmark_reminders")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // Use app icon in real app
            .setContentTitle("Benchmark Reminder")
            .setContentText(taskName)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri) // <--- PLAYS YOUR CUSTOM TONE

        // 4. Show it!
        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        }
    }
}