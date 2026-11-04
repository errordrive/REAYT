package com.apkshield.protector.engine

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object AxmlPatcher {

    private const val RES_STRING_POOL_TYPE = 0x0001
    private const val RES_XML_TYPE = 0x0003
    private const val RES_XML_START_ELEMENT_TYPE = 0x0102
    private const val RES_XML_RESOURCE_MAP_TYPE = 0x0180
    private const val UTF8_FLAG = 0x100
    private const val ATTR_NAME = 0x01010003

    fun patchApplicationName(axml: ByteArray, newClassName: String): ByteArray {
        val buf = ByteBuffer.wrap(axml).order(ByteOrder.LITTLE_ENDIAN)
        if ((buf.getShort(0).toInt() and 0xFFFF) != RES_XML_TYPE) {
            throw IllegalStateException("not AXML")
        }
        val headerSize = buf.getShort(2).toInt() and 0xFFFF
        val fileSize = buf.getInt(4)

        var pos = headerSize
        var stringPoolStart = -1
        var stringPoolSize = 0
        var stringPoolHeader = 0
        var stringPoolUtf8 = false
        var stringCount = 0
        var stringsBase = 0
        var resMapStart = -1
        var resMapHeader = 0
        val strings = mutableListOf<String>()

        while (pos + 8 <= fileSize) {
            val chunkType = buf.getShort(pos).toInt() and 0xFFFF
            val chunkHeader = buf.getShort(pos + 2).toInt() and 0xFFFF
            val chunkSize = buf.getInt(pos + 4)
            if (chunkSize < 8 || pos + chunkSize > fileSize) break
            when (chunkType) {
                RES_STRING_POOL_TYPE -> {
                    stringPoolStart = pos
                    stringPoolSize = chunkSize
                    stringPoolHeader = chunkHeader
                    stringPoolUtf8 = (buf.getInt(pos + 16) and UTF8_FLAG) != 0
                    stringCount = buf.getInt(pos + 8)
                    stringsBase = pos + buf.getInt(pos + 20)
                    for (i in 0 until stringCount) {
                        val off = buf.getInt(pos + chunkHeader + i * 4)
                        strings.add(readString(buf, stringsBase + off, stringPoolUtf8))
                    }
                }
                RES_XML_RESOURCE_MAP_TYPE -> {
                    resMapStart = pos
                    resMapHeader = chunkHeader
                }
            }
            pos += chunkSize
        }

        if (stringPoolStart < 0) throw IllegalStateException("no string pool")
        val appTagIndex = strings.indexOf("application")
        if (appTagIndex < 0) throw IllegalStateException("no <application>")

        var nameValueOffset = -1
        pos = headerSize
        while (pos + 8 <= fileSize) {
            val chunkType = buf.getShort(pos).toInt() and 0xFFFF
            val chunkHeader = buf.getShort(pos + 2).toInt() and 0xFFFF
            val chunkSize = buf.getInt(pos + 4)
            if (chunkSize < 8 || pos + chunkSize > fileSize) break
            if (chunkType == RES_XML_START_ELEMENT_TYPE) {
                val ext = pos + chunkHeader
                if (buf.getInt(ext + 4) == appTagIndex) {
                    val attrStart = buf.getShort(ext + 8).toInt() and 0xFFFF
                    val attrSize = buf.getShort(ext + 10).toInt() and 0xFFFF
                    val attrCount = buf.getShort(ext + 12).toInt() and 0xFFFF
                    for (i in 0 until attrCount) {
                        val a = ext + attrStart + i * attrSize
                        val nsIdx = buf.getInt(a)
                        val nameIdx = buf.getInt(a + 4)
                        if (nsIdx < 0 || nameIdx < 0 || nameIdx >= strings.size) continue
                        val resId = if (resMapStart >= 0 && nameIdx * 4 + 4 <= (buf.getInt(resMapStart + 4) - resMapHeader)) {
                            buf.getInt(resMapStart + resMapHeader + nameIdx * 4)
                        } else -1
                        val isName = resId == ATTR_NAME || strings[nameIdx] == "name"
                        if (isName) {
                            nameValueOffset = a + 16
                        }
                    }
                }
            }
            pos += chunkSize
        }
        if (nameValueOffset < 0) throw IllegalStateException("no android:name")

        val newIndex = strings.indexOf(newClassName).takeIf { it >= 0 } ?: strings.size
        val pool = if (newIndex < strings.size) {
            rebuildPool(strings, stringPoolHeader, stringPoolUtf8)
        } else {
            rebuildPool(strings + newClassName, stringPoolHeader, stringPoolUtf8)
        }

        val delta = pool.size - stringPoolSize
        val result = ByteArray(axml.size + delta)
        System.arraycopy(axml, 0, result, 0, stringPoolStart)
        System.arraycopy(pool, 0, result, stringPoolStart, pool.size)
        System.arraycopy(axml, stringPoolStart + stringPoolSize, result, stringPoolStart + pool.size, axml.size - stringPoolStart - stringPoolSize)

        val outBuf = ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN)
        outBuf.putInt(4, result.size)
        outBuf.putInt(nameValueOffset + delta, newIndex)
        return result
    }

    private fun rebuildPool(strings: List<String>, headerSize: Int, utf8: Boolean): ByteArray {
        val encoded = strings.map { if (utf8) encodeUtf8(it) else encodeUtf16(it) }
        val count = strings.size
        val offsetsSize = count * 4
        val dataStart = headerSize + offsetsSize
        val dataSize = encoded.sumOf { it.size }
        val pad = (4 - (dataSize % 4)) % 4
        val total = dataStart + dataSize + pad
        val bb = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN)
        bb.putShort(RES_STRING_POOL_TYPE.toShort())
        bb.putShort(headerSize.toShort())
        bb.putInt(total)
        bb.putInt(count)
        bb.putInt(0)
        bb.putInt(if (utf8) UTF8_FLAG else 0)
        bb.putInt(dataStart)
        bb.putInt(0)
        var off = 0
        for (e in encoded) { bb.putInt(off); off += e.size }
        for (e in encoded) bb.put(e)
        while (bb.position() < total) bb.put(0)
        return bb.array()
    }

    private fun readString(buf: ByteBuffer, offset: Int, utf8: Boolean): String {
        if (utf8) {
            var p = offset
            var b = buf.get(p).toInt() and 0xFF; p++
            if (b and 0x80 != 0) p++
            b = buf.get(p).toInt() and 0xFF; p++
            val len = if (b and 0x80 != 0) {
                val l = ((b and 0x7F) shl 8) or (buf.get(p).toInt() and 0xFF); p++; l
            } else b
            val bytes = ByteArray(len)
            for (i in 0 until len) bytes[i] = buf.get(p + i)
            return String(bytes, Charsets.UTF_8)
        }
        var p = offset
        var len = buf.getShort(p).toInt() and 0xFFFF; p += 2
        if (len and 0x8000 != 0) {
            len = ((len and 0x7FFF) shl 16) or (buf.getShort(p).toInt() and 0xFFFF); p += 2
        }
        val chars = CharArray(len)
        for (i in 0 until len) chars[i] = buf.getChar(p + i * 2)
        return String(chars)
    }

    private fun encodeUtf8(s: String): ByteArray {
        val bytes = s.toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        if (bytes.size > 0x7F) out.write((bytes.size shr 8) or 0x80)
        out.write(bytes.size and 0xFF)
        out.write(bytes)
        out.write(0)
        return out.toByteArray()
    }

    private fun encodeUtf16(s: String): ByteArray {
        val len = s.length
        val out = ByteArrayOutputStream()
        val bb = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        if (len > 0x7FFF) {
            bb.putShort(((len shr 16) or 0x8000).toShort()); out.write(bb.array()); bb.clear()
        }
        bb.putShort((len and 0xFFFF).toShort())
        out.write(bb.array())
        for (c in s) { out.write(c.code and 0xFF); out.write((c.code shr 8) and 0xFF) }
        out.write(0); out.write(0)
        return out.toByteArray()
    }
}
