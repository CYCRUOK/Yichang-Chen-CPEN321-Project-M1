package com.example.cpen321application.ui.live

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

/** Button 2: Live Updates. Placeholder content; the real feature is filled in later. */
@Composable
fun LiveUpdatesScreen(onBack: () -> Unit) {
    FeatureScreen(title = "Live Updates", testTag = "screen_live", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Live Updates — coming soon",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
