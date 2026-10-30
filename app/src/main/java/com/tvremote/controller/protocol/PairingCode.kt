package com.tvremote.controller.protocol

import java.math.BigInteger

/**
 * 6-character pairing code decoder.
 *
 * Reverse engineered from the reference TV remote app — the code shown on the TV screen
 * secretly carries the TV's IP address and WebSocket port, so entering the code is enough
 * to connect without any network discovery.
 *
 * Alphabet: 0-9 and A-V (Base32).  'W' is read as '0' and 'X' as '1'.
 *
 * Decoded layout (30 bits):
 *   bit  0..2   network prefix : 00 -> 10.x.x.x   01 -> 172.x.x.x   10 -> 192.168.x.x
 *   bit  2..12  (192.168 only) 10-bit checksum
 *   bit  2..4   (other)         2-bit checksum
 *   bit  4..6   (other)         port index : 00=8125 01=8126 10=8127 11=8128
 *   bit 12..14  (192.168 only)  port index
 *   rest        the actual IP octets, 8 bits each
 */
object PairingCode {

    private const val ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUV"
    private const val TOTAL_BITS = 30

    private val PORTS = mapOf("00" to 8125, "01" to 8126, "10" to 8127, "11" to 8128)

    data class Result(val ip: String, val port: Int)

    /** Returns null when the code is malformed or the checksum does not match. */
    fun decode(input: String): Result? {
        val cleaned = input.trim().uppercase().replace("W", "0").replace("X", "1")
        if (cleaned.isEmpty()) return null

        var value = BigInteger.ZERO
        for (ch in cleaned) {
            val idx = ALPHABET.indexOf(ch)
            if (idx < 0) return null
            value = value.multiply(BigInteger.valueOf(32)).add(BigInteger.valueOf(idx.toLong()))
        }

        val bits = value.toString(2).padStart(TOTAL_BITS, '0').takeLast(TOTAL_BITS)

        val prefix = bits.substring(0, 2)
        return when (prefix) {
            "10" -> {
                val checksum = bits.substring(2, 12)
                val portBits = bits.substring(12, 14)
                val host = bits.substring(14, 30)
                if (checksum != fold(10, host)) return null
                val port = PORTS[portBits] ?: return null
                Result("192.168.${octet(host, 0)}.${octet(host, 8)}", port)
            }
            "00", "01" -> {
                val checksum = bits.substring(2, 4)
                val portBits = bits.substring(4, 6)
                val host = bits.substring(6, 30)
                if (checksum != fold(2, host)) return null
                val port = PORTS[portBits] ?: return null
                val head = if (prefix == "00") "10" else "172"
                Result("$head.${octet(host, 0)}.${octet(host, 8)}.${octet(host, 16)}", port)
            }
            else -> null
        }
    }

    /**
     * Inverse of [decode] — used by the TV agent app to display its own code.
     */
    fun encode(ip: String, port: Int): String? {
        val portBits = PORTS.entries.firstOrNull { it.value == port }?.key ?: return null
        val parts = ip.split(".")
        if (parts.size != 4) return null
        val o = parts.map { it.toIntOrNull() ?: return null }

        val bits: String = when {
            ip.startsWith("192.168.") -> {
                val host = bin(o[2]) + bin(o[3])
                "10" + fold(10, host) + portBits + host
            }
            ip.startsWith("10.") -> {
                val host = bin(o[1]) + bin(o[2]) + bin(o[3])
                "00" + fold(2, host) + portBits + host
            }
            ip.startsWith("172.") -> {
                val host = bin(o[1]) + bin(o[2]) + bin(o[3])
                "01" + fold(2, host) + portBits + host
            }
            else -> return null
        }

        var value = BigInteger(bits, 2)
        val sb = StringBuilder()
        val base = BigInteger.valueOf(32)
        repeat(6) {
            sb.append(ALPHABET[value.mod(base).toInt()])
            value = value.divide(base)
        }
        return sb.reverse().toString()
    }

    private fun bin(v: Int) = Integer.toBinaryString(v).padStart(8, '0').takeLast(8)

    private fun octet(bits: String, from: Int) = Integer.parseInt(bits.substring(from, from + 8), 2)

    /** checksum = first half + second half of the given bit string */
    private fun fold(width: Int, source: String): String {
        val half = width / 2
        return source.substring(0, half) + source.substring(source.length - half)
    }
}
