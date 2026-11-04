package com.apkshield.protector.engine

data class ProtectOptions(
    val encryptStrings: Boolean = true,
    val renameSymbols: Boolean = true,
    val injectIntegrityGuard: Boolean = true,
    val antiInstrumentation: Boolean = true,
    val encryptResources: Boolean = true,
    val resign: Boolean = true,
    val packDex: Boolean = true,
    val deepObfuscate: Boolean = true
)
