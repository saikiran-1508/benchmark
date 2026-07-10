package com.example.benchmark.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fullscreen dim overlay shown while the voice assistant is active. */
@Composable
fun VoiceOverlay(status: VoiceStatus, transcript: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = status != VoiceStatus.IDLE,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when (status) {
                        VoiceStatus.SPEAKING -> "Assistant Speaking..."
                        VoiceStatus.LISTENING -> "Listening..."
                        VoiceStatus.PROCESSING -> "Thinking..."
                        VoiceStatus.ERROR -> "Something went wrong"
                        else -> ""
                    },
                    color = Color.Cyan,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                if (transcript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = transcript,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Try: \"Add gym at 6 PM\" · \"Move gym to 7\" · \"Delete gym\"",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}
