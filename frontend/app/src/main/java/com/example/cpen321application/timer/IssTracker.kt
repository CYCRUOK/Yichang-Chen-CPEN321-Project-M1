package com.example.cpen321application.timer

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** A snapshot of where the International Space Station is. */
data class IssPosition(
    val latitude: Double,
    val longitude: Double,
    val altitudeKm: Double,
    val velocityKmh: Double,
)

/** Abstraction over the ISS position lookup so the timer can be tested offline. */
interface IssTracker {
    /** Returns the current position, or null if it cannot be determined (offline, API down). */
    suspend fun currentPosition(): IssPosition?
}

/** Real lookup via the free, key-less https://wheretheiss.at API. */
class WhereTheIssAtTracker : IssTracker {
    override suspend fun currentPosition(): IssPosition? = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5_000
                readTimeout = 5_000
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            IssPosition(
                latitude = json.getDouble("latitude"),
                longitude = json.getDouble("longitude"),
                altitudeKm = json.getDouble("altitude"),
                velocityKmh = json.getDouble("velocity"),
            )
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val ENDPOINT = "https://api.wheretheiss.at/v1/satellites/25544"
    }
}
