package com.apkshield.protector.engine

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object DexPacker {

    data class Packed(val blob: ByteArray, val keyB64: String, val dexCount: Int)

    fun pack(dexEntries: Map<String, ByteArray>): Packed {
        val ordered = dexEntries.toSortedMap()
        val total = ordered.values.sumOf { it.size }
        val header = StringBuilder()
            .append("APKSHIELD-PAYLOAD/1\n")
            .append("count=").append(ordered.size).append('\n')
        for ((name, bytes) in ordered) {
            header.append(name).append(':').append(bytes.size).append('\n')
        }
        header.append("--\n")
        val headerBytes = header.toString().toByteArray(Charsets.UTF_8)
        val body = ByteArray(headerBytes.size + total)
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.size)
        var off = headerBytes.size
        for (bytes in ordered.values) {
            System.arraycopy(bytes, 0, body, off, bytes.size)
            off += bytes.size
        }
        val keyB64 = ResourceCipher.randomKeyB64()
        val key = MessageDigest.getInstance("SHA-256").digest(keyB64.toByteArray())
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val sealed = cipher.doFinal(body)
        val tagLen = 16
        val ct = sealed.copyOfRange(0, sealed.size - tagLen)
        val tag = sealed.copyOfRange(sealed.size - tagLen, sealed.size)
        val blob = ByteArray(4 + 12 + 16 + ct.size)
        blob[0] = 'S'.code.toByte(); blob[1] = 'H'.code.toByte()
        blob[2] = 'E'.code.toByte(); blob[3] = 'D'.code.toByte()
        System.arraycopy(iv, 0, blob, 4, 12)
        System.arraycopy(tag, 0, blob, 16, 16)
        System.arraycopy(ct, 0, blob, 32, ct.size)
        return Packed(blob, keyB64, ordered.size)
    }
}
