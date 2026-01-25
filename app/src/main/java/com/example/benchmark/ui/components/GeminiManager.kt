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

class GeminiManager(private val context: Context, private val viewModel: TaskViewModel) {

    // 🔴 YOUR API KEY 🔴
    private val geminiApiKey = "YOUR_GEMINI_API_KEY"

    // 🧠 IMPROVED PROMPT: Gives Gemini a personality!
    private val systemInstruction = """
        You are a helpful, friendly voice assistant for a Timetable App.
        
        RULES:
        1. If the user says "Hello", "Hi", or asks how you are, reply warmly.
           Example JSON: {"action": "chat", "reply": "Hello! I'm ready to help. What task shall we add?"}
           
        2. If the user wants to ADD a task, extract details.
           Example JSON: {"action": "add", "taskName": "Gym", "time": "6:00 PM", "duration": "1h", "reply": "Okay, I've added Gym for 6 PM."}
           
        3. If the user says "Stop", "Cancel", or "Bye", say goodbye.
           Example JSON: {"action": "stop", "reply": "Goodbye! Have a productive day."}
           
        4. ALWAYS return valid JSON. Do not use Markdown.
        5. Default time: 9:00 AM. Default duration: 1h.
    """.trimIndent()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = geminiApiKey
    )

    private val speechRecognizer: SpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    private var tts: TextToSpeech? = null

    private val _status = MutableStateFlow(VoiceStatus.IDLE)
    val status: StateFlow<VoiceStatus> = _status

    init {
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.e("GeminiManager", "TTS Init Failed")
            }
        }
    }

    // --- CONVERSATION LOOP ---

    fun startConversation() {
        // 🎉 NEW: Starts with a friendly greeting!
        speak("Hi there! What's on your schedule today?") {
            startListening()
        }
    }

    fun stopConversation() {
        if (_status.value == VoiceStatus.SPEAKING) tts?.stop()
        stopListening()
        _status.value = VoiceStatus.IDLE
    }

    private fun speak(text: String, onDone: () -> Unit = {}) {
        _status.value = VoiceStatus.SPEAKING
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "id")

        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                CoroutineScope(Dispatchers.Main).launch { onDone() }
            }
            override fun onError(id: String?) {
                CoroutineScope(Dispatchers.Main).launch { _status.value = VoiceStatus.IDLE }
            }
        })

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "id")
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
                delay(300)
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
            if (error != SpeechRecognizer.ERROR_NO_MATCH) _status.value = VoiceStatus.IDLE
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull() ?: ""
            if (text.isNotEmpty()) processWithGemini(text)
        }

        override fun onPartialResults(p: Bundle?) {}
        override fun onEvent(e: Int, p: Bundle?) {}
    }

    private fun processWithGemini(userText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Combine System Instructions + User Input
                val fullPrompt = "$systemInstruction\n\nUser said: \"$userText\""

                val response = generativeModel.generateContent(fullPrompt)
                val cleanJson = response.text?.replace("```json", "")?.replace("```", "")?.trim() ?: "{}"

                if (cleanJson.startsWith("{")) {
                    val data = JSONObject(cleanJson)
                    val action = data.optString("action")
                    val reply = data.optString("reply") // The "Human" reply

                    if (action == "add") {
                        val taskName = data.optString("taskName")
                        val time = data.optString("time")
                        val duration = data.optString("duration")

                        val dbFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val todayString = dbFormatter.format(Calendar.getInstance().time)

                        viewModel.addTask(context, taskName, duration, time, todayString, null)

                        // Speak confirmation & Listen again for next command
                        speak(reply) { startListening() }
                    }
                    else if (action == "chat") {
                        // Just chatting (Hello/How are you) -> Listen again
                        speak(reply) { startListening() }
                    }
                    else {
                        // Stop/Bye
                        speak(reply) { _status.value = VoiceStatus.IDLE }
                    }
                }
            } catch (e: Exception) {
                Log.e("Gemini", "Error", e)
                speak("I didn't quite catch that.") { _status.value = VoiceStatus.IDLE }
            }
        }
    }
}