package com.example.cpen321application.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cpen321application.auth.GoogleAuthenticator
import com.example.cpen321application.auth.GoogleUser
import com.example.cpen321application.ui.components.FeatureScreen

/** Button 1: Google sign-in, then (step 6) server + client info. */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    authenticator: GoogleAuthenticator,
    viewModel: LoginViewModel = viewModel { LoginViewModel(authenticator) },
) {
    val uiState by viewModel.uiState.collectAsState()

    FeatureScreen(title = "Login + Server", testTag = "screen_login", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            when (val state = uiState) {
                LoginUiState.SignedOut -> SignedOutContent(onSignIn = viewModel::signIn)
                LoginUiState.SigningIn -> SigningInContent()
                is LoginUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::signIn)
                is LoginUiState.SignedIn -> SignedInContent(user = state.user, onSignOut = viewModel::signOut)
            }
        }
    }
}

@Composable
private fun SignedOutContent(onSignIn: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    Text(
        text = "Sign in with your Google account to view server and client information.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(32.dp))
    Button(
        onClick = onSignIn,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("btn_google_sign_in"),
    ) {
        Text("Sign in with Google", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SigningInContent() {
    Spacer(Modifier.height(64.dp))
    CircularProgressIndicator(modifier = Modifier.testTag("signing_in_progress"))
    Spacer(Modifier.height(16.dp))
    Text("Signing in…", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("sign_in_error"),
    )
    Spacer(Modifier.height(24.dp))
    Button(onClick = onRetry, modifier = Modifier.testTag("btn_google_sign_in")) {
        Text("Try again")
    }
}

@Composable
private fun SignedInContent(user: GoogleUser, onSignOut: () -> Unit) {
    Text(
        text = "Signed in as",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = "${user.firstName} ${user.lastName}".trim(),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.testTag("signed_in_name"),
    )
    Text(
        text = user.email,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.testTag("signed_in_email"),
    )
    Spacer(Modifier.height(32.dp))

    // Step 6 will render the server / client info table here.

    OutlinedButton(onClick = onSignOut, modifier = Modifier.testTag("btn_sign_out")) {
        Text("Sign out")
    }
}
