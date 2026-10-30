package com.tvremote.controller.protocol

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Protobuf message exchanged with the TV over WebSocket.
 *
 * Wire layout (original .proto):
 *   message Message {
 *     int32    type     = 1;   // 0 = EVENT (touch/mouse), 1 = KEY_EVENT (button)
 *     string   action   = 2;   // Android KeyEvent keycode or MotionEvent action, as text
 *     repeated Pointer pointer = 3;   // Pointer { float x = 1; float y = 2; }
 *     bytes    data     = 4;   // raw mic PCM for voice
 *     Extra    extra    = 5;
 *   }
 *
 * Hand written encoder/decoder so the project does not need protoc during the build.
 */
data class RemoteMessage(
    val type: Int,
    val action: String = "",
    val pointers: List<Pointer> = emptyList(),
    val data: ByteArray = ByteArray(0)
) {
    data class Pointer(val x: Float, val y: Float)

    companion object {
        const val TYPE_EVENT = 0
        const val TYPE_KEY_EVENT = 1
    }

    fun encode(): ByteArray {
        val out = ByteArrayOutputStream()
        if (type != 0) out.writeWireVarint(1, type.toLong())
        if (action.isNotEmpty()) out.writeWireString(2, action)
        pointers.forEach { p ->
            val inner = ByteArrayOutputStream()
            if (p.x != 0f) inner.writeWireFixed32(1, p.x)
            if (p.y != 0f) inner.writeWireFixed32(2, p.y)
            out.writeWireBytes(3, inner.toByteArray())
        }
        if (data.isNotEmpty()) out.writeWireBytes(4, data)
        return out.toByteArray()
    }

    fun decode(bytes: ByteArray): RemoteMessage {
        var type = 0
        var action = ""
        val pointers = mutableListOf<Pointer>()
        var i = 0
        while (i < bytes.size) {
            val tag = bytes.readVarint(i); i = tag.next
            val field = (tag.value ushr 3).toInt()
            val wire = (tag.value and 0x7).toInt()
            when (wire) {
                0 -> { val v = bytes.readVarint(i); i = v.next
                    if (field == 1) type = v.value.toInt() }
                2 -> { val len = bytes.readVarint(i); i = len.next
                    val payload = bytes.copyOfRange(i, i + len.value.toInt()); i += len.value.toInt()
                    when (field) {
                        2 -> action = String(payload, Charsets.UTF_8)
                        3 -> pointers.add(decodePointer(payload))
                    } }
                5 -> { i += 4 }
                1 -> { i += 8 }
                else -> return RemoteMessage(type, action, pointers)
            }
        }
        return RemoteMessage(type, action, pointers)
    }

    private fun decodePointer(payload: ByteArray): Pointer {
        var x = 0f; var y = 0f; var i = 0
        while (i < payload.size) {
            val tag = payload.readVarint(i); i = tag.next
            val field = (tag.value ushr 3).toInt()
            val wire = (tag.value and 0x7).toInt()
            if (wire == 5 && i + 4 <= payload.size) {
                val f = ByteBuffer.wrap(payload, i, 4).order(ByteOrder.LITTLE_ENDIAN).float
                if (field == 1) x = f else if (field == 2) y = f
                i += 4
            } else break
        }
        return Pointer(x, y)
    }
}

/* ---------- protobuf wire primitives ---------- */

private data class Varint(val value: Long, val next: Int)

private fun ByteArray.readVarint(start: Int): Varint {
    var result = 0L; var shift = 0; var i = start
    while (i < size) {
        val b = this[i].toInt() and 0xFF
        result = result or ((b and 0x7F).toLong() shl shift)
        i++
        if (b and 0x80 == 0) break
        shift += 7
    }
    return Varint(result, i)
}

private fun ByteArrayOutputStream.writeVarint(value: Long) {
    var v = value
    while (v and 0x7FL.inv() != 0L) {
        write(((v and 0x7F) or 0x80).toInt())
        v = v ushr 7
    }
    write(v.toInt())
}

private fun ByteArrayOutputStream.writeWireVarint(field: Int, value: Long) {
    writeVarint(((field shl 3) or 0).toLong()); writeVarint(value)
}

private fun ByteArrayOutputStream.writeWireString(field: Int, value: String) =
    writeWireBytes(field, value.toByteArray(Charsets.UTF_8))

private fun ByteArrayOutputStream.writeWireBytes(field: Int, value: ByteArray) {
    writeVarint(((field shl 3) or 2).toLong()); writeVarint(value.size.toLong()); write(value)
}

private fun ByteArrayOutputStream.writeWireFixed32(field: Int, value: Float) {
    writeVarint(((field shl 3) or 5).toLong())
    write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array())
}
