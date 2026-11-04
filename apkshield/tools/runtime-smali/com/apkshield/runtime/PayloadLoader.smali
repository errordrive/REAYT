.class public final Lcom/apkshield/runtime/PayloadLoader;
.super Ljava/lang/Object;

.method private constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static install(Landroid/content/Context;)Ljava/lang/ClassLoader;
    .locals 8
    :try_start
    invoke-virtual {p0}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;
    move-result-object v0
    const-string v1, "apkshield_payload.bin"
    invoke-virtual {v0, v1}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;
    move-result-object v0
    invoke-static {v0}, Lcom/apkshield/runtime/PayloadLoader;->readAll(Ljava/io/InputStream;)[B
    move-result-object v0
    const-string v1, "apkshield_payload.key"
    invoke-static {p0, v1}, Lcom/apkshield/runtime/AssetCrypto;->open(Landroid/content/Context;Ljava/lang/String;)[B
    move-result-object v1
    const-string v2, "SHA-256"
    invoke-static {v2}, Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;
    move-result-object v2
    invoke-virtual {v2, v1}, Ljava/security/MessageDigest;->digest([B)[B
    move-result-object v1
    const/16 v2, 0x20
    const/16 v3, 0x10
    new-array v4, v3, [B
    const/16 v5, 0xc
    new-array v5, v5, [B
    array-length v6, v0
    const/16 v7, 0x4
    sub-int/2addr v6, v7
    sub-int/2addr v6, v5
    sub-int/2addr v6, v3
    new-array v6, v6, [B
    const/4 v3, 0x0
    const/16 v7, 0xc
    invoke-static {v0, v7, v5, v3, v7}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V
    const/16 v7, 0x10
    const/16 v3, 0x0
    invoke-static {v0, v7, v4, v3, v7}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V
    const/16 v7, 0x20
    invoke-static {v0, v7, v6, v3, v7}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V
    const-string v7, "AES/GCM/NoPadding"
    invoke-static {v7}, Ljavax/crypto/Cipher;->getInstance(Ljava/lang/String;)Ljavax/crypto/Cipher;
    move-result-object v7
    new-instance v3, Ljavax/crypto/spec/SecretKeySpec;
    const-string v0, "AES"
    invoke-direct {v3, v1, v0}, Ljavax/crypto/spec/SecretKeySpec;-><init>([BLjava/lang/String;)V
    new-instance v0, Ljavax/crypto/spec/GCMParameterSpec;
    const/16 v1, 0x80
    invoke-direct {v0, v1, v5}, Ljavax/crypto/spec/GCMParameterSpec;-><init>(I[B)V
    const/4 v1, 0x2
    invoke-virtual {v7, v1, v3, v0}, Ljavax/crypto/Cipher;->init(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V
    invoke-virtual {v7, v6}, Ljavax/crypto/Cipher;->doFinal([B)[B
    move-result-object v0
    new-instance v1, Ldalvik/system/InMemoryDexClassLoader;
    invoke-virtual {p0}, Landroid/content/Context;->getClassLoader()Ljava/lang/ClassLoader;
    move-result-object v3
    invoke-direct {v1, v0, v3}, Ldalvik/system/InMemoryDexClassLoader;-><init>([BLjava/lang/ClassLoader;)V
    return-object v1
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :err
    :err
    invoke-virtual {p0}, Landroid/content/Context;->getClassLoader()Ljava/lang/ClassLoader;
    move-result-object v0
    return-object v0
.end method

.method private static readAll(Ljava/io/InputStream;)[B
    .locals 5
    new-instance v0, Ljava/io/ByteArrayOutputStream;
    invoke-direct {v0}, Ljava/io/ByteArrayOutputStream;-><init>()V
    const/16 v1, 0x2000
    new-array v1, v1, [B
    :loop
    invoke-virtual {p0, v1}, Ljava/io/InputStream;->read([B)I
    move-result v2
    if-lez v2, :done
    const/4 v3, 0x0
    invoke-virtual {v0, v1, v3, v2}, Ljava/io/ByteArrayOutputStream;->write([BII)V
    goto :loop
    :done
    invoke-virtual {p0}, Ljava/io/InputStream;->close()V
    invoke-virtual {v0}, Ljava/io/ByteArrayOutputStream;->toByteArray()[B
    move-result-object v3
    return-object v3
.end method
