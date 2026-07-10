package com.example.benchmark

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class BenchmarkApp : Application() {

    companion object {
        const val REMINDER_CHANNEL_ID = "benchmark_reminders"
    }

    override fun onCreate() {
        super.onCreate()
        createReminderChannel()
    }

    private fun createReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarms for your scheduled tasks"
                enableVibration(true)
                // Channel is silent on purpose: the channel sound would override
                // the per-task ringtone, so ReminderReceiver plays it manually.
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
