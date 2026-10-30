package com.tvremote.controller.transport

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.tvremote.controller.data.Capabilities
import com.tvremote.controller.data.TvDevice
import com.google.gson.Gson

/**
 * Finds TVs on the same WiFi using mDNS / DNS-SD.
 *
 * Service type is "_share._tcp" (same as the reference app, so it also finds stock
 * CVTE / Bytello panels). Our own agent additionally publishes "_tvagent._tcp",
 * so we search for both.
 */
class TvDiscovery(private val context: Context) {

    companion object {
        private const val TAG = "TvDiscovery"
        val SERVICE_TYPES = listOf("_share._tcp", "_tvagent._tcp")
    }

    fun interface Callback {
        fun onDevice(device: TvDevice, found: Boolean)
    }

    private val nsdManager by lazy { context.getSystemService(Context.NSD_SERVICE) as NsdManager }
    private val gson = Gson()
    private val listeners = mutableListOf<NsdManager.DiscoveryListener>()
    private var cb: Callback? = null

    fun start(callback: Callback) {
        cb = callback
        SERVICE_TYPES.forEach { type -> startType(type) }
    }

    private fun startType(type: String) {
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) = Unit
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "start failed $serviceType code=$errorCode")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
            override fun onDiscoveryStopped(serviceType: String) = Unit
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                cb?.onDevice(parse(serviceInfo), false)
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(si: NsdServiceInfo, errorCode: Int) = Unit
                    override fun onServiceResolved(si: NsdServiceInfo) {
                        cb?.onDevice(parse(si), true)
                    }
                })
            }
        }
        listeners.add(listener)
        try {
            nsdManager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.w(TAG, "discover failed", e)
        }
    }

    fun stop() {
        listeners.forEach { runCatching { nsdManager.stopServiceDiscovery(it) } }
        listeners.clear()
        cb = null
    }

    private fun parse(info: NsdServiceInfo): TvDevice {
        val attrs = info.attributes
        fun str(key: String) = attrs[key]?.toString(Charsets.UTF_8) ?: ""
        fun num(key: String) = str(key).toIntOrNull() ?: 0

        val ip = info.host?.hostAddress ?: str("device_ip")
        val configJson = str("config")
        val caps = if (configJson.isNotEmpty())
            runCatching { gson.fromJson(configJson, Capabilities::class.java) }.getOrNull() ?: Capabilities()
        else Capabilities()

        return TvDevice(
            macAddress = str("device_mac"),
            name = str("device_name"),
            code = str("device_code"),
            ip = ip,
            port = info.port,
            width = num("screen_width"),
            height = num("screen_height"),
            alias = str("device_name"),
            httpPort = str("http_port"),
            websocketPort = str("websocket_port"),
            config = caps
        )
    }
}
