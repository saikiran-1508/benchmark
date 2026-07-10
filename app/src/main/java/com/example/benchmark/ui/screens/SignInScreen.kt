package com.example.benchmark.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.benchmark.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun SignInScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // --- GOOGLE SIGN IN SETUP ---
    // 1. Configure Google Sign In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.example.benchmark.R.string.default_web_client_id)) // Auto-generated ID
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // 2. Create the Launcher (The "Pop-up" handler)
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Parse the result even when resultCode != OK: configuration errors
        // (like a missing SHA-1 in Firebase) come back as "cancelled" with a
        // hidden status code, and we must show the user what went wrong.
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                authViewModel.signInWithGoogle(idToken,
                    onSuccess = {
                        isLoading = false
                        onLoginSuccess()
                    },
                    onError = { error ->
                        isLoading = false
                        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    }
                )
            } else {
                isLoading = false
                Toast.makeText(context, "Google didn't return a sign-in token. Try again.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            isLoading = false
            val message = when (e.statusCode) {
                12501 -> null // user actually cancelled the picker — stay quiet
                10 -> "Google Sign-In isn't configured for this build (SHA-1 mismatch). Use email sign-in for now."
                7 -> "No internet connection. Check your network and try again."
                else -> "Google Sign-In failed (code ${e.statusCode}). Try email sign-in instead."
            }
            message?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
        }
    }
    // ----------------------------

    // Theme Colors
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

            // Email & Password Fields
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email", color = Color.White.copy(alpha = 0.8f)) },
                leadingIcon = { Icon(Icons.Default.Email, null, tint = accentColor) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), cursorColor = accentColor
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password", color = Color.White.copy(alpha = 0.8f)) },
                leadingIcon = { Icon(Icons.Default.Lock, null, tint = accentColor) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                    focusedBorderColor = accentColor, unfocusedBorderColor = Color.White.copy(alpha = 0.3f), cursorColor = accentColor
                )
            )

            // Forgot password — sends a reset email to the address typed above
            Text(
                text = "Forgot Password?",
                color = accentColor,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
                    .clickable {
                        authViewModel.resetPassword(email,
                            onSuccess = { Toast.makeText(context, "Reset link sent to $email — check your inbox", Toast.LENGTH_LONG).show() },
                            onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
                        )
                    }
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Sign In Button
            Button(
                onClick = {
                    isLoading = true
                    authViewModel.signIn(email, password,
                        onSuccess = { isLoading = false; onLoginSuccess() },
                        onError = { errorMsg -> isLoading = false; Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_SHORT).show() }
                    )
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                else Text("SIGN IN", color = Color(0xFF0F2027), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.clickable { onNavigateToSignUp() }) {
                Text("New here? ", color = Color.White.copy(alpha = 0.7f))
                Text("Start Journey", color = accentColor, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- GOOGLE BUTTON (Connected) ---
            OutlinedButton(
                onClick = {
                    isLoading = true
                    // Launch the Google Intent
                    val signInIntent = googleSignInClient.signInIntent
                    googleLauncher.launch(signInIntent)
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Continue with Google", fontWeight = FontWeight.Medium)
            }
        }
    }
}