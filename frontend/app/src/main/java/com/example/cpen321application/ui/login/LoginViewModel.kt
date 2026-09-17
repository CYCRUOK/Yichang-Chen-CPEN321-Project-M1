package com.example.cpen321application.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cpen321application.auth.GoogleAuthenticator
import com.example.cpen321application.auth.GoogleUser
import com.example.cpen321application.auth.SignInException
import com.example.cpen321application.info.ClientInfo
import com.example.cpen321application.info.InfoApi
import com.example.cpen321application.info.InfoApiException
import com.example.cpen321application.info.OwnerName
import java.time.ZonedDateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything Button 1 displays after sign-in. */
data class ConnectionInfo(
    val serverIp: String,
    val clientIp: String,
    val serverTime: String,
    val clientTime: String,
    val owner: OwnerName,
)

sealed interface InfoState {
    data object Loading : InfoState
    data class Loaded(val info: ConnectionInfo) : InfoState
    data class Failed(val message: String) : InfoState
}

sealed interface LoginUiState {
    data object SignedOut : LoginUiState
    data object SigningIn : LoginUiState
    data class SignedIn(val user: GoogleUser, val info: InfoState = InfoState.Loading) : LoginUiState
    data class Error(val message: String) : LoginUiState
}

/** Holds the sign-in state and the server/client info across configuration changes. */
class LoginViewModel(
    private val authenticator: GoogleAuthenticator,
    private val infoApi: InfoApi,
    private val clientIp: () -> String = ClientInfo::localIp,
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.SignedOut)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var infoJob: Job? = null

    fun signIn() {
        if (_uiState.value is LoginUiState.SigningIn) return
        _uiState.value = LoginUiState.SigningIn
        viewModelScope.launch {
            try {
                val user = authenticator.signIn()
                _uiState.value = LoginUiState.SignedIn(user)
                loadInfo()
            } catch (e: SignInException) {
                _uiState.value = LoginUiState.Error(e.message ?: "Sign-in failed")
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error("Sign-in failed: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    fun signOut() {
        infoJob?.cancel()
        viewModelScope.launch {
            try {
                authenticator.signOut()
            } finally {
                _uiState.value = LoginUiState.SignedOut
            }
        }
    }

    /** (Re)fetches the three server APIs and samples the client IP/time alongside. */
    fun loadInfo() {
        if (_uiState.value !is LoginUiState.SignedIn) return
        infoJob?.cancel()
        setInfo(InfoState.Loading)
        infoJob = viewModelScope.launch {
            val result = try {
                coroutineScope {
                    val ip = async { infoApi.serverIp() }
                    val owner = async { infoApi.ownerName() }
                    val serverTime = async { infoApi.serverTime() }
                    // Sample the client clock right after the server's time arrives so
                    // the two rows are comparable.
                    val serverTimeValue = serverTime.await()
                    val clientTime = ClientInfo.formatLocalTime(now())
                    InfoState.Loaded(
                        ConnectionInfo(
                            serverIp = ip.await(),
                            clientIp = clientIp(),
                            serverTime = serverTimeValue,
                            clientTime = clientTime,
                            owner = owner.await(),
                        ),
                    )
                }
            } catch (e: InfoApiException) {
                InfoState.Failed(e.message ?: "Could not reach the server")
            } catch (e: Exception) {
                InfoState.Failed("Could not reach the server: ${e.message ?: e.javaClass.simpleName}")
            }
            setInfo(result)
        }
    }

    private fun setInfo(info: InfoState) {
        _uiState.update { state ->
            if (state is LoginUiState.SignedIn) state.copy(info = info) else state
        }
    }
}
