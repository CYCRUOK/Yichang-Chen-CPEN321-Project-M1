package com.example.cpen321application.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.cpen321application.network.fetchHealthStatus

/**
 * Home screen: the three independent feature buttons required by M1, plus a
 * small backend health line at the bottom used during development.
 */
@Composable
fun HomeScreen(
    apiBaseUrl: String,
    onLoginClick: () -> Unit,
    onLiveUpdatesClick: () -> Unit,
    onTimerClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_home"),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "CPEN 321 M1",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(48.dp))

            HomeButton(label = "Login + Server", tag = "btn_login", onClick = onLoginClick)
            Spacer(Modifier.height(24.dp))
            HomeButton(label = "Live Updates", tag = "btn_live", onClick = onLiveUpdatesClick)
            Spacer(Modifier.height(24.dp))
            HomeButton(label = "Timer", tag = "btn_timer", onClick = onTimerClick)

            Spacer(Modifier.height(48.dp))
            BackendStatus(apiBaseUrl = apiBaseUrl)
        }
    }
}

@Composable
private fun HomeButton(label: String, tag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag(tag),
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BackendStatus(apiBaseUrl: String) {
    var statusText by remember { mutableStateOf("Checking backend at $apiBaseUrl ...") }

    LaunchedEffect(apiBaseUrl) {
        statusText = fetchHealthStatus(apiBaseUrl)
    }

    Text(
        text = statusText,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.testTag("backend_status"),
    )
}
