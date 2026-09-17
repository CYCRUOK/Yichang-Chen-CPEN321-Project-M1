package com.example.cpen321application.live

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.emitAll
import org.json.JSONObject

/** One cell update on the 16x16 grid, exactly as the course server sends it. */
data class Pixel(val x: Int, val y: Int, val color: String)

sealed interface PixelEvent {
    data object Connected : PixelEvent
    data class Disconnected(val reason: String) : PixelEvent
    data class Update(val pixel: Pixel) : PixelEvent
}

/** Source of pixel updates; the real one is our backend's /ws relay. */
interface PixelStream {
    /** Emits events until the collector cancels; reconnects on its own after a drop. */
    fun events(): Flow<PixelEvent>
}

/** Parses `{"x":<int>,"y":<int>,"color":"<hex>"}`; returns null for anything else. */
fun parsePixel(text: String): Pixel? = try {
    val json = JSONObject(text)
    val x = json.getInt("x")
    val y = json.getInt("y")
    val color = json.getString("color")
    if (x in 0..15 && y in 0..15 && HEX_COLOR.matches(color)) Pixel(x, y, color) else null
} catch (e: Exception) {
    null
}

private val HEX_COLOR = Regex("^#[0-9a-fA-F]{6}$")

/** Turns the backend's HTTP base URL into the relay's WebSocket URL (http->ws, https->wss). */
fun relayWebSocketUrl(apiBaseUrl: String): String {
    val base = apiBaseUrl.trimEnd('/')
    val ws = when {
        base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
        base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
        else -> base
    }
    return "$ws/ws"
}

/** OkHttp-backed [PixelStream] with a fixed reconnect delay. */
class OkHttpPixelStream(
    private val url: String,
    private val reconnectDelayMs: Long = 2_000L,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build(),
) : PixelStream {

    override fun events(): Flow<PixelEvent> = flow {
        while (currentCoroutineContext().isActive) {
            emitAll(singleConnection())
            delay(reconnectDelayMs)
        }
    }

    /** One WebSocket session: completes when the socket closes or fails. */
    private fun singleConnection(): Flow<PixelEvent> = callbackFlow {
        val socket = client.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    trySend(PixelEvent.Connected)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    parsePixel(text)?.let { trySend(PixelEvent.Update(it)) }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(code, reason)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    trySend(PixelEvent.Disconnected("closed ($code)"))
                    close()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    trySend(PixelEvent.Disconnected(t.message ?: t.javaClass.simpleName))
                    close()
                }
            },
        )
        awaitClose { socket.cancel() }
    }
}
