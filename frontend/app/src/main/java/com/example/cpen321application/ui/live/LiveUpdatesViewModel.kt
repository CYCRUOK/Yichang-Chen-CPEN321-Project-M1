package com.example.cpen321application.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cpen321application.live.Pixel
import com.example.cpen321application.live.PixelEvent
import com.example.cpen321application.live.PixelStream
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val GRID_SIZE = 16

enum class ConnectionState { CONNECTING, CONNECTED, RECONNECTING }

/**
 * @param cells 16x16 colours in row-major order (index = y * 16 + x); null = not painted yet.
 * @param imagesCompleted how many blank-canvas restarts have happened since opening the screen.
 */
data class LiveUiState(
    val cells: List<String?> = List(GRID_SIZE * GRID_SIZE) { null },
    val connection: ConnectionState = ConnectionState.CONNECTING,
    val lastError: String? = null,
    val pixelsThisImage: Int = 0,
    val imagesCompleted: Int = 0,
)

/**
 * Paints each update onto the grid the moment it arrives. The course server
 * does not send a "new image" marker; it just pauses ~5 s between images, so
 * a gap longer than [newImageGapMs] clears the canvas before the next pixel.
 */
class LiveUpdatesViewModel(
    private val stream: PixelStream,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newImageGapMs: Long = 2_000L,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LiveUiState())
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    private var lastPixelAt = 0L
    private var job: Job? = null

    init {
        connect()
    }

    private fun connect() {
        job?.cancel()
        job = viewModelScope.launch {
            stream.events().collect { event ->
                when (event) {
                    PixelEvent.Connected -> _uiState.update {
                        it.copy(connection = ConnectionState.CONNECTED, lastError = null)
                    }
                    is PixelEvent.Disconnected -> _uiState.update {
                        it.copy(connection = ConnectionState.RECONNECTING, lastError = event.reason)
                    }
                    is PixelEvent.Update -> paint(event.pixel)
                }
            }
        }
    }

    private fun paint(pixel: Pixel) {
        val now = clock()
        val startNewImage = lastPixelAt != 0L && now - lastPixelAt > newImageGapMs
        lastPixelAt = now
        _uiState.update { state ->
            val base = if (startNewImage) {
                state.copy(
                    cells = List(GRID_SIZE * GRID_SIZE) { null },
                    pixelsThisImage = 0,
                    imagesCompleted = state.imagesCompleted + 1,
                )
            } else {
                state
            }
            val cells = base.cells.toMutableList()
            cells[pixel.y * GRID_SIZE + pixel.x] = pixel.color
            base.copy(cells = cells, pixelsThisImage = base.pixelsThisImage + 1)
        }
    }
}
