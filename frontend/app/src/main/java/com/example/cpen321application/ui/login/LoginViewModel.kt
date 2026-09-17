package com.example.cpen321application.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cpen321application.auth.GoogleAuthenticator
import com.example.cpen321application.auth.GoogleUser
import com.example.cpen321application.auth.SignInException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object SignedOut : LoginUiState
    data object SigningIn : LoginUiState
    data class SignedIn(val user: GoogleUser) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

/** Holds the sign-in state across configuration changes (rotation). */
class LoginViewModel(private val authenticator: GoogleAuthenticator) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.SignedOut)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn() {
        if (_uiState.value is LoginUiState.SigningIn) return
        _uiState.value = LoginUiState.SigningIn
        viewModelScope.launch {
            _uiState.value = try {
                LoginUiState.SignedIn(authenticator.signIn())
            } catch (e: SignInException) {
                LoginUiState.Error(e.message ?: "Sign-in failed")
            } catch (e: Exception) {
                LoginUiState.Error("Sign-in failed: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                authenticator.signOut()
            } finally {
                _uiState.value = LoginUiState.SignedOut
            }
        }
    }
}
