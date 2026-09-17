package com.example.cpen321application.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Common chrome for the three feature screens: a top bar with a title and a
 * back arrow, and a content slot. Each feature screen owns its own content so
 * the three buttons stay fully independent of one another.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureScreen(
    title: String,
    testTag: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag(testTag),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            content()
        }
    }
}
