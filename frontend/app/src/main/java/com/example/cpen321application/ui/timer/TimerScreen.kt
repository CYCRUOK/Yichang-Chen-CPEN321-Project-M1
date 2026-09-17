package com.example.cpen321application.ui.timer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cpen321application.timer.IssPosition
import com.example.cpen321application.timer.IssTracker
import com.example.cpen321application.timer.TimerAlerts
import com.example.cpen321application.timer.WaitFacts
import com.example.cpen321application.ui.components.FeatureScreen
import com.example.cpen321application.ui.timer.WorldOutline.drawContinents
import java.util.Locale

/** Button 3: countdown timer; when it goes off, the "While You Waited" surprise. */
@Composable
fun TimerScreen(
    onBack: () -> Unit,
    issTracker: IssTracker,
    viewModel: TimerViewModel = viewModel { TimerViewModel(issTracker) },
    alertsEnabled: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Vibrate + notify every time the timer fires (also when the app is in the background).
    LaunchedEffect(viewModel) {
        viewModel.firedEvents.collect {
            if (alertsEnabled) {
                TimerAlerts.vibrate(context)
                TimerAlerts.notifyTimerFinished(context)
            }
        }
    }

    FeatureScreen(title = "Timer", testTag = "screen_timer", onBack = onBack) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (val state = uiState) {
                    TimerUiState.Idle -> IdleContent(onStart = viewModel::start, askNotificationPermission = alertsEnabled)
                    is TimerUiState.Running -> RunningContent(state, onCancel = viewModel::cancel)
                    is TimerUiState.Fired -> FiredContent(state, onReset = viewModel::reset)
                }
            }
            if (uiState is TimerUiState.Fired) {
                Confetti(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Idle: minutes / seconds input
// ---------------------------------------------------------------------------

@Composable
private fun IdleContent(
    onStart: (minutes: Int, seconds: Int) -> Unit,
    askNotificationPermission: Boolean,
) {
    var minutesText by rememberSaveable { mutableStateOf("0") }
    var secondsText by rememberSaveable { mutableStateOf("10") }
    val minutes = minutesText.toIntOrNull() ?: 0
    val seconds = secondsText.toIntOrNull() ?: 0
    val valid = minutes >= 0 && seconds in 0..59 && (minutes > 0 || seconds > 0)

    // Android 13+ needs runtime permission to post the "time's up" notification.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onStart(minutes, seconds) }
    val context = LocalContext.current

    Spacer(Modifier.height(24.dp))
    Text("Set a timer", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(
        "When it goes off, you'll see what happened in the world while you waited.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(32.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = minutesText,
            onValueChange = { minutesText = it.filter(Char::isDigit).take(3) },
            label = { Text("Minutes") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(120.dp)
                .testTag("input_minutes"),
        )
        Text("  :  ", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(
            value = secondsText,
            onValueChange = { secondsText = it.filter(Char::isDigit).take(2) },
            label = { Text("Seconds") },
            singleLine = true,
            isError = seconds > 59,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .width(120.dp)
                .testTag("input_seconds"),
        )
    }
    if (seconds > 59) {
        Text("Seconds must be 0–59", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
    Spacer(Modifier.height(32.dp))

    Button(
        onClick = {
            if (askNotificationPermission &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !TimerAlerts.canNotify(context)
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                onStart(minutes, seconds)
            }
        },
        enabled = valid,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("btn_start_timer"),
    ) {
        Text("Start", style = MaterialTheme.typography.titleMedium)
    }
}

// ---------------------------------------------------------------------------
// Running: countdown
// ---------------------------------------------------------------------------

@Composable
private fun RunningContent(state: TimerUiState.Running, onCancel: () -> Unit) {
    Spacer(Modifier.height(48.dp))
    Text(
        text = formatCountdown(state.remainingMs),
        fontSize = 72.sp,
        fontWeight = FontWeight.Light,
        modifier = Modifier.testTag("countdown_text"),
    )
    Spacer(Modifier.height(24.dp))
    LinearProgressIndicator(
        progress = { 1f - state.remainingMs.toFloat() / state.totalMs.toFloat() },
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(48.dp))
    OutlinedButton(onClick = onCancel, modifier = Modifier.testTag("btn_cancel_timer")) {
        Text("Cancel")
    }
}

/** Remaining time as mm:ss, rounding up so the display never shows 00:00 while running. */
internal fun formatCountdown(remainingMs: Long): String {
    val totalSeconds = (remainingMs + 999L) / 1000L
    return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}

// ---------------------------------------------------------------------------
// Fired: the surprise
// ---------------------------------------------------------------------------

@Composable
private fun FiredContent(state: TimerUiState.Fired, onReset: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    Text("🎉 Time's up!", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag("surprise_title"))
    Spacer(Modifier.height(4.dp))
    Text(
        "While you waited ${formatElapsed(state.elapsedSeconds)}…",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(20.dp))

    val facts = state.facts
    if (facts == null) {
        CircularProgressIndicator(modifier = Modifier.testTag("facts_loading"))
        Spacer(Modifier.height(8.dp))
        Text("Asking the Space Station where it is…", style = MaterialTheme.typography.bodySmall)
    } else {
        FactsList(facts)
    }

    Spacer(Modifier.height(32.dp))
    Button(onClick = onReset, modifier = Modifier.testTag("btn_new_timer")) {
        Text("Set another timer")
    }
}

@Composable
private fun FactsList(facts: WaitFacts) {
    Column(modifier = Modifier.testTag("facts_list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (facts.issDistanceKm != null) {
            FactCard(
                emoji = "🛰️",
                title = "The Space Station flew ${formatKm(facts.issDistanceKm)}",
                body = issRoute(facts),
            ) {
                if (facts.issStart != null && facts.issEnd != null) {
                    Spacer(Modifier.height(12.dp))
                    IssMiniMap(facts.issStart, facts.issEnd)
                }
            }
        } else {
            FactCard(emoji = "🛰️", title = "The Space Station kept orbiting", body = "(couldn't reach the ISS tracker — offline?)")
        }
        FactCard("🌍", "Earth travelled ${formatKm(facts.earthOrbitKm)}", "along its orbit around the Sun, at 29.8 km/s")
        FactCard(
            "💡",
            "Light travelled ${formatKm(facts.lightKm)}",
            "enough for ${String.format(Locale.US, "%.1f", facts.moonRoundTrips)} round trips to the Moon",
        )
        FactCard(
            "☀️",
            if (facts.sunlightArrived) "Sunlight that left the Sun when you pressed Start has now reached you"
            else "Sunlight that left the Sun when you pressed Start is still on its way",
            "it takes about 8 min 20 s to get here",
        )
        FactCard("❤️", "Your heart beat about ${times(facts.heartbeats)}", "and you blinked about ${times(facts.blinks)}")
        FactCard("👶", "About ${formatCount(facts.babiesBorn)} babies were born worldwide", "roughly 4.4 every second")
    }
}

@Composable
private fun FactCard(emoji: String, title: String, body: String, extra: @Composable () -> Unit = {}) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(emoji, fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            extra()
        }
    }
}

/** Equirectangular mini world map with lat/long grid and the ISS start -> end hop. */
@Composable
private fun IssMiniMap(start: IssPosition, end: IssPosition) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val landColor = MaterialTheme.colorScheme.secondaryContainer
    val seaColor = MaterialTheme.colorScheme.surface
    val startColor = MaterialTheme.colorScheme.onSurfaceVariant
    val endColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .testTag("iss_map"),
    ) {
        fun project(lat: Double, lon: Double) =
            Offset(((lon + 180.0) / 360.0 * size.width).toFloat(), ((90.0 - lat) / 180.0 * size.height).toFloat())

        drawRect(color = seaColor)
        drawContinents(landColor)
        for (lon in -180..180 step 30) {
            val x = ((lon + 180) / 360f) * size.width
            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        }
        for (lat in -90..90 step 30) {
            val y = ((90 - lat) / 180f) * size.height
            drawLine(if (lat == 0) startColor else gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = if (lat == 0) 2f else 1f)
        }
        val a = project(start.latitude, start.longitude)
        val b = project(end.latitude, end.longitude)
        drawLine(endColor, a, b, strokeWidth = 3f)
        drawCircle(startColor, radius = 7f, center = a)
        drawCircle(endColor, radius = 9f, center = b)
        drawCircle(Color.White, radius = 4f, center = b)
    }
}

private fun issRoute(facts: WaitFacts): String {
    val s = facts.issStart
    val e = facts.issEnd
    return when {
        s != null && e != null -> "from ${formatLatLon(s)} to ${formatLatLon(e)}, ${formatKm(e.altitudeKm)} up at ${formatCount(e.velocityKmh.toLong())} km/h"
        e != null -> "now over ${formatLatLon(e)} at ${formatCount(e.velocityKmh.toLong())} km/h"
        s != null -> "was over ${formatLatLon(s)} when you started"
        else -> ""
    }
}

internal fun formatLatLon(p: IssPosition): String {
    val ns = if (p.latitude >= 0) "N" else "S"
    val ew = if (p.longitude >= 0) "E" else "W"
    return String.format(Locale.US, "%.1f°%s %.1f°%s", kotlin.math.abs(p.latitude), ns, kotlin.math.abs(p.longitude), ew)
}

internal fun formatKm(km: Double): String = when {
    km >= 1_000_000 -> String.format(Locale.US, "%.1f million km", km / 1_000_000)
    km >= 100 -> String.format(Locale.US, "%,d km", km.toLong())
    else -> String.format(Locale.US, "%.1f km", km)
}

internal fun formatCount(n: Long): String = String.format(Locale.US, "%,d", n)

internal fun times(n: Long): String = if (n == 1L) "1 time" else "${formatCount(n)} times"

internal fun formatElapsed(seconds: Long): String = when {
    seconds >= 60 -> String.format(Locale.US, "%d min %d s", seconds / 60, seconds % 60)
    else -> "$seconds s"
}
