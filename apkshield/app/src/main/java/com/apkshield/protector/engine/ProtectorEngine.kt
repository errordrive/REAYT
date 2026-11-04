package com.apkshield.protector.engine

import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom

class ProtectorEngine(private val log: (String) -> Unit) {

    data class Result(
        val outputApk: File,
        val dexCount: Int,
        val encryptedAssets: Int,
        val signed: Boolean,
        val packed: Boolean
    )

    fun protect(
        sourceApk: File,
        outputApk: File,
        options: ProtectOptions,
        runtimeDex: ByteArray,
        certSha256: String
    ): Result {
        require(sourceApk.isFile) { "source apk missing" }
        val seed = SecureRandom().nextLong()
        log("seed=" + java.lang.Long.toHexString(seed))

        val entries = ApkIo.readEntries(sourceApk)
            .filterKeys { !ApkIo.isSignatureEntry(it) }
            .toMutableMap()
        log("loaded " + entries.size + " entries from " + sourceApk.name)

        val dexNames = entries.keys.filter { it.endsWith(".dex") }.sorted()
        val dexDigest = MessageDigest.getInstance("SHA-256")
        val processedDex = LinkedHashMap<String, ByteArray>()
        var dexCount = 0
        for (name in dexNames) {
            var bytes = entries[name]!!
            if (options.encryptStrings) {
                bytes = DexTransformer.encryptStrings(bytes, seed)
                log("  " + name + ": string literals encrypted")
            }
            if (options.renameSymbols) {
                bytes = DexTransformer.renameIdentifiers(
                    bytes, seed,
                    if (options.deepObfuscate)
                        listOf("Lcom/apkshield/", "Landroid/", "Landroidx/", "Lkotlin/")
                    else
                        listOf("Lcom/apkshield/", "Landroid/", "Landroidx/", "Lkotlin/")
                )
                log("  " + name + ": identifiers renamed" + if (options.deepObfuscate) " (deep)" else "")
            }
            dexDigest.update(bytes)
            processedDex[name] = bytes
            dexCount++
        }

        var packed = false
        if (options.packDex && processedDex.isNotEmpty()) {
            val packedResult = DexPacker.pack(processedDex)
            for (name in dexNames) entries.remove(name)
            entries["assets/apkshield_payload.bin"] = packedResult.blob
            entries["assets/apkshield_payload.key"] = packedResult.keyB64.toByteArray()
            packed = true
            log("dex packed: " + packedResult.dexCount + " dex -> " +
                packedResult.blob.size + " bytes encrypted payload")
        } else {
            entries.putAll(processedDex)
            log("dex left unpacked (" + dexCount + " files)")
        }

        var encryptedAssets = 0
        if (options.encryptResources) {
            val assetKey = ResourceCipher.randomKeyB64()
            for (name in entries.keys.toList()) {
                if (!ResourceCipher.isEncryptable(name)) continue
                entries[name] = ResourceCipher.encrypt(entries[name]!!, assetKey)
                encryptedAssets++
            }
            entries["assets/apkshield_asset.key"] = assetKey.toByteArray()
            log("encrypted " + encryptedAssets + " asset/raw entries")
        }

        entries["classes2.dex"] = runtimeDex
        entries["assets/apkshield_cert.sha256"] = certSha256.trim().toByteArray()
        entries["assets/apkshield_digest.bin"] = dexDigest.digest()
        log("injected runtime dex (" + runtimeDex.size + " bytes)")

        if (options.injectIntegrityGuard) {
            val manifest = entries["AndroidManifest.xml"]
                ?: throw IllegalStateException("no AndroidManifest.xml")
            entries["AndroidManifest.xml"] = AxmlPatcher.patchApplicationName(
                manifest,
                "com.apkshield.runtime.IntegrityApplication"
            )
            log("manifest application name patched")
        }

        if (options.antiInstrumentation) {
            entries["assets/apkshield_native.bin"] = NativeGuard.buildStub(certSha256)
            log("anti-instrumentation descriptor injected")
        }

        ApkIo.writeEntries(outputApk, entries)
        log("wrote " + outputApk.name + " (" + outputApk.length() + " bytes)")

        if (options.resign) {
            ApkSigner.signInPlace(outputApk)
            log("signed output with built-in keystore")
        }

        return Result(outputApk, dexCount, encryptedAssets, options.resign, packed)
    }

    companion object {
        fun loadRuntimeDex(appClassLoader: ClassLoader): ByteArray {
            val stream = appClassLoader.getResourceAsStream("apkshield_runtime.dex")
                ?: throw IllegalStateException("runtime dex missing from ApkShield assets")
            return stream.use { it.readBytes() }
        }

        fun loadCertSha256(appClassLoader: ClassLoader): String {
            val ks = java.security.KeyStore.getInstance("PKCS12")
            val stream = appClassLoader.getResourceAsStream("apkshield.p12")
                ?: throw IllegalStateException("keystore missing")
            stream.use { ks.load(it, "apkshield".toCharArray()) }
            val cert = ks.getCertificate("apkshield") as java.security.cert.X509Certificate
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(cert.encoded).joinToString("") { "%02x".format(it) }
        }
    }
}
