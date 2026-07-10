package com.example.benchmark

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = Firebase.auth

    private val _user = MutableStateFlow(auth.currentUser)
    val user = _user.asStateFlow()

    val userEmail: String? get() = auth.currentUser?.email
    val userName: String? get() = auth.currentUser?.displayName

    // Turn raw Firebase exceptions into messages a person can act on
    private fun friendlyError(e: Exception?): String = when (e) {
        is FirebaseAuthInvalidUserException -> "No account found with this email. Try signing up."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists. Try signing in."
        is FirebaseAuthWeakPasswordException -> "Password is too weak — use at least 6 characters."
        is FirebaseNetworkException -> "No internet connection. Check your network and try again."
        else -> e?.localizedMessage ?: "Something went wrong. Please try again."
    }

    private fun validate(email: String, pass: String): String? = when {
        email.isBlank() -> "Please enter your email."
        !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "That doesn't look like a valid email."
        pass.isBlank() -> "Please enter your password."
        pass.length < 6 -> "Password must be at least 6 characters."
        else -> null
    }

    // 1. Sign Up (Email)
    fun signUp(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        validate(email, pass)?.let { onError(it); return }
        auth.createUserWithEmailAndPassword(email.trim(), pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) { _user.value = auth.currentUser; onSuccess() }
                else onError(friendlyError(task.exception))
            }
    }

    // 2. Sign In (Email)
    fun signIn(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        validate(email, pass)?.let { onError(it); return }
        auth.signInWithEmailAndPassword(email.trim(), pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) { _user.value = auth.currentUser; onSuccess() }
                else onError(friendlyError(task.exception))
            }
    }

    // 3. Google Sign In
    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) { _user.value = auth.currentUser; onSuccess() }
                else onError(friendlyError(task.exception))
            }
    }

    // 4. Forgot Password
    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            onError("Enter your email above first, then tap Forgot Password.")
            return
        }
        auth.sendPasswordResetEmail(email.trim())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) onSuccess()
                else onError(friendlyError(task.exception))
            }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
    }

    fun isUserLoggedIn() = auth.currentUser != null
}
