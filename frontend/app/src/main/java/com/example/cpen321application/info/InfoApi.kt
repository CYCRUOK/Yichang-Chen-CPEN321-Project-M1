package com.example.cpen321application.info

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** The developer's name as served by GET /api/name. */
data class OwnerName(val first: String, val last: String)

/** Thrown with a user-presentable message when a backend call fails. */
class InfoApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** The three Button-1 backend APIs. */
interface InfoApi {
    suspend fun serverIp(): String
    suspend fun serverTime(): String
    suspend fun ownerName(): OwnerName
}

/** HttpURLConnection implementation against `apiBaseUrl` (http or https). */
class HttpInfoApi(private val apiBaseUrl: String) : InfoApi {

    override suspend fun serverIp(): String = getJson("/api/server-ip").getString("ip")

    override suspend fun serverTime(): String = getJson("/api/server-time").getString("time")

    override suspend fun ownerName(): OwnerName = getJson("/api/name").let {
        OwnerName(first = it.getString("first"), last = it.getString("last"))
    }

    private suspend fun getJson(path: String): JSONObject = withContext(Dispatchers.IO) {
        val url = apiBaseUrl.trimEnd('/') + path
        try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8_000
                readTimeout = 8_000
            }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw InfoApiException("$path: HTTP $code${body?.let { " $it" } ?: ""}")
            }
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } catch (e: InfoApiException) {
            throw e
        } catch (e: Exception) {
            throw InfoApiException("$path: ${e.message ?: e.javaClass.simpleName}", e)
        }
    }
}
