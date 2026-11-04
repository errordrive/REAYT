package com.apkshield.protector.engine

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ApkIo {

    fun readEntries(apk: File): Map<String, ByteArray> {
        val out = LinkedHashMap<String, ByteArray>()
        ZipFile(apk).use { zip ->
            val en = zip.entries()
            while (en.hasMoreElements()) {
                val e = en.nextElement()
                if (e.isDirectory) continue
                zip.getInputStream(e).use { it.readBytes() }.let { out[e.name] = it }
            }
        }
        return out
    }

    fun writeEntries(dest: File, entries: Map<String, ByteArray>) {
        dest.parentFile?.mkdirs()
        FileOutputStream(dest).use { fos ->
            ZipOutputStream(fos).use { zos ->
                zos.setMethod(ZipOutputStream.DEFLATED)
                for ((name, bytes) in entries) {
                    val e = ZipEntry(name)
                    e.time = 0L
                    zos.putNextEntry(e)
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
        }
    }

    fun isSignatureEntry(name: String): Boolean {
        val n = name.uppercase()
        return n.startsWith("META-INF/") && (
            n.endsWith(".SF") || n.endsWith(".RSA") || n.endsWith(".DSA") || n.endsWith(".EC") || n == "META-INF/MANIFEST.MF"
        )
    }
}
