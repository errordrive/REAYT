package com.apkshield.protector.engine

import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.security.SecureRandom

object ResourceCipher {

    fun isEncryptable(name: String): Boolean {
        if (!name.startsWith("assets/") && !name.startsWith("res/raw/")) return false
        if (name.startsWith("assets/apkshield_")) return false
        return !name.endsWith(".dex") && !name.endsWith(".arsc")
    }

    fun encrypt(data: ByteArray, keyB64: String): ByteArray {
        val key = MessageDigest.getInstance("SHA-256").digest(keyB64.toByteArray())
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val body = cipher.doFinal(data)
        val tagLen = 16
        val ct = ByteArray(body.size - tagLen)
        val tag = ByteArray(tagLen)
        System.arraycopy(body, 0, ct, 0, ct.size)
        System.arraycopy(body, ct.size, tag, 0, tagLen)
        val out = ByteArray(4 + 12 + 16 + ct.size)
        out[0] = 'S'.code.toByte()
        out[1] = 'H'.code.toByte()
        out[2] = 'E'.code.toByte()
        out[3] = 'D'.code.toByte()
        System.arraycopy(iv, 0, out, 4, 12)
        System.arraycopy(tag, 0, out, 16, 16)
        System.arraycopy(ct, 0, out, 32, ct.size)
        return out
    }

    fun randomKeyB64(): String {
        val raw = ByteArray(32).also { SecureRandom().nextBytes(it) }
        return java.util.Base64.getEncoder().encodeToString(raw)
    }
}
