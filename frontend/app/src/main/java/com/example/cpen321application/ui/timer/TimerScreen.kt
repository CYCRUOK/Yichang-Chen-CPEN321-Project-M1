package com.example.cpen321application.ui.timer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cpen321application.ui.components.FeatureScreen

/** Button 3: Timer. Placeholder content; the real feature is filled in later. */
@Composable
fun TimerScreen(onBack: () -> Unit) {
    FeatureScreen(title = "Timer", testTag = "screen_timer", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Timer — coming soon",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
