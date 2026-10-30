package com.tvremote.controller.transport

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tvremote.controller.protocol.RemoteMessage
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

/**
 * Command channel to the TV.
 *
 * Same transport the reference app uses: plain WebSocket + protobuf frames,
 * 5 second lost-connection detection, TCP_NODELAY on.
 *
 * Production build swaps ws:// for wss:// and adds an auth token header.
 */
class TvWebSocketClient(
    url: String,
    private val authToken: String? = null
) : WebSocketClient(URI.create(url), Draft_6455(), null, 5000) {

    companion object { private const val TAG = "TvWs" }

    interface Listener {
        fun onConnected()
        fun onDisconnected(reason: String)
        fun onReply(json: String)
    }

    var listener: Listener? = null
    private val main = Handler(Looper.getMainLooper())
    private var reconnectAttempts = 0

    init {
        setTcpNoDelay(true)
        connectionLostTimeout = 5
    }

    fun send(message: RemoteMessage) {
        if (!isOpen) { Log.w(TAG, "not connected, dropping"); return }
        send(message.encode())
    }

    fun sendKey(keyCode: Int) = send(RemoteMessage(RemoteMessage.TYPE_KEY_EVENT, keyCode.toString()))

    fun sendTouch(action: Int, points: List<RemoteMessage.Pointer>) =
        send(RemoteMessage(RemoteMessage.TYPE_EVENT, action.toString(), points))

    fun sendVoice(pcm: ByteArray) =
        send(RemoteMessage(RemoteMessage.TYPE_EVENT, "voice", emptyList(), pcm))

    override fun onOpen(handshake: ServerHandshake) {
        reconnectAttempts = 0
        main.post { listener?.onConnected() }
    }

    override fun onMessage(message: String) {
        main.post { listener?.onReply(message) }
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) {
        main.post { listener?.onDisconnected(reason) }
        scheduleReconnect()
    }

    override fun onError(ex: Exception) {
        Log.w(TAG, "error", ex)
    }

    override fun onStart() {
        addHeader("X-Auth-Token", authToken ?: "")
    }

    /** Exponential backoff reconnect: 1s, 2s, 4s ... max 30s */
    private fun scheduleReconnect() {
        if (reconnectAttempts >= 6) return
        val delay = (1000L shl reconnectAttempts).coerceAtMost(30_000L)
        reconnectAttempts++
        main.postDelayed({
            if (!isOpen) runCatching { reconnect() }
        }, delay)
    }
}
