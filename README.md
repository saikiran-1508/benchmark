# ⏰ Benchmark — Voice-Controlled Alarm Timetable

Benchmark is a modern Android productivity app that lets you **manage your daily timetable by voice**. Say *"Add gym at 6 PM for 1 hour"* and the app creates the task, schedules an exact alarm, and rings like a real alarm clock at 6 PM — with Stop, Snooze, and Mark Done right on the notification.

Built with **100% Kotlin + Jetpack Compose**, backed by **Room**, secured with **Firebase Auth**, and powered by **Google Gemini** (with a full offline fallback).

---

## ✨ Features

### 🎙️ Voice Assistant
Tap the mic and speak naturally. The assistant knows your actual schedule, confirms out loud, and keeps listening so you can chain commands.

| Say this | It does |
|---|---|
| "Add gym at 6 PM for 1 hour" | Creates the task + schedules the alarm |
| "Move gym to 7:30" | Reschedules the task and its alarm |
| "Delete gym" | Removes the task, cancels the alarm |
| "Mark gym as done" | Completes it (keeps your streak alive) |
| "Mark gym as important" | Stars it into the Focus tab |
| "What's my schedule?" | Reads today's tasks aloud |
| "Stop" | Ends the conversation |

**Works offline too** — if there's no Gemini API key or no internet, a built-in rule-based parser handles all the commands above.

### ⏰ Alarm-Style Reminders (WhatsApp-call style)
- Pops **over the lock screen** (full-screen intent) and turns the screen on
- **Rings and vibrates continuously** like an incoming call — on the alarm audio stream, with the ringtone *you* picked per task
- **Stop / Snooze 10m / Done** action buttons; Done also completes the task in the database
- Survives **reboots** (alarms are re-registered on boot)
- 60-second safety cutoff so a missed alarm never rings forever

### 📅 Timetable & Tasks
- **Home** — scrollable 365-day calendar strip, chronologically sorted task timeline, smart add dialog (time picker, duration preset chips, per-task ringtone picker, AI duration hint)
- **Grid** — hour-by-hour daily grid with tap-to-add, long-press copy/paste of tasks, configurable wake/sleep hours
- **Focus** — shows *only* the tasks you starred as important
- **Profile** — real Firebase account info, computed day-streak, animated productivity pie chart (deep vs shallow work), CSV export, working notification/alarm settings shortcuts, sign out

### 🔐 Authentication
- Email/password + **Google Sign-In** via Firebase
- Friendly error messages, password visibility toggles, forgot-password reset email
- Auth-gated navigation: signed-in users skip straight to the dashboard

---

## 🏗️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose (single-activity) |
| State | ViewModel + StateFlow + Coroutines |
| Database | Room (KSP) |
| Alarms | AlarmManager (`setExactAndAllowWhileIdle`) |
| Notifications | NotificationManager, silent channel + manual MediaPlayer (per-task sounds) |
| Voice | SpeechRecognizer + TextToSpeech |
| AI | Google Generative AI SDK (Gemini 1.5 Flash) + offline regex parser |
| Auth | Firebase Authentication + Google Sign-In |

## 📂 Project Structure

```
app/src/main/java/com/example/benchmark/
├── MainActivity.kt          # Entry point, runtime permissions
├── BenchmarkApp.kt          # Application class — notification channel
├── TaskViewModel.kt         # Task CRUD + alarm orchestration
├── AuthViewModel.kt         # Firebase auth with friendly errors
├── ReminderScheduler.kt     # Single source of truth for AlarmManager
├── ReminderReceiver.kt      # Fires the alarm UI (Stop/Snooze/Done)
├── AlarmSoundPlayer.kt      # Looping ringtone + vibration engine
├── BootReceiver.kt          # Re-registers alarms after reboot
├── data/                    # Room: TaskEntity, TaskDao, AppDatabase
└── ui/
    ├── Navigation.kt        # Auth-aware NavHost + bottom bar
    ├── Screen.kt            # Route definitions
    ├── components/          # GeminiManager, VoiceOverlay, task item, bars
    └── screens/             # Dashboard, Timetable, Focus, Profile, SignIn/Up
```

## 🚀 Setup

1. **Clone & open** in Android Studio (Koala+):
   ```bash
   git clone https://github.com/saikiran-1508/benchmark.git
   ```

2. **Firebase** — the project ships with `app/google-services.json`. For your own Firebase project, replace it and register your debug SHA-1 (needed for Google Sign-In):
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
   ```
   Add the SHA-1 in Firebase Console → Project settings → Your apps, then re-download `google-services.json`.

3. **Gemini API key (optional)** — add one line to `local.properties`:
   ```properties
   GEMINI_API_KEY=your_key_from_aistudio.google.com
   ```
   No key? Voice commands still work through the offline parser.

4. **Run** on a device (voice + alarms need real hardware):
   ```bash
   ./gradlew installDebug
   ```

## 📱 Permissions

| Permission | Why |
|---|---|
| `RECORD_AUDIO` | Voice commands |
| `POST_NOTIFICATIONS` | Reminder alerts (Android 13+) |
| `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM` | Alarms that fire exactly on time |
| `USE_FULL_SCREEN_INTENT` | Lock-screen alarm pop-up |
| `RECEIVE_BOOT_COMPLETED` | Restore alarms after reboot |

## ⚠️ Troubleshooting on vivo / oppo / xiaomi

OEM skins aggressively block notifications and background alarms. On these devices, enable for Benchmark:
- **Notifications → Banner + Lock screen styles ON**
- **Autostart / Auto-launch ON**
- **Battery → Unrestricted** (otherwise the OS silently cancels scheduled alarms)

## 🗺️ Roadmap

- [ ] Recurring tasks ("gym every Mon/Wed/Fri")
- [ ] Proper Room migrations (currently destructive during development)
- [ ] Firestore cloud sync of tasks across devices
- [ ] Custom app icon + R8 release shrinking
- [ ] Unit tests for streak calculator and voice parser

---

*Built with Kotlin, Compose, and a lot of real-device debugging.* 🤖
