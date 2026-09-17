package com.example.cpen321application.e2e

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cpen321application.live.Pixel
import com.example.cpen321application.live.PixelEvent
import com.example.cpen321application.live.PixelStream
import com.example.cpen321application.ui.live.LiveUpdatesScreen
import com.example.cpen321application.ui.theme.CPEN321ApplicationTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Live Updates screen driven by a scripted pixel stream (no network). */
@RunWith(AndroidJUnit4::class)
class LiveUpdatesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val events = MutableSharedFlow<PixelEvent>(extraBufferCapacity = 64)
    private val stream = object : PixelStream {
        override fun events(): Flow<PixelEvent> = events
    }

    private fun stateDescription(expected: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, expected)

    @Test
    fun blankGrid_thenPixelsPaintAsTheyArrive() {
        composeRule.setContent {
            CPEN321ApplicationTheme { LiveUpdatesScreen(onBack = {}, stream = stream) }
        }

        composeRule.onNodeWithTag("pixel_grid").assertIsDisplayed().assert(stateDescription("0 pixels painted"))
        composeRule.onNodeWithTag("connection_status").assertTextEquals("Connecting…")

        events.tryEmit(PixelEvent.Connected)
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("connection_status").assertTextEquals("Live")

        events.tryEmit(PixelEvent.Update(Pixel(0, 0, "#FF0000")))
        events.tryEmit(PixelEvent.Update(Pixel(15, 15, "#00FF00")))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pixel_grid").assert(stateDescription("2 pixels painted"))
        composeRule.onNodeWithTag("pixel_counter").assertTextEquals("2 / 256 pixels · image #1")
    }

    @Test
    fun disconnect_showsReconnecting() {
        composeRule.setContent {
            CPEN321ApplicationTheme { LiveUpdatesScreen(onBack = {}, stream = stream) }
        }
        events.tryEmit(PixelEvent.Connected)
        events.tryEmit(PixelEvent.Disconnected("EOF"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("connection_status").assertTextEquals("Reconnecting… (EOF)")
    }
}
