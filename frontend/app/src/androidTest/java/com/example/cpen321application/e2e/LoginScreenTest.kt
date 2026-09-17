package com.example.cpen321application.e2e

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cpen321application.auth.GoogleAuthenticator
import com.example.cpen321application.auth.GoogleUser
import com.example.cpen321application.auth.SignInException
import com.example.cpen321application.info.InfoApi
import com.example.cpen321application.info.InfoApiException
import com.example.cpen321application.info.OwnerName
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

    private class FakeInfoApi(private val ip: () -> String = { "142.250.217.110" }) : InfoApi {
        override suspend fun serverIp() = ip()
        override suspend fun serverTime() = "12:18:22 GMT+01:00"
        override suspend fun ownerName() = OwnerName("Yichang", "Chen")
    }

    private fun setLoginScreen(authenticator: GoogleAuthenticator, infoApi: InfoApi = FakeInfoApi()) {
        composeRule.setContent {
            CPEN321ApplicationTheme {
                LoginScreen(onBack = {}, authenticator = authenticator, infoApi = infoApi)
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

    @Test
    fun afterSignIn_infoTableShowsAllSixValues() {
        setLoginScreen(object : GoogleAuthenticator {
            override suspend fun signIn() = user
            override suspend fun signOut() = Unit
        })
        composeRule.onNodeWithTag("btn_google_sign_in").performClick()

        composeRule.onNodeWithTag("info_server_ip").assertTextEquals("142.250.217.110")
        composeRule.onNodeWithTag("info_server_time").assertTextEquals("12:18:22 GMT+01:00")
        composeRule.onNodeWithTag("info_client_time").assert(hasTextMatching(Regex("""\d{2}:\d{2}:\d{2} GMT[+-]\d{2}:\d{2}""")))
        composeRule.onNodeWithTag("info_client_ip").assert(hasTextMatching(Regex(""".+""")))
        composeRule.onNodeWithTag("info_owner_name").assertTextEquals("Yichang Chen")
        composeRule.onNodeWithTag("info_user_name").assertTextEquals("Yichang Chen")
    }

    @Test
    fun serverUnreachable_showsErrorAndRetryRecovers() {
        var attempts = 0
        setLoginScreen(
            object : GoogleAuthenticator {
                override suspend fun signIn() = user
                override suspend fun signOut() = Unit
            },
            FakeInfoApi(ip = { if (attempts++ == 0) throw InfoApiException("/api/server-ip: timeout") else "1.2.3.4" }),
        )
        composeRule.onNodeWithTag("btn_google_sign_in").performClick()
        composeRule.onNodeWithTag("info_error").assertTextEquals("/api/server-ip: timeout")
        composeRule.onNodeWithTag("signed_in_name").assertTextEquals("Yichang Chen")

        composeRule.onNodeWithTag("btn_refresh").performClick()
        composeRule.onNodeWithTag("info_server_ip").assertTextEquals("1.2.3.4")
    }

    private fun hasTextMatching(regex: Regex) = SemanticsMatcher("text matches $regex") { node ->
        node.config.getOrNull(SemanticsProperties.Text)?.any { regex.matches(it.text) } == true
    }
}
