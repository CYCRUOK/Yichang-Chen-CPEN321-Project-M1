package com.example.cpen321application.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/** The subset of a Google account we show in the app. */
data class GoogleUser(
    val email: String,
    val firstName: String,
    val lastName: String,
    val idToken: String,
)

/** Thrown by [GoogleAuthenticator.signIn] with a message safe to show to the user. */
class SignInException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Abstraction over the Google sign-in flow so the login UI and view model can
 * be tested with a fake instead of the real Credential Manager system dialog.
 */
interface GoogleAuthenticator {
    /** Opens the Google account picker and returns the chosen account. */
    suspend fun signIn(): GoogleUser

    /** Clears any cached credential state so the next [signIn] shows the picker again. */
    suspend fun signOut()
}

/** Real implementation backed by Jetpack Credential Manager + Sign in with Google. */
class CredentialManagerGoogleAuthenticator(
    private val context: Context,
    private val serverClientId: String,
) : GoogleAuthenticator {

    private val credentialManager = CredentialManager.create(context)

    override suspend fun signIn(): GoogleUser {
        if (serverClientId.isBlank()) {
            throw SignInException("GOOGLE_CLIENT_ID is not set in frontend/local.properties")
        }

        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = try {
            // `context` must be an Activity so the system sheet can be shown.
            credentialManager.getCredential(context, request)
        } catch (e: GetCredentialCancellationException) {
            // Also what Credential Manager reports when this APK's signing key is not
            // registered as an Android OAuth client (developer error 10).
            throw SignInException("Sign-in cancelled, or this build's signing key is not registered in Google Cloud Console", e)
        } catch (e: NoCredentialException) {
            throw SignInException("No Google account available on this device", e)
        } catch (e: GetCredentialException) {
            throw SignInException("Google sign-in failed: ${e.message ?: e.type}", e)
        }

        return response.credential.toGoogleUser()
    }

    override suspend fun signOut() {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}

/** Converts the credential returned by Credential Manager into our [GoogleUser]. */
internal fun androidx.credentials.Credential.toGoogleUser(): GoogleUser {
    if (this !is CustomCredential || type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        throw SignInException("Unexpected credential type: $type")
    }
    val googleCredential = try {
        GoogleIdTokenCredential.createFrom(data)
    } catch (e: GoogleIdTokenParsingException) {
        throw SignInException("Could not parse Google credential", e)
    }
    return GoogleUser(
        email = googleCredential.id,
        firstName = googleCredential.givenName ?: "",
        lastName = googleCredential.familyName ?: "",
        idToken = googleCredential.idToken,
    )
}
