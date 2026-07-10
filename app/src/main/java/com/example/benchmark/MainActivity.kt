package com.example.benchmark

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.benchmark.ui.AppNavigation
import com.example.benchmark.ui.theme.BenchmarkTheme

class MainActivity : ComponentActivity() {

    private val taskViewModel: TaskViewModel by viewModels()

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* results handled per-feature */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRuntimePermissions()
        requestExactAlarmAccessIfNeeded()

        setContent {
            BenchmarkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel = taskViewModel)
                }
            }
        }
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        // Notifications need a runtime grant on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (needed.isNotEmpty()) {
            requestPermissionsLauncher.launch(needed.toTypedArray())
        }
    }

    // On Android 12/12L the user can revoke exact-alarm access in Settings;
    // send them there once so task reminders fire on time.
    private fun requestExactAlarmAccessIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } catch (e: Exception) {
                    // Some devices don't expose this screen; scheduler falls back to windowed alarms
                }
            }
        }
    }
}
