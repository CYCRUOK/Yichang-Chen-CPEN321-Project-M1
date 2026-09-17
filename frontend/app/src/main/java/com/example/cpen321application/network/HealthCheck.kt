package com.example.cpen321application.network

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Calls GET {apiBaseUrl}/health and returns a one-line human-readable status. */
suspend fun fetchHealthStatus(apiBaseUrl: String): String = withContext(Dispatchers.IO) {
    val healthUrl = "${apiBaseUrl.trimEnd('/')}/health"
    try {
        val connection = (URL(healthUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
        }

        when (val code = connection.responseCode) {
            HttpURLConnection.HTTP_OK -> {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                "Backend healthy ($healthUrl): $body"
            }
            else -> {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                "Backend error ($healthUrl): HTTP $code${errorBody?.let { " — $it" } ?: ""}"
            }
        }
    } catch (e: Exception) {
        "Backend unreachable ($healthUrl): ${e.message ?: e.javaClass.simpleName}"
    }
}
