package com.example.benchmark

import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow


class AuthViewModel : ViewModel() {
    // 1. Get the Firebase tool
    private val auth: FirebaseAuth = Firebase.auth

    // 2. Track the user (Are they logged in?)
    private val _user = MutableStateFlow(auth.currentUser)
    val user = _user.asStateFlow()

    // 3. Function to Sign Up
    fun signUp(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isEmpty() || pass.isEmpty()) {
            onError("Please fill in all fields")
            return
        }
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _user.value = auth.currentUser
                    onSuccess()
                } else {
                    onError(task.exception?.message ?: "Sign up failed")
                }
            }
    }

    // 4. Function to Sign In
    fun signIn(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isEmpty() || pass.isEmpty()) {
            onError("Please fill in all fields")
            return
        }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _user.value = auth.currentUser
                    onSuccess()
                } else {
                    onError(task.exception?.message ?: "Login failed")
                }
            }
    }

    // 5. Sign Out
    fun signOut() {
        auth.signOut()
        _user.value = null
    }

    // 6. Check status instantly
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}