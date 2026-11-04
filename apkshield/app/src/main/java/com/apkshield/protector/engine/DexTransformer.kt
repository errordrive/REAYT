package com.apkshield.protector.engine

import org.jf.dexlib2.Opcode
import org.jf.dexlib2.Opcodes
import org.jf.dexlib2.builder.MethodImplementationBuilder
import org.jf.dexlib2.builder.MethodLocation
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import org.jf.dexlib2.iface.MethodImplementation
import org.jf.dexlib2.iface.instruction.ReferenceInstruction
import org.jf.dexlib2.iface.reference.StringReference
import org.jf.dexlib2.rewriter.DexRewriter
import org.jf.dexlib2.rewriter.Rewriter
import org.jf.dexlib2.rewriter.RewriterModule
import org.jf.dexlib2.rewriter.Rewriters
import org.jf.dexlib2.writer.io.MemoryDataStore
import org.jf.dexlib2.writer.pool.DexPool
import java.security.MessageDigest
import java.util.Random

object DexTransformer {

    private const val STRINGS_CLASS = "Lcom/apkshield/runtime/StringVault;"

    fun encryptStrings(dex: ByteArray, seed: Long): ByteArray {
        val dexFile = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), dex.inputStream())
        val rewriter = DexRewriter(object : RewriterModule() {
            override fun getMethodImplementationRewriter(rewriters: Rewriters): Rewriter<MethodImplementation> {
                return Rewriter { impl ->
                    if (impl == null) return@Rewriter null
                    try {
                        val builder = MethodImplementationBuilder(impl.registerCount)
                        for (instruction in impl.instructions) {
                            val loc: MethodLocation = builder.addInstruction(instruction)
                            if (instruction is ReferenceInstruction &&
                                instruction.opcode == Opcode.CONST_STRING &&
                                instruction.reference is StringReference) {
                                val literal = (instruction.reference as StringReference).string
                                if (literal.isNotEmpty()) {
                                    val encoded = encryptLiteral(literal, seed)
                                    loc.replaceInstruction(
                                        BuilderInstruction21c(Opcode.CONST_STRING, loc, StringRef(encoded))
                                    )
                                }
                            }
                        }
                        builder.build()
                    } catch (t: Throwable) {
                        impl
                    }
                }
            }
        })
        val rewritten = rewriter.rewriteDexFile(dexFile)
        val store = MemoryDataStore()
        DexPool.writeTo(store, rewritten)
        return store.data
    }

    private fun encryptLiteral(value: String, seed: Long): String {
        val rnd = Random(seed xor value.hashCode().toLong())
        val key = rnd.nextInt(255) + 1
        val plain = value.toByteArray(Charsets.UTF_8)
        val out = ByteArray(plain.size + 1)
        out[0] = key.toByte()
        for (i in plain.indices) {
            out[i + 1] = (plain[i].toInt() xor key xor (i and 0x1F)).toByte()
        }
        return java.util.Base64.getEncoder().encodeToString(out)
    }

    class StringRef(private val s: String) : StringReference {
        override fun getString(): String = s
        override fun getType(): Int = org.jf.dexlib2.ReferenceType.STRING
    }

    fun renameIdentifiers(dex: ByteArray, seed: Long, protectedPrefixes: List<String>): ByteArray {
        val dexFile = DexBackedDexFile.fromInputStream(Opcodes.getDefault(), dex.inputStream())
        val mapping = HashMap<String, String>()
        val rnd = Random(seed)
        for (cls in dexFile.classes) {
            val t = cls.type
            if (t == STRINGS_CLASS || protectedPrefixes.any { t.startsWith(it) }) continue
            if (t.startsWith("Landroid/") || t.startsWith("Landroidx/") || t.startsWith("Lkotlin/")) continue
            mapping[t] = obfuscatedName(rnd)
        }
        val rewriter = DexRewriter(object : RewriterModule() {
            override fun getTypeRewriter(rewriters: Rewriters): Rewriter<String> {
                return Rewriter { t -> mapping[t] ?: t }
            }
        })
        val rewritten = rewriter.rewriteDexFile(dexFile)
        val store = MemoryDataStore()
        DexPool.writeTo(store, rewritten)
        return store.data
    }

    private fun obfuscatedName(rnd: Random): String {
        val len = 3 + rnd.nextInt(4)
        val sb = StringBuilder("L")
        repeat(len) { sb.append(('a'.code + rnd.nextInt(26)).toChar()) }
        sb.append(';')
        return sb.toString()
    }

    fun digest(dex: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(dex).joinToString("") { "%02x".format(it) }
    }
}
