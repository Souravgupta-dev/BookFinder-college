package com.example.ui.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthenticationViewModel : ViewModel() {
  private val auth: FirebaseAuth = FirebaseAuth.getInstance()

  private val _currentUser = MutableStateFlow(auth.currentUser)
  val currentUser: StateFlow<com.google.firebase.auth.FirebaseUser?> = _currentUser.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorMessage = MutableStateFlow<String?>(null)
  val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

  init {
    auth.addAuthStateListener { firebaseAuth ->
      _currentUser.value = firebaseAuth.currentUser
    }
  }

  fun signInWithEmail(email: String, pass: String, onResult: (Boolean) -> Unit) {
    if (email.isBlank() || pass.isBlank()) {
      _errorMessage.value = "Email and password cannot be empty"
      onResult(false)
      return
    }
    _isLoading.value = true
    _errorMessage.value = null
    auth.signInWithEmailAndPassword(email, pass)
      .addOnCompleteListener { task ->
        _isLoading.value = false
        if (task.isSuccessful) {
          _currentUser.value = auth.currentUser
          onResult(true)
        } else {
          _errorMessage.value = task.exception?.localizedMessage ?: "Sign-in failed"
          onResult(false)
        }
      }
  }

  fun signUpWithEmail(email: String, pass: String, onResult: (Boolean) -> Unit) {
    if (email.isBlank() || pass.isBlank()) {
      _errorMessage.value = "Email and password cannot be empty"
      onResult(false)
      return
    }
    _isLoading.value = true
    _errorMessage.value = null
    auth.createUserWithEmailAndPassword(email, pass)
      .addOnCompleteListener { task ->
        _isLoading.value = false
        if (task.isSuccessful) {
          _currentUser.value = auth.currentUser
          onResult(true)
        } else {
          _errorMessage.value = task.exception?.localizedMessage ?: "Sign-up failed"
          onResult(false)
        }
      }
  }

  fun signInWithGoogleCredential(context: Context, serverClientId: String, onResult: (Boolean) -> Unit) {
    viewModelScope.launch {
      _isLoading.value = true
      _errorMessage.value = null
      try {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
          .setFilterByAuthorizedAccounts(false)
          .setServerClientId(serverClientId)
          .setAutoSelectEnabled(false)
          .build()

        val request = GetCredentialRequest.Builder()
          .addCredentialOption(googleIdOption)
          .build()

        val result = credentialManager.getCredential(context, request)
        handleGoogleCredentialResult(result, onResult)
      } catch (e: Exception) {
        _isLoading.value = false
        _errorMessage.value = e.localizedMessage ?: "Google Sign-In failed"
        onResult(false)
      }
    }
  }

  private fun handleGoogleCredentialResult(result: GetCredentialResponse, onResult: (Boolean) -> Unit) {
    val credential = result.credential
    if (credential is androidx.credentials.CustomCredential &&
        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
      try {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val googleIdToken = googleIdTokenCredential.idToken
        val firebaseCred = GoogleAuthProvider.getCredential(googleIdToken, null)

        auth.signInWithCredential(firebaseCred)
          .addOnCompleteListener { task ->
            _isLoading.value = false
            if (task.isSuccessful) {
              _currentUser.value = auth.currentUser
              onResult(true)
            } else {
              _errorMessage.value = task.exception?.localizedMessage ?: "Firebase Google auth failed"
              onResult(false)
            }
          }
      } catch (e: Exception) {
        _isLoading.value = false
        _errorMessage.value = e.localizedMessage ?: "Failed to parse Google ID token"
        onResult(false)
      }
    } else {
      _isLoading.value = false
      _errorMessage.value = "Unexpected credential type"
      onResult(false)
    }
  }

  fun signOut() {
    auth.signOut()
    _currentUser.value = null
  }
}
