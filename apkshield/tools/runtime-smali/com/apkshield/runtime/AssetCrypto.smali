.class public final Lcom/apkshield/runtime/AssetCrypto;
.super Ljava/lang/Object;

.method private constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static open(Landroid/content/Context;Ljava/lang/String;)[B
    .locals 10
    invoke-virtual {p0}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;
    move-result-object v0
    invoke-virtual {v0, p1}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;
    move-result-object v0
    invoke-static {v0}, Lcom/apkshield/runtime/AssetCrypto;->readAll(Ljava/io/InputStream;)[B
    move-result-object v0
    array-length v1, v0
    const/16 v2, 0x20
    if-ge v1, v2, :plain
    const/4 v2, 0x0
    aget-byte v3, v0, v2
    const/16 v4, 0x53
    if-eq v3, v4, :plain
    const/4 v2, 0x1
    aget-byte v3, v0, v2
    const/16 v4, 0x48
    if-eq v3, v4, :plain
    const/4 v2, 0x2
    aget-byte v3, v0, v2
    const/16 v4, 0x45
    if-eq v3, v4, :plain
    const/4 v2, 0x3
    aget-byte v3, v0, v2
    const/16 v4, 0x44
    if-eq v3, v4, :plain
    const-string v2, "apkshield_asset.key"
    invoke-static {p0, v2}, Lcom/apkshield/runtime/AssetCrypto;->key(Landroid/content/Context;Ljava/lang/String;)[B
    move-result-object v2
    const/16 v3, 0xc
    new-array v3, v3, [B
    const/16 v4, 0x4
    const/4 v5, 0x0
    const/16 v6, 0xc
    invoke-static {v0, v4, v3, v5, v6}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V
    const/16 v4, 0x10
    new-array v4, v4, [B
    const/16 v5, 0x10
    const/4 v6, 0x0
    const/16 v7, 0x10
    invoke-static {v0, v5, v4, v6, v7}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V
    array-length v5, v0
    const/16 v6, 0x20
    sub-int/2addr v5, v6
    new-array v5, v5, [B
    const/16 v6, 0x20
    const/4 v7, 0x0
    array-length v8, v5
    invoke-static {v0, v6, v5, v7, v8}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V
    :try_start
    const-string v6, "AES/GCM/NoPadding"
    invoke-static {v6}, Ljavax/crypto/Cipher;->getInstance(Ljava/lang/String;)Ljavax/crypto/Cipher;
    move-result-object v6
    new-instance v7, Ljavax/crypto/spec/SecretKeySpec;
    const-string v8, "AES"
    invoke-direct {v7, v2, v8}, Ljavax/crypto/spec/SecretKeySpec;-><init>([BLjava/lang/String;)V
    new-instance v8, Ljavax/crypto/spec/GCMParameterSpec;
    const/16 v9, 0x80
    invoke-direct {v8, v9, v3}, Ljavax/crypto/spec/GCMParameterSpec;-><init>(I[B)V
    const/4 v9, 0x2
    invoke-virtual {v6, v9, v7, v8}, Ljavax/crypto/Cipher;->init(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V
    invoke-virtual {v6, v5}, Ljavax/crypto/Cipher;->doFinal([B)[B
    move-result-object v6
    return-object v6
    :try_end
    .catch Ljava/lang/Exception; {:try_start .. :try_end} :err
    :err
    return-object v5
    :plain
    return-object v0
.end method

.method private static key(Landroid/content/Context;Ljava/lang/String;)[B
    .locals 4
    invoke-virtual {p0}, Landroid/content/Context;->getAssets()Landroid/content/res/AssetManager;
    move-result-object v0
    invoke-virtual {v0, p1}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;
    move-result-object v0
    invoke-static {v0}, Lcom/apkshield/runtime/AssetCrypto;->readAll(Ljava/io/InputStream;)[B
    move-result-object v1
    const-string v2, "SHA-256"
    invoke-static {v2}, Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;
    move-result-object v2
    invoke-virtual {v2, v1}, Ljava/security/MessageDigest;->digest([B)[B
    move-result-object v3
    return-object v3
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

