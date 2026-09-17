package com.example.cpen321application.ui.live

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cpen321application.live.PixelStream
import com.example.cpen321application.ui.components.FeatureScreen

/** Button 2: the pixel-art image assembling live from the backend's WebSocket relay. */
@Composable
fun LiveUpdatesScreen(
    onBack: () -> Unit,
    stream: PixelStream,
    viewModel: LiveUpdatesViewModel = viewModel { LiveUpdatesViewModel(stream) },
) {
    val uiState by viewModel.uiState.collectAsState()

    FeatureScreen(title = "Live Updates", testTag = "screen_live", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Pixel Art", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            PixelGrid(
                cells = uiState.cells,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .testTag("pixel_grid")
                    .semantics { stateDescription = "${uiState.pixelsThisImage} pixels painted" },
            )

            Spacer(Modifier.height(16.dp))
            ConnectionRow(uiState)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${uiState.pixelsThisImage} / ${GRID_SIZE * GRID_SIZE} pixels · image #${uiState.imagesCompleted + 1}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("pixel_counter"),
            )
        }
    }
}

@Composable
private fun ConnectionRow(state: LiveUiState) {
    val (label, color) = when (state.connection) {
        ConnectionState.CONNECTING -> "Connecting…" to MaterialTheme.colorScheme.outline
        ConnectionState.CONNECTED -> "Live" to Color(0xFF2E7D32)
        ConnectionState.RECONNECTING -> "Reconnecting… (${state.lastError ?: "connection lost"})" to MaterialTheme.colorScheme.error
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(10.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.padding(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("connection_status"))
    }
}

/** 16x16 grid; unpainted cells stay blank (canvas colour) so the image visibly emerges. */
@Composable
fun PixelGrid(cells: List<String?>, modifier: Modifier = Modifier) {
    val blank = MaterialTheme.colorScheme.surfaceVariant
    val gridLine = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = modifier) {
        val cell = size.width / GRID_SIZE
        drawRect(blank)
        for (y in 0 until GRID_SIZE) {
            for (x in 0 until GRID_SIZE) {
                val hex = cells[y * GRID_SIZE + x] ?: continue
                drawRect(
                    color = parseHexColor(hex),
                    topLeft = Offset(x * cell, y * cell),
                    size = Size(cell, cell),
                )
            }
        }
        for (i in 0..GRID_SIZE) {
            val p = i * cell
            drawLine(gridLine, Offset(p, 0f), Offset(p, size.height), strokeWidth = 1f)
            drawLine(gridLine, Offset(0f, p), Offset(size.width, p), strokeWidth = 1f)
        }
    }
}

internal fun parseHexColor(hex: String): Color =
    Color(0xFF000000L or hex.removePrefix("#").toLong(16))
