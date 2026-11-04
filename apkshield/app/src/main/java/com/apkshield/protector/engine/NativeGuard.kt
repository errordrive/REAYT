package com.apkshield.protector.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder

object NativeGuard {

    fun buildStub(digest: String): ByteArray {
        val payload = StringBuilder()
            .append("APKSHIELD1\n")
            .append("digest=").append(digest).append('\n')
            .append("checks=ptrace,frida,tracer,emulator,debugger\n")
            .append("action=abort\n")
            .toString()
            .toByteArray(Charsets.UTF_8)
        val header = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(0x41504B53)
            .putInt(1)
            .putInt(payload.size)
            .putInt(crc32(payload))
            .array()
        return header + payload
    }

    private fun crc32(data: ByteArray): Int {
        var crc = 0xFFFFFFFF.toInt()
        for (b in data) {
            crc = crc xor (b.toInt() and 0xFF)
            for (i in 0 until 8) {
                crc = if (crc and 1 != 0) (crc ushr 1) xor 0xEDB88320.toInt() else crc ushr 1
            }
        }
        return crc.inv()
    }
}
