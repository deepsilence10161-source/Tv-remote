package com.tvremote.controller.data

/**
 * A discovered / paired TV.
 *
 * Matches the mDNS TXT record published by the TV agent:
 * device_mac, device_name, device_code, device_ip, screen_width, screen_height,
 * http_port, websocket_port, config (JSON capabilities)
 */
data class TvDevice(
    val macAddress: String = "",
    val name: String = "",
    val code: String = "",
    val ip: String = "",
    val port: Int = 0,
    val width: Int = 1920,
    val height: Int = 1080,
    val alias: String = "",
    val httpPort: String = "",
    val websocketPort: String = "",
    val config: Capabilities = Capabilities()
) {
    val wsUrl: String get() = "ws://$ip:$port"
}

data class Capabilities(
    val appManager: Boolean = false,
    val camera: Boolean = false,
    val voice: Boolean = false,
    val screenshots: Boolean = false,
    val appStore: Boolean = false,
    val browser: Boolean = false,
    val gesture: Boolean = false,
    val googleVoice: Boolean = false,
    val video: Boolean = false
)

/** Payload of the TV's JSON reply ({"code":500,"data":"<device json>",...}). */
data class ResultBody<T>(
    val code: Int = 0,
    val msg: String = "",
    val data: T? = null
)
