package com.example.benchmark

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

/**
 * Plays the reminder like a real alarm: looping ringtone on the ALARM audio
 * stream plus repeating vibration, until stop() is called (user taps
 * Stop/Done on the notification) or the safety timeout hits.
 */
object AlarmSoundPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable { stop() }

    /** Id of the task currently ringing, or -1. Lets the UI silence an alarm it just deleted. */
    var ringingTaskId: Int = -1
        private set

    fun start(context: Context, soundUri: Uri, taskId: Int = -1, timeoutMs: Long = 60_000L) {
        stop() // never stack two alarms
        ringingTaskId = taskId

        // 1. Looping sound on the ALARM stream (rings even in silent-ish modes,
        //    respects the alarm volume slider like a real alarm clock)
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("Benchmark", "MediaPlayer failed, falling back to single ring", e)
            try {
                RingtoneManager.getRingtone(context, soundUri)?.play()
            } catch (ignored: Exception) {
            }
        }

        // 2. Repeating vibration (buzz-pause-buzz, like an incoming call)
        try {
            @Suppress("DEPRECATION")
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            val pattern = longArrayOf(0, 600, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e("Benchmark", "Vibration failed", e)
        }

        // 3. Safety stop so a missed alarm doesn't ring forever
        handler.postDelayed(timeoutRunnable, timeoutMs)
    }

    fun stop() {
        ringingTaskId = -1
        handler.removeCallbacks(timeoutRunnable)
        mediaPlayer?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (ignored: Exception) {
            }
            it.release()
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }
}
