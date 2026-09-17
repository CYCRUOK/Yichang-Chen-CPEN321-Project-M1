package com.example.cpen321application.ui.timer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

private val CONFETTI_COLORS = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835),
    Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA),
)

private class Particle(
    val x0: Float, val vy: Float, val sway: Float, val phase: Float,
    val w: Float, val h: Float, val color: Color, val delay: Float,
)

/** Falling confetti drawn on a Canvas for [durationMs], then it stops itself. */
@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 90, durationMs: Long = 4_500L) {
    val particles = remember {
        val rnd = Random(42)
        List(count) {
            Particle(
                x0 = rnd.nextFloat(),
                vy = 0.25f + rnd.nextFloat() * 0.35f,      // screen heights per second
                sway = 0.02f + rnd.nextFloat() * 0.05f,
                phase = rnd.nextFloat() * 6.28f,
                w = 8f + rnd.nextFloat() * 10f,
                h = 4f + rnd.nextFloat() * 8f,
                color = CONFETTI_COLORS[rnd.nextInt(CONFETTI_COLORS.size)],
                delay = rnd.nextFloat() * 1.2f,
            )
        }
    }
    var elapsedMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (elapsedMs < durationMs) {
            withFrameNanos { now -> elapsedMs = (now - start) / 1_000_000L }
        }
    }
    if (elapsedMs >= durationMs) return

    Canvas(modifier = modifier) {
        val t = elapsedMs / 1000f
        for (p in particles) {
            val life = t - p.delay
            if (life < 0f) continue
            val y = life * p.vy * size.height
            if (y > size.height) continue
            val x = (p.x0 + p.sway * sin(life * 3f + p.phase)) * size.width
            drawRect(
                color = p.color,
                topLeft = Offset(x, y),
                size = Size(p.w, p.h),
                alpha = 0.9f,
            )
        }
    }
}
