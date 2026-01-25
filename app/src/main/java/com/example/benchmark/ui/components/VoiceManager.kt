package com.example.benchmark.ui.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.example.benchmark.TaskViewModel
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class VoiceStatus { IDLE, LISTENING, PROCESSING, SPEAKING, ERROR }
enum class ConversationState { LISTENING_FOR_TASK, CONFIRMING_TASK }

// Helper to hold data temporarily before saving
data class PendingTaskData(val name: String, val time: String, val duration: String)

class VoiceManager(private val context: Context, private val viewModel: TaskViewModel) {

    // 🔴 TODO: PASTE GEMINI KEY HERE 🔴
    private val geminiApiKey = "YOUR_GEMINI_API_KEY"

    private val generativeModel = GenerativeModel(modelName = "gemini-1.5-flash", apiKey = geminiApiKey)
    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var tts: TextToSpeech? = null

    private val _status = MutableStateFlow(VoiceStatus.IDLE)
    val status: StateFlow<VoiceStatus> = _status

    // Conversation State Tracking
    private var conversationState = ConversationState.LISTENING_FOR_TASK
    private var pendingTask: PendingTaskData? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.e("VoiceManager", "TTS Init Failed")
            }
        }
    }

    fun startConversation() {
        // Reset state
        conversationState = ConversationState.LISTENING_FOR_TASK
        pendingTask = null

        speak("What task would you like to add to your schedule?", afterSpeak = {
            startListening()
        })
    }

    fun stopConversation() {
        if (_status.value == VoiceStatus.SPEAKING) {
            tts?.stop()
        }
        stopListening()
        _status.value = VoiceStatus.IDLE
    }

    private fun speak(text: String, afterSpeak: () -> Unit = {}) {
        _status.value = VoiceStatus.SPEAKING
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageID")

        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                CoroutineScope(Dispatchers.Main).launch { afterSpeak() }
            }
            override fun onError(utteranceId: String?) { _status.value = VoiceStatus.IDLE }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "messageID")
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                _status.value = VoiceStatus.LISTENING
                delay(300) // Small delay to prevent catching TTS echo
                speechRecognizer.setRecognitionListener(recognitionListener)
                speechRecognizer.startListening(intent)
            } catch (e: Exception) {
                _status.value = VoiceStatus.ERROR
            }
        }
    }

    private fun stopListening() {
        CoroutineScope(Dispatchers.Main).launch {
            speechRecognizer.stopListening()
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { _status.value = VoiceStatus.PROCESSING }
        override fun onError(error: Int) {
            if (error != SpeechRecognizer.ERROR_NO_MATCH) _status.value = VoiceStatus.ERROR
            else _status.value = VoiceStatus.IDLE
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""

            if (text.isNotEmpty()) {
                handleUserInput(text)
            } else {
                _status.value = VoiceStatus.IDLE
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleUserInput(input: String) {
        val lowerInput = input.lowercase()

        // 1. CHECK FOR EXIT COMMANDS ALWAYS
        if (conversationState == ConversationState.LISTENING_FOR_TASK) {
            if (lowerInput.contains("done") || lowerInput.contains("finished") || lowerInput.contains("that's all")) {
                speak("Okay, schedule updated. Goodbye.") { _status.value = VoiceStatus.IDLE }
                return
            }
        }

        // 2. STATE MACHINE
        if (conversationState == ConversationState.CONFIRMING_TASK) {
            // We are waiting for YES or NO
            if (lowerInput.contains("yes") || lowerInput.contains("yeah") || lowerInput.contains("correct") || lowerInput.contains("right")) {
                // SAVE IT
                pendingTask?.let { task ->
                    val dbFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val todayString = dbFormatter.format(Calendar.getInstance().time)
                    viewModel.addTask(context, task.name, task.duration, task.time, todayString, null)
                }
                conversationState = ConversationState.LISTENING_FOR_TASK
                pendingTask = null
                speak("Saved. Do you have anything else to add?", afterSpeak = { startListening() })
            }
            else if (lowerInput.contains("no") || lowerInput.contains("stop") || lowerInput.contains("wrong")) {
                // DISCARD IT
                conversationState = ConversationState.LISTENING_FOR_TASK
                pendingTask = null
                speak("Okay, I cancelled that. What should I add instead?", afterSpeak = { startListening() })
            }
            else {
                // UNCLEAR
                speak("Please say Yes to save, or No to cancel.", afterSpeak = { startListening() })
            }
        }
        else {
            // We are LISTENING_FOR_TASK -> Send to Gemini
            processWithGemini(input)
        }
    }

    private fun processWithGemini(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Prompt Gemini to extract data
                val prompt = """
                    User said: "$text".
                    Extract: {"taskName": "...", "time": "HH:mm AM/PM", "duration": "..."}.
                    Default duration 1h. Default time 9 AM.
                    Return valid JSON only.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val cleanJson = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"

                if (cleanJson.startsWith("{")) {
                    val data = JSONObject(cleanJson)
                    val taskName = data.optString("taskName", "Task")
                    val time = data.optString("time", "09:00 AM")
                    val duration = data.optString("duration", "1h")

                    // Store temporarily
                    pendingTask = PendingTaskData(taskName, time, duration)
                    conversationState = ConversationState.CONFIRMING_TASK

                    // Ask for Clarification
                    speak("I heard $taskName at $time. Is that correct?", afterSpeak = {
                        startListening()
                    })
                } else {
                    speak("I didn't understand. Try saying 'Math at 10'.", afterSpeak = {
                        startListening()
                    })
                }
            } catch (e: Exception) {
                Log.e("VoiceManager", "AI Error", e)
                speak("Sorry, I had an error.") { _status.value = VoiceStatus.ERROR }
            }
        }
    }
}