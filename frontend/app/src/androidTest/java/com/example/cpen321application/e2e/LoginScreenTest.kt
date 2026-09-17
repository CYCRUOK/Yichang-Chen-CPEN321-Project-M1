package com.example.cpen321application.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cpen321application.auth.GoogleAuthenticator
import com.example.cpen321application.auth.GoogleUser
import com.example.cpen321application.auth.SignInException
import com.example.cpen321application.ui.login.LoginScreen
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Login screen UI states, driven by a fake authenticator. The real Google
 * account picker is system UI and cannot be automated here; it is verified
 * manually on a device (see README "Google sign-in").
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val user = GoogleUser("yichang@example.com", "Yichang", "Chen", "token")

    private fun setLoginScreen(authenticator: GoogleAuthenticator) {
        composeRule.setContent {
            CPEN321ApplicationTheme {
                LoginScreen(onBack = {}, authenticator = authenticator)
            }
        }
    }

    @Test
    fun signedOut_showsSignInButton() {
        setLoginScreen(object : GoogleAuthenticator {
            override suspend fun signIn() = user
            override suspend fun signOut() = Unit
        })
        composeRule.onNodeWithTag("btn_google_sign_in").assertIsDisplayed()
    }

    @Test
    fun signIn_showsProgress_thenNameAndEmail() {
        val gate = CompletableDeferred<GoogleUser>()
        setLoginScreen(object : GoogleAuthenticator {
            override suspend fun signIn() = gate.await()
            override suspend fun signOut() = Unit
        })

        composeRule.onNodeWithTag("btn_google_sign_in").performClick()
        composeRule.onNodeWithTag("signing_in_progress").assertIsDisplayed()

        gate.complete(user)
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("signed_in_name").assertTextEquals("Yichang Chen")
        composeRule.onNodeWithTag("signed_in_email").assertTextEquals("yichang@example.com")
        composeRule.onNodeWithTag("btn_sign_out").assertIsDisplayed()
    }

    @Test
    fun signInFailure_showsMessage_andRetryWorks() {
        var attempts = 0
        setLoginScreen(object : GoogleAuthenticator {
            override suspend fun signIn(): GoogleUser {
                attempts++
                if (attempts == 1) throw SignInException("Sign-in cancelled")
                return user
            }
            override suspend fun signOut() = Unit
        })

        composeRule.onNodeWithTag("btn_google_sign_in").performClick()
        composeRule.onNodeWithTag("sign_in_error").assertTextEquals("Sign-in cancelled")

        composeRule.onNodeWithTag("btn_google_sign_in").performClick()
        composeRule.onNodeWithTag("signed_in_name").assertTextEquals("Yichang Chen")
    }

    @Test
    fun signOut_returnsToSignInButton() {
        setLoginScreen(object : GoogleAuthenticator {
            override suspend fun signIn() = user
            override suspend fun signOut() = Unit
        })

        composeRule.onNodeWithTag("btn_google_sign_in").performClick()
        composeRule.onNodeWithTag("btn_sign_out").performClick()

        composeRule.onNodeWithTag("btn_google_sign_in").assertIsDisplayed()
    }
}
