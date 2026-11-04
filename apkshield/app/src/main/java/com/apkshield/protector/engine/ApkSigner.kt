package com.apkshield.protector.engine

import com.android.apksig.ApkSigner as ApkSigSigner
import com.android.apksig.ApkSigner.SignerConfig
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

object ApkSigner {

    private const val KEYSTORE_ASSET = "apkshield.p12"
    private const val KEYSTORE_PASS = "apkshield"
    private const val ALIAS = "apkshield"

    fun signInPlace(apk: File) {
        val ks = loadKeystore()
        val key = ks.getKey(ALIAS, KEYSTORE_PASS.toCharArray()) as PrivateKey
        val cert = ks.getCertificate(ALIAS) as X509Certificate
        val signer = SignerConfig.builder()
            .setName("ApkShield")
            .setPrivateKey(key)
            .addCertificate(cert)
            .build()
        val out = File(apk.parentFile, apk.name + ".signed")
        ApkSigSigner.Builder(listOf(signer))
            .setInputApk(apk)
            .setOutputApk(out)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
        if (!apk.delete()) throw IllegalStateException("cannot replace unsigned apk")
        if (!out.renameTo(apk)) throw IllegalStateException("cannot move signed apk")
    }

    private fun loadKeystore(): KeyStore {
        val ks = KeyStore.getInstance("PKCS12")
        val cls = ApkSigner::class.java
        val stream = cls.classLoader!!.getResourceAsStream(KEYSTORE_ASSET)
            ?: throw IllegalStateException("built-in keystore missing from assets: $KEYSTORE_ASSET")
        stream.use { ks.load(it, KEYSTORE_PASS.toCharArray()) }
        return ks
    }
}
