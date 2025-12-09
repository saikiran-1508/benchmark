package com.example.benchmark.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SignInScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 🎨 Theme: "Deep Ocean Focus"
    // Dark Navy -> Deep Teal -> Black
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F2027),
            Color(0xFF203A43),
            Color(0xFF2C5364)
        )
    )

    // Accent Color: Neon Mint (Pops against dark blue)
    val accentColor = Color(0xFF69F0AE)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Title
            Text(
                text = "BENCHMARK",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black, // Extra Bold
                color = Color.White,
                letterSpacing = 3.sp
            )
            Text(
                text = "Focus. Learn. Achieve.",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Email Field
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email", color = Color.White.copy(alpha = 0.8f)) },
                leadingIcon = { Icon(Icons.Default.Email, null, tint = accentColor) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = accentColor, // Mint Border
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = accentColor
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password", color = Color.White.copy(alpha = 0.8f)) },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = accentColor) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = accentColor
                )
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Login Button (Neon Mint)
            Button(
                onClick = onLoginSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "SIGN IN",
                    color = Color(0xFF0F2027), // Dark text on bright button
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Text
            Row(modifier = Modifier.clickable { onNavigateToSignUp() }) {
                Text("New here? ", color = Color.White.copy(alpha = 0.7f))
                Text("Start Journey", color = accentColor, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Button
            OutlinedButton(
                onClick = onLoginSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Continue with Google", fontWeight = FontWeight.Medium)
            }
        }
    }
}