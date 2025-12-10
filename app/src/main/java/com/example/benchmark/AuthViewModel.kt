package com.example.benchmark

import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider // Import this
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    private val _user = MutableStateFlow(auth.currentUser)
    val user = _user.asStateFlow()

    // 1. Sign Up (Email)
    fun signUp(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isEmpty() || pass.isEmpty()) { onError("Empty fields"); return }
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) { _user.value = auth.currentUser; onSuccess() }
                else { onError(task.exception?.message ?: "Error") }
            }
    }

    // 2. Sign In (Email)
    fun signIn(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isEmpty() || pass.isEmpty()) { onError("Empty fields"); return }
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) { _user.value = auth.currentUser; onSuccess() }
                else { onError(task.exception?.message ?: "Error") }
            }
    }

    // 3. GOOGLE SIGN IN (New Function)
    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _user.value = auth.currentUser
                    onSuccess()
                } else {
                    onError(task.exception?.message ?: "Google Sign-In failed")
                }
            }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
    }

    fun isUserLoggedIn() = auth.currentUser != null
}