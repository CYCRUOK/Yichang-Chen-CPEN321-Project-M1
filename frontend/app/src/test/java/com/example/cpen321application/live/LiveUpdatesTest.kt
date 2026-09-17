package com.example.cpen321application.live

import com.example.cpen321application.ui.live.ConnectionState
import com.example.cpen321application.ui.live.GRID_SIZE
import com.example.cpen321application.ui.live.LiveUpdatesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PixelParsingTest {
    @Test
    fun `parses the course server format`() {
        assertEquals(Pixel(2, 15, "#FFFFFF"), parsePixel("""{"x":2,"y":15,"color":"#FFFFFF"}"""))
        assertEquals(Pixel(6, 12, "#ffd23f"), parsePixel("""{"x":6,"y":12,"color":"#ffd23f"}"""))
    }

    @Test
    fun `rejects malformed or out-of-range updates`() {
        assertNull(parsePixel("not json"))
        assertNull(parsePixel("""{"x":16,"y":0,"color":"#000000"}"""))
        assertNull(parsePixel("""{"x":0,"y":-1,"color":"#000000"}"""))
        assertNull(parsePixel("""{"x":0,"y":0,"color":"red"}"""))
        assertNull(parsePixel("""{"x":0,"y":0}"""))
    }

    @Test
    fun `derives the relay URL from the API base URL`() {
        assertEquals("ws://10.0.2.2:3000/ws", relayWebSocketUrl("http://10.0.2.2:3000"))
        assertEquals("ws://localhost:3000/ws", relayWebSocketUrl("http://localhost:3000/"))
        assertEquals("wss://example.duckdns.org/ws", relayWebSocketUrl("https://example.duckdns.org"))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LiveUpdatesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private var now = 1_000_000L
    private val events = MutableSharedFlow<PixelEvent>(extraBufferCapacity = 64)
    private val stream = object : PixelStream {
        override fun events(): Flow<PixelEvent> = events
    }

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun newViewModel() = LiveUpdatesViewModel(stream, clock = { now }, newImageGapMs = 2_000)

    @Test
    fun `starts blank and connecting, then connected`() = runTest(dispatcher) {
        val vm = newViewModel()
        runCurrent()
        assertEquals(ConnectionState.CONNECTING, vm.uiState.value.connection)
        assertEquals(GRID_SIZE * GRID_SIZE, vm.uiState.value.cells.size)
        assertEquals(0, vm.uiState.value.cells.count { it != null })

        events.emit(PixelEvent.Connected)
        runCurrent()
        assertEquals(ConnectionState.CONNECTED, vm.uiState.value.connection)
    }

    @Test
    fun `paints each update at the right cell as it arrives`() = runTest(dispatcher) {
        val vm = newViewModel()
        runCurrent()

        events.emit(PixelEvent.Update(Pixel(2, 15, "#FFFFFF")))
        runCurrent()
        assertEquals("#FFFFFF", vm.uiState.value.cells[15 * GRID_SIZE + 2])
        assertEquals(1, vm.uiState.value.pixelsThisImage)

        now += 50
        events.emit(PixelEvent.Update(Pixel(2, 15, "#000000")))
        runCurrent()
        assertEquals("#000000", vm.uiState.value.cells[15 * GRID_SIZE + 2])
        assertEquals(1, vm.uiState.value.cells.count { it != null })
    }

    @Test
    fun `a pause longer than the gap clears the canvas for the next image`() = runTest(dispatcher) {
        val vm = newViewModel()
        runCurrent()
        events.emit(PixelEvent.Update(Pixel(0, 0, "#111111")))
        now += 50
        events.emit(PixelEvent.Update(Pixel(1, 0, "#222222")))
        runCurrent()
        assertEquals(2, vm.uiState.value.pixelsThisImage)

        now += 5_050 // the server's ~5 s pause
        events.emit(PixelEvent.Update(Pixel(5, 5, "#333333")))
        runCurrent()

        val state = vm.uiState.value
        assertEquals(1, state.imagesCompleted)
        assertEquals(1, state.pixelsThisImage)
        assertNull(state.cells[0])
        assertEquals("#333333", state.cells[5 * GRID_SIZE + 5])
    }

    @Test
    fun `disconnect shows reconnecting but keeps the picture`() = runTest(dispatcher) {
        val vm = newViewModel()
        runCurrent()
        events.emit(PixelEvent.Connected)
        events.emit(PixelEvent.Update(Pixel(3, 3, "#abcdef")))
        events.emit(PixelEvent.Disconnected("EOF"))
        runCurrent()

        assertEquals(ConnectionState.RECONNECTING, vm.uiState.value.connection)
        assertEquals("EOF", vm.uiState.value.lastError)
        assertEquals("#abcdef", vm.uiState.value.cells[3 * GRID_SIZE + 3])
    }
}
