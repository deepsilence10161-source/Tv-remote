package com.tvremote.controller

import com.tvremote.controller.protocol.PairingCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PairingCodeTest {

    @Test
    fun `encode then decode round trip`() {
        val cases = listOf(
            "192.168.1.23" to 8125,
            "192.168.100.200" to 8127,
            "10.0.1.15" to 8126,
            "172.16.8.9" to 8128
        )
        for ((ip, port) in cases) {
            val code = PairingCode.encode(ip, port)
            requireNotNull(code) { "encode failed for $ip" }
            assertEquals(6, code.length)
            val back = PairingCode.decode(code)
            requireNotNull(back) { "decode failed for $code" }
            assertEquals(ip, back.ip)
            assertEquals(port, back.port)
        }
    }

    @Test
    fun `garbage code returns null`() {
        assertNull(PairingCode.decode(""))
        assertNull(PairingCode.decode("ZZZZZZ"))
    }
}
