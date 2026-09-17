package com.example.cpen321application.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cpen321application.timer.IssPosition
import com.example.cpen321application.timer.IssTracker
import com.example.cpen321application.ui.timer.TimerScreen
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Timer flow end to end with a fake ISS tracker: input -> countdown -> surprise.
 * Vibration / notification are disabled here; they are checked manually on a device.
 */
@RunWith(AndroidJUnit4::class)
class TimerScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fakeTracker = object : IssTracker {
        private var calls = 0
        override suspend fun currentPosition() =
            if (calls++ == 0) IssPosition(49.28, -123.12, 420.0, 27_600.0)
            else IssPosition(51.05, -114.07, 420.0, 27_600.0)
    }

    private fun setTimerScreen() {
        composeRule.setContent {
            CPEN321ApplicationTheme {
                TimerScreen(onBack = {}, issTracker = fakeTracker, alertsEnabled = false)
            }
        }
    }

    private fun enterDuration(minutes: String, seconds: String) {
        composeRule.onNodeWithTag("input_minutes").performTextClearance()
        composeRule.onNodeWithTag("input_minutes").performTextInput(minutes)
        composeRule.onNodeWithTag("input_seconds").performTextClearance()
        composeRule.onNodeWithTag("input_seconds").performTextInput(seconds)
    }

    @Test
    fun startIsDisabledForZeroOrInvalidDurations() {
        setTimerScreen()
        enterDuration("0", "0")
        composeRule.onNodeWithTag("btn_start_timer").assertIsNotEnabled()

        enterDuration("0", "75")
        composeRule.onNodeWithTag("btn_start_timer").assertIsNotEnabled()

        enterDuration("1", "30")
        composeRule.onNodeWithTag("btn_start_timer").assertIsEnabled()
    }

    @Test
    fun countdownShowsRemainingTime_andCancelReturnsToInput() {
        setTimerScreen()
        enterDuration("2", "30")
        composeRule.onNodeWithTag("btn_start_timer").performClick()

        composeRule.onNodeWithTag("countdown_text").assertTextEquals("02:30")
        composeRule.onNodeWithTag("btn_cancel_timer").performClick()
        composeRule.onNodeWithTag("btn_start_timer").assertIsDisplayed()
    }

    @Test
    fun timerFires_thenShowsWhileYouWaitedFacts() {
        setTimerScreen()
        enterDuration("0", "2")
        composeRule.onNodeWithTag("btn_start_timer").performClick()
        composeRule.onNodeWithTag("countdown_text").assertIsDisplayed()

        // The real clock drives the countdown: wait a little longer than 2 s.
        composeRule.waitUntil(timeoutMillis = 6_000) {
            composeRule.onAllNodesWithTag("surprise_title").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag("facts_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("iss_map").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("btn_new_timer").performScrollTo().performClick()
        composeRule.onNodeWithTag("btn_start_timer").assertIsDisplayed()
    }
}
