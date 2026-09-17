package com.example.cpen321application.e2e

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cpen321application.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Step 2 acceptance: home shows three independent buttons; each opens its own
 * screen; the system back button and the top-bar arrow both return home; and
 * rotating the device does not crash or lose the home screen.
 */
@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    /** Waits for the navigation transition instead of asserting mid-animation. */
    private fun expectScreen(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    @Test
    fun home_showsThreeButtons() {
        expectScreen("screen_home")
        composeRule.onNodeWithTag("btn_login").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_live").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_timer").assertIsDisplayed()
    }

    @Test
    fun loginButton_opensLoginScreen_systemBackReturnsHome() {
        composeRule.onNodeWithTag("btn_login").performClick()
        expectScreen("screen_login")

        Espresso.pressBack()
        expectScreen("screen_home")
    }

    @Test
    fun liveUpdatesButton_opensLiveScreen_topBarBackReturnsHome() {
        composeRule.onNodeWithTag("btn_live").performClick()
        expectScreen("screen_live")

        composeRule.onNodeWithTag("btn_back").performClick()
        expectScreen("screen_home")
    }

    @Test
    fun timerButton_opensTimerScreen_systemBackReturnsHome() {
        composeRule.onNodeWithTag("btn_timer").performClick()
        expectScreen("screen_timer")

        Espresso.pressBack()
        expectScreen("screen_home")
    }

    @Test
    fun screensAreIndependent_visitingAllThreeInSequence() {
        for (pair in listOf("btn_login" to "screen_login", "btn_live" to "screen_live", "btn_timer" to "screen_timer")) {
            composeRule.onNodeWithTag(pair.first).performClick()
            expectScreen(pair.second)
            Espresso.pressBack()
            expectScreen("screen_home")
        }
    }

    @Test
    fun rotation_keepsHomeScreen() {
        composeRule.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_login").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_timer").assertIsDisplayed()

        composeRule.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        composeRule.waitForIdle()
        expectScreen("screen_home")
    }
}
