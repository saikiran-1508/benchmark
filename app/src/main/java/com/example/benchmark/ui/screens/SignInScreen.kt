package com.example.benchmark.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.benchmark.AuthViewModel

@Composable
fun SignInScreen(
    authViewModel: AuthViewModel, // We now pass the Logic Engine here
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) } // Loading state
    val context = LocalContext.current

    // Deep Ocean Theme
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    )
    val accentColor = Color(0xFF69F0AE)

    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("BENCHMARK", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 3.sp)
            Text("Focus. Learn. Achieve.", fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(bottom = 48.dp))

            // Email
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email", color = Color.White.copy(alpha = 0.8f)) },
                leadingIcon = { Icon(Icons.Default.Email, null, tint = accentColor) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = accentColor
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Password
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password", color = Color.White.copy(alpha = 0.8f)) },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = accentColor) },
                singleLine = true, visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = accentColor
                )
            )
            Spacer(modifier = Modifier.height(32.dp))

            // Login Button (with Loading Spinner)
            Button(
                onClick = {
                    isLoading = true
                    authViewModel.signIn(email, password,
                        onSuccess = {
                            isLoading = false
                            onLoginSuccess()
                        },
                        onError = { errorMsg ->
                            isLoading = false
                            Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show()
                        }
                    )
                },
                enabled = !isLoading, // Disable button while loading
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                } else {
                    Text("SIGN IN", color = Color(0xFF0F2027), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.clickable { onNavigateToSignUp() }) {
                Text("New here? ", color = Color.White.copy(alpha = 0.7f))
                Text("Start Journey", color = accentColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}