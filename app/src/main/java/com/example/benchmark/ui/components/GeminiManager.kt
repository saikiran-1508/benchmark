package com.example.benchmark.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.benchmark.BuildConfig
import com.example.benchmark.Task
import com.example.benchmark.TaskViewModel
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class VoiceStatus { IDLE, LISTENING, PROCESSING, SPEAKING, ERROR }

/**
 * Voice assistant for the timetable.
 *
 * Understands: add, delete, move (reschedule), complete, list, chat, stop.
 * Uses Gemini when an API key is configured (GEMINI_API_KEY in local.properties);
 * otherwise — or whenever the network call fails — it falls back to a local
 * rule-based parser so voice control always works.
 */
class GeminiManager(private val context: Context, private val viewModel: TaskViewModel) {

    private val geminiApiKey = BuildConfig.GEMINI_API_KEY

    private val generativeModel: GenerativeModel? =
        if (geminiApiKey.isNotBlank()) GenerativeModel(modelName = "gemini-1.5-flash", apiKey = geminiApiKey)
        else null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var tts: TextToSpeech? = null

    private val _status = MutableStateFlow(VoiceStatus.IDLE)
    val status: StateFlow<VoiceStatus> = _status

    // What the assistant last heard / said, shown in the UI overlay
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript

    private val dbDayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    init {
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) Log.e("GeminiManager", "TTS Init Failed")
        }
    }

    // --- PUBLIC API ---

    fun startConversation() {
        speak("Hi! You can add, move, or delete tasks. What would you like?") {
            startListening()
        }
    }

    fun stopConversation() {
        if (_status.value == VoiceStatus.SPEAKING) tts?.stop()
        speechRecognizer.stopListening()
        _status.value = VoiceStatus.IDLE
        _transcript.value = ""
    }

    /** Release the recognizer/TTS. Call when the hosting screen leaves composition. */
    fun destroy() {
        stopConversation()
        speechRecognizer.destroy()
        tts?.shutdown()
        scope.cancel()
    }

    // --- SPEECH OUTPUT ---

    private fun speak(text: String, onDone: () -> Unit = {}) {
        _status.value = VoiceStatus.SPEAKING
        _transcript.value = text
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "id")

        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                scope.launch { onDone() }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                scope.launch { _status.value = VoiceStatus.IDLE }
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "id")
    }

    // --- SPEECH INPUT ---

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        scope.launch {
            try {
                _status.value = VoiceStatus.LISTENING
                _transcript.value = "Listening..."
                delay(300)
                speechRecognizer.setRecognitionListener(recognitionListener)
                speechRecognizer.startListening(intent)
            } catch (e: Exception) {
                _status.value = VoiceStatus.ERROR
            }
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { _status.value = VoiceStatus.PROCESSING }

        override fun onError(error: Int) {
            if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                speak("I didn't hear anything. Try again from the mic button.") {
                    _status.value = VoiceStatus.IDLE
                }
            } else {
                _status.value = VoiceStatus.IDLE
            }
        }

        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            if (text.isNotEmpty()) {
                _transcript.value = "\"$text\""
                processCommand(text)
            } else {
                _status.value = VoiceStatus.IDLE
            }
        }

        override fun onPartialResults(p: Bundle?) {}
        override fun onEvent(e: Int, p: Bundle?) {}
    }

    // --- COMMAND PROCESSING ---

    private fun processCommand(userText: String) {
        _status.value = VoiceStatus.PROCESSING
        CoroutineScope(Dispatchers.IO).launch {
            val command = if (generativeModel != null) {
                askGemini(userText) ?: parseLocally(userText)
            } else {
                parseLocally(userText)
            }
            executeCommand(command)
        }
    }

    private data class VoiceCommand(
        val action: String,           // add | delete | move | complete | list | chat | stop
        val taskName: String = "",
        val time: String = "",
        val duration: String = "1h",
        val reply: String = ""
    )

    private fun scheduleContext(): String {
        val today = dbDayFormat.format(Calendar.getInstance().time)
        val todays = viewModel.tasksForDay(today)
        return if (todays.isEmpty()) "The user has no tasks today."
        else "Today's tasks: " + todays.joinToString("; ") {
            "'${it.name}' at ${it.startTime} for ${it.duration}" + if (it.isCompleted) " (done)" else ""
        }
    }

    private suspend fun askGemini(userText: String): VoiceCommand? {
        return try {
            val prompt = """
                You are a voice assistant for a timetable app. ${scheduleContext()}

                Interpret the user's request and return ONLY valid JSON (no markdown):
                - Add a task:      {"action":"add","taskName":"Gym","time":"6:00 PM","duration":"1h","reply":"Added Gym at 6 PM."}
                - Delete a task:   {"action":"delete","taskName":"Gym","reply":"Deleted Gym."}
                - Move/reschedule: {"action":"move","taskName":"Gym","time":"7:00 PM","reply":"Moved Gym to 7 PM."}
                - Mark done:       {"action":"complete","taskName":"Gym","reply":"Nice work! Marked Gym as done."}
                - Mark important:  {"action":"important","taskName":"Gym","reply":"Starred Gym — it's in your Focus list."}
                - Read schedule:   {"action":"list","reply":""}
                - Small talk:      {"action":"chat","reply":"Hello! What shall we plan?"}
                - Stop/bye:        {"action":"stop","reply":"Goodbye! Have a productive day."}

                Rules: time format must be "h:mm a" (e.g. "6:30 PM"). Default time 9:00 AM, default duration 1h.
                When deleting/moving/completing, taskName must match one of today's task names as closely as possible.

                User said: "$userText"
            """.trimIndent()

            val response = generativeModel!!.generateContent(prompt)
            val cleanJson = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: return null
            if (!cleanJson.startsWith("{")) return null
            val data = JSONObject(cleanJson)
            VoiceCommand(
                action = data.optString("action"),
                taskName = data.optString("taskName"),
                time = data.optString("time"),
                duration = data.optString("duration").ifBlank { "1h" },
                reply = data.optString("reply")
            )
        } catch (e: Exception) {
            Log.e("GeminiManager", "Gemini failed, falling back to local parser", e)
            null
        }
    }

    /** Rule-based fallback so voice control works offline / without an API key. */
    private fun parseLocally(userText: String): VoiceCommand {
        val text = userText.trim().lowercase(Locale.getDefault())
        val timeRegex = Regex("""(\d{1,2})(?::(\d{2}))?\s*(a\.?m\.?|p\.?m\.?)?""")
        val durationRegex = Regex("""for\s+(\d+)\s*(hours?|hrs?|h|minutes?|mins?|m)\b""")

        fun extractTime(str: String): String {
            val atMatch = Regex("""(?:at|to)\s+(\d{1,2}(?::\d{2})?\s*(?:a\.?m\.?|p\.?m\.?)?)""").find(str)
                ?: return "9:00 AM"
            val m = timeRegex.find(atMatch.groupValues[1]) ?: return "9:00 AM"
            var hour = m.groupValues[1].toInt()
            val minute = m.groupValues[2].ifBlank { "0" }.toInt()
            val meridiem = m.groupValues[3].replace(".", "")
            // No AM/PM said: assume daytime (1-6 -> PM), like a human would
            val isPm = meridiem.startsWith("p") || (meridiem.isBlank() && hour in 1..6)
            if (hour == 12) hour = if (meridiem.startsWith("a")) 0 else 12
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, if (isPm && hour < 12) hour + 12 else hour)
                set(Calendar.MINUTE, minute)
            }
            return timeFormat.format(cal.time)
        }

        fun extractDuration(str: String): String {
            val m = durationRegex.find(str) ?: return "1h"
            val amount = m.groupValues[1]
            return if (m.groupValues[2].startsWith("m")) "${amount}m" else "${amount}h"
        }

        fun extractName(str: String, keywords: List<String>): String {
            var name = str
            keywords.forEach { name = name.replaceFirst(Regex("""\b$it\b"""), "") }
            name = name.replace(Regex("""\b(?:at|to)\s+\d{1,2}(?::\d{2})?\s*(?:a\.?m\.?|p\.?m\.?)?"""), "")
            name = name.replace(durationRegex, "")
            name = name.replace(Regex("""\b(a|an|the|task|my|called|named|please)\b"""), "")
            return name.trim().replaceFirstChar { it.uppercase() }
        }

        return when {
            Regex("""\b(stop|bye|goodbye|exit|that's all|nothing)\b""").containsMatchIn(text) ->
                VoiceCommand(action = "stop", reply = "Goodbye! Have a productive day.")

            Regex("""\b(what|show|read|list|tell me).*(schedule|tasks?|plan|today)|\bmy schedule\b""").containsMatchIn(text) ->
                VoiceCommand(action = "list")

            Regex("""\b(delete|remove|cancel|clear)\b""").containsMatchIn(text) -> {
                val name = extractName(text, listOf("delete", "remove", "cancel", "clear"))
                VoiceCommand(action = "delete", taskName = name, reply = "Deleted $name.")
            }

            Regex("""\b(move|reschedule|shift|change|push)\b""").containsMatchIn(text) -> {
                val name = extractName(text, listOf("move", "reschedule", "shift", "change", "push"))
                val time = extractTime(text)
                VoiceCommand(action = "move", taskName = name, time = time, reply = "Moved $name to $time.")
            }

            Regex("""\b(important|star|starred|priority|focus)\b""").containsMatchIn(text) -> {
                val name = extractName(text, listOf("mark", "make", "set", "as", "important", "star", "starred", "priority", "add", "to", "focus"))
                VoiceCommand(action = "important", taskName = name, reply = "Starred $name — it's in your Focus list.")
            }

            Regex("""\b(complete|completed|done|finish|finished|mark)\b""").containsMatchIn(text) -> {
                val name = extractName(text, listOf("complete", "completed", "done", "finish", "finished", "mark", "as"))
                VoiceCommand(action = "complete", taskName = name, reply = "Great job! Marked $name as done.")
            }

            Regex("""\b(add|create|schedule|set|new|remind)\b""").containsMatchIn(text) -> {
                val name = extractName(text, listOf("add", "create", "schedule", "set", "new", "remind", "me", "reminder", "for"))
                val time = extractTime(text)
                val duration = extractDuration(text)
                VoiceCommand(
                    action = "add", taskName = name.ifBlank { "New Task" }, time = time, duration = duration,
                    reply = "Added ${name.ifBlank { "your task" }} at $time."
                )
            }

            Regex("""\b(hello|hi|hey|how are you)\b""").containsMatchIn(text) ->
                VoiceCommand(action = "chat", reply = "Hello! Try saying: add gym at 6 PM, or move gym to 7.")

            else -> VoiceCommand(action = "chat", reply = "You can say add, move, delete, or ask for your schedule.")
        }
    }

    // --- EXECUTION ---

    private fun findTask(name: String): Task? {
        if (name.isBlank()) return null
        val today = dbDayFormat.format(Calendar.getInstance().time)
        val todays = viewModel.tasksForDay(today)
        val query = name.lowercase(Locale.getDefault())
        return todays.firstOrNull { it.name.lowercase(Locale.getDefault()) == query }
            ?: todays.firstOrNull {
                it.name.lowercase(Locale.getDefault()).contains(query) ||
                        query.contains(it.name.lowercase(Locale.getDefault()))
            }
    }

    private fun executeCommand(command: VoiceCommand) {
        val today = dbDayFormat.format(Calendar.getInstance().time)

        when (command.action) {
            "add" -> {
                viewModel.addTask(context, command.taskName, command.duration, command.time.ifBlank { "9:00 AM" }, today, null)
                speak(command.reply.ifBlank { "Added ${command.taskName}." }) { startListening() }
            }

            "delete" -> {
                val task = findTask(command.taskName)
                if (task != null) {
                    viewModel.deleteTask(context, task)
                    speak(command.reply.ifBlank { "Deleted ${task.name}." }) { startListening() }
                } else {
                    speak("I couldn't find a task called ${command.taskName} today.") { startListening() }
                }
            }

            "move" -> {
                val task = findTask(command.taskName)
                if (task != null && command.time.isNotBlank()) {
                    viewModel.rescheduleTask(context, task, command.time)
                    speak(command.reply.ifBlank { "Moved ${task.name} to ${command.time}." }) { startListening() }
                } else {
                    speak("I couldn't find a task called ${command.taskName} to move.") { startListening() }
                }
            }

            "complete" -> {
                val task = findTask(command.taskName)
                if (task != null) {
                    if (!task.isCompleted) viewModel.toggleComplete(context, task)
                    speak(command.reply.ifBlank { "Marked ${task.name} as done." }) { startListening() }
                } else {
                    speak("I couldn't find a task called ${command.taskName}.") { startListening() }
                }
            }

            "important" -> {
                val task = findTask(command.taskName)
                if (task != null) {
                    if (!task.isImportant) viewModel.toggleImportant(task)
                    speak(command.reply.ifBlank { "Starred ${task.name}. It's in your Focus list now." }) { startListening() }
                } else {
                    speak("I couldn't find a task called ${command.taskName}.") { startListening() }
                }
            }

            "list" -> {
                val todays = viewModel.tasksForDay(today).sortedBy { timeToMillis(it.startTime) }
                val summary = if (todays.isEmpty()) {
                    "Your schedule is empty today. Say add, to create a task."
                } else {
                    "You have ${todays.size} task${if (todays.size > 1) "s" else ""} today. " +
                            todays.joinToString(". ") { "${it.name} at ${it.startTime}" + if (it.isCompleted) ", done" else "" }
                }
                speak(summary) { startListening() }
            }

            "chat" -> speak(command.reply) { startListening() }

            else -> speak(command.reply.ifBlank { "Goodbye!" }) {
                _status.value = VoiceStatus.IDLE
                _transcript.value = ""
            }
        }
    }

    private fun timeToMillis(time: String): Long = try {
        timeFormat.parse(time)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}
