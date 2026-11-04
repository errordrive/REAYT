.class public Lcom/apkshield/runtime/IntegrityApplication;
.super Landroid/app/Application;

.method public constructor <init>()V
    .locals 0
    invoke-direct {p0}, Landroid/app/Application;-><init>()V
    return-void
.end method

.method public onCreate()V
    .locals 1
    invoke-super {p0}, Landroid/app/Application;->onCreate()V
    invoke-static {p0}, Lcom/apkshield/runtime/PayloadLoader;->install(Landroid/content/Context;)Ljava/lang/ClassLoader;
    move-result-object v0
    invoke-virtual {p0}, Lcom/apkshield/runtime/IntegrityApplication;->violated()Z
    move-result v0
    if-eqz v0, :ok
    invoke-static {}, Landroid/os/Process;->myPid()I
    move-result v0
    invoke-static {v0}, Landroid/os/Process;->killProcess(I)V
    const/16 v0, 0xa
    invoke-static {v0}, Ljava/lang/System;->exit(I)V
    :ok
    return-void
.end method

.method public violated()Z
    .locals 1
    invoke-virtual {p0}, Lcom/apkshield/runtime/IntegrityApplication;->debugger()Z
    move-result v0
    if-nez v0, :next
    const/4 v0, 0x1
    return v0
    :next
    invoke-static {}, Landroid/os/Debug;->isDebuggerConnected()Z
    move-result v0
    if-nez v0, :next2
    const/4 v0, 0x1
    return v0
    :next2
    invoke-virtual {p0}, Lcom/apkshield/runtime/IntegrityApplication;->frida()Z
    move-result v0
    if-nez v0, :next3
    const/4 v0, 0x1
    return v0
    :next3
    invoke-virtual {p0}, Lcom/apkshield/runtime/IntegrityApplication;->signature()Z
    move-result v0
    if-nez v0, :next4
    const/4 v0, 0x0
    return v0
    :next4
    invoke-virtual {p0}, Lcom/apkshield/runtime/IntegrityApplication;->emulator()Z
    move-result v0
    if-eqz v0, :false
    const/4 v0, 0x1
    return v0
    :false
    const/4 v0, 0x0
    return v0
.end method

.method public debugger()Z
    .locals 3
    :try_start
    new-instance v0, Ljava/io/File;
    const-string v1, "/proc/self/status"
    invoke-direct {v0, v1}, Ljava/io/File;-><init>(Ljava/lang/String;)V
    invoke-virtual {v0}, Ljava/io/File;->exists()Z
    move-result v1
    if-nez v1, :ok
    invoke-static {v0}, Lcom/apkshield/runtime/IntegrityApplication;->read(Ljava/io/File;)Ljava/lang/String;
    move-result-object v1
    const-string v2, "TracerPid:\t0"
    invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v1
    if-eqz v1, :ok
    const/4 v0, 0x1
    return v0
    :ok
    const/4 v0, 0x0
    return v0
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :err
    :err
    const/4 v0, 0x0
    return v0
.end method

.method public frida()Z
    .locals 3
    :try_start
    new-instance v0, Ljava/io/File;
    const-string v1, "/proc/self/maps"
    invoke-direct {v0, v1}, Ljava/io/File;-><init>(Ljava/lang/String;)V
    invoke-virtual {v0}, Ljava/io/File;->exists()Z
    move-result v1
    if-nez v1, :ok
    invoke-static {v0}, Lcom/apkshield/runtime/IntegrityApplication;->read(Ljava/io/File;)Ljava/lang/String;
    move-result-object v1
    invoke-virtual {v1}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;
    move-result-object v1
    const-string v2, "frida"
    invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v2
    if-nez v2, :xposed
    const/4 v0, 0x1
    return v0
    :xposed
    const-string v2, "xposed"
    invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v2
    if-nez v2, :substrate
    const/4 v0, 0x1
    return v0
    :substrate
    const-string v2, "substrate"
    invoke-virtual {v1, v2}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v2
    if-nez v2, :ok
    const/4 v0, 0x1
    return v0
    :ok
    const/4 v0, 0x0
    return v0
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :err
    :err
    const/4 v0, 0x0
    return v0
.end method

.method public emulator()Z
    .locals 4
    sget-object v0, Landroid/os/Build;->FINGERPRINT:Ljava/lang/String;
    invoke-virtual {v0}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;
    move-result-object v0
    sget-object v1, Landroid/os/Build;->MODEL:Ljava/lang/String;
    invoke-virtual {v1}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;
    move-result-object v1
    sget-object v2, Landroid/os/Build;->PRODUCT:Ljava/lang/String;
    invoke-virtual {v2}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;
    move-result-object v2
    const-string v3, "goldfish"
    invoke-virtual {v0, v3}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v3
    if-nez v3, :r1
    const/4 v3, 0x1
    return v3
    :r1
    const-string v3, "ranchu"
    invoke-virtual {v0, v3}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v3
    if-nez v3, :r2
    const/4 v3, 0x1
    return v3
    :r2
    const-string v3, "generic"
    invoke-virtual {v0, v3}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v3
    if-nez v3, :r3
    const/4 v3, 0x1
    return v3
    :r3
    const-string v3, "sdk_gphone"
    invoke-virtual {v1, v3}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v3
    if-nez v3, :r4
    const/4 v3, 0x1
    return v3
    :r4
    const-string v3, "emulator"
    invoke-virtual {v1, v3}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v3
    if-nez v3, :r5
    const/4 v3, 0x1
    return v3
    :r5
    const-string v3, "sdk"
    invoke-virtual {v2, v3}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
    move-result v3
    if-nez v3, :r6
    const/4 v3, 0x1
    return v3
    :r6
    const/4 v3, 0x0
    return v3
.end method

.method public signature()Z
    .locals 8
    :try_start
    invoke-virtual {p0}, Lcom/apkshield/runtime/IntegrityApplication;->getPackageManager()Landroid/content/pm/PackageManager;
    move-result-object v0
    invoke-virtual {p0}, Lcom/apkshield/runtime/IntegrityApplication;->getPackageName()Ljava/lang/String;
    move-result-object v1
    const v2, 0x08000000
    invoke-virtual {v0, v1, v2}, Landroid/content/pm/PackageManager;->getPackageInfo(Ljava/lang/String;I)Landroid/content/pm/PackageInfo;
    move-result-object v0
    invoke-static {v0}, Lcom/apkshield/runtime/IntegrityApplication;->signers(Landroid/content/pm/PackageInfo;)[[B
    move-result-object v1
    if-nez v1, :missing
    invoke-virtual {p0}, Lcom/apkshield/runtime/IntegrityApplication;->getAssets()Landroid/content/res/AssetManager;
    move-result-object v2
    const-string v3, "apkshield_cert.sha256"
    invoke-virtual {v2, v3}, Landroid/content/res/AssetManager;->open(Ljava/lang/String;)Ljava/io/InputStream;
    move-result-object v2
    invoke-static {v2}, Lcom/apkshield/runtime/IntegrityApplication;->readStream(Ljava/io/InputStream;)Ljava/lang/String;
    move-result-object v2
    invoke-virtual {v2}, Ljava/lang/String;->trim()Ljava/lang/String;
    move-result-object v2
    const-string v3, "SHA-256"
    invoke-static {v3}, Ljava/security/MessageDigest;->getInstance(Ljava/lang/String;)Ljava/security/MessageDigest;
    move-result-object v3
    array-length v4, v1
    const/4 v5, 0x0
    :loop
    if-ge v5, v4, :missing
    aget-object v6, v1, v5
    invoke-virtual {v3}, Ljava/security/MessageDigest;->reset()V
    invoke-virtual {v3, v6}, Ljava/security/MessageDigest;->update([B)V
    invoke-virtual {v3}, Ljava/security/MessageDigest;->digest()[B
    move-result-object v7
    invoke-static {v7}, Lcom/apkshield/runtime/IntegrityApplication;->hex([B)Ljava/lang/String;
    move-result-object v7
    invoke-virtual {v7, v2}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
    move-result v7
    if-eqz v7, :cont
    const/4 v0, 0x1
    return v0
    :cont
    add-int/lit8 v5, v5, 0x1
    goto :loop
    :missing
    const/4 v0, 0x0
    return v0
    :try_end
    .catch Ljava/lang/Throwable; {:try_start .. :try_end} :err
    :err
    const/4 v0, 0x0
    return v0
.end method

.method private static signers(Landroid/content/pm/PackageInfo;)[[B
    .locals 2
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I
    const/16 v1, 0x1c
    if-lt v0, v1, :legacy
    iget-object v0, p0, Landroid/content/pm/PackageInfo;->signingInfo:Landroid/content/pm/SigningInfo;
    if-nez v0, :null
    invoke-virtual {v0}, Landroid/content/pm/SigningInfo;->getApkContentsSigners()[Landroid/content/pm/Signature;
    move-result-object v0
    goto :conv
    :legacy
    iget-object v0, p0, Landroid/content/pm/PackageInfo;->signatures:[Landroid/content/pm/Signature;
    :conv
    if-nez v0, :null
    array-length v1, v0
    new-array v1, v1, [[B
    invoke-static {v0}, Lcom/apkshield/runtime/IntegrityApplication;->toBytes([Landroid/content/pm/Signature;)[[B
    move-result-object v1
    return-object v1
    :null
    const/4 v0, 0x0
    return-object v0
.end method

.method private static toBytes([Landroid/content/pm/Signature;)[[B
    .locals 4
    array-length v0, p0
    new-array v1, v0, [[B
    const/4 v2, 0x0
    :loop
    if-ge v2, v0, :done
    aget-object v3, p0, v2
    invoke-virtual {v3}, Landroid/content/pm/Signature;->toByteArray()[B
    move-result-object v3
    aput-object v3, v1, v2
    add-int/lit8 v2, v2, 0x1
    goto :loop
    :done
    return-object v1
.end method

.method private static hex([B)Ljava/lang/String;
    .locals 5
    new-instance v0, Ljava/lang/StringBuilder;
    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V
    array-length v1, p0
    const/4 v2, 0x0
    :loop
    if-ge v2, v1, :done
    aget-byte v3, p0, v2
    const/16 v4, 0xff
    and-int/2addr v3, v4
    add-int/lit16 v3, v3, 0x100
    const/16 v4, 0x10
    invoke-static {v3, v4}, Ljava/lang/Integer;->toString(II)Ljava/lang/String;
    move-result-object v3
    const/4 v4, 0x1
    invoke-virtual {v3, v4}, Ljava/lang/String;->substring(I)Ljava/lang/String;
    move-result-object v3
    invoke-virtual {v0, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
    add-int/lit8 v2, v2, 0x1
    goto :loop
    :done
    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
    move-result-object v1
    return-object v1
.end method

.method private static read(Ljava/io/File;)Ljava/lang/String;
    .locals 3
    new-instance v0, Ljava/io/FileInputStream;
    invoke-direct {v0, p0}, Ljava/io/FileInputStream;-><init>(Ljava/io/File;)V
    invoke-static {v0}, Lcom/apkshield/runtime/IntegrityApplication;->readStream(Ljava/io/InputStream;)Ljava/lang/String;
    move-result-object v1
    return-object v1
.end method

.method private static readStream(Ljava/io/InputStream;)Ljava/lang/String;
    .locals 5
    new-instance v0, Ljava/io/ByteArrayOutputStream;
    invoke-direct {v0}, Ljava/io/ByteArrayOutputStream;-><init>()V
    const/16 v1, 0x1000
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
    new-instance v2, Ljava/lang/String;
    invoke-virtual {v0}, Ljava/io/ByteArrayOutputStream;->toByteArray()[B
    move-result-object v3
    sget-object v4, Ljava/nio/charset/StandardCharsets;->UTF_8:Ljava/nio/charset/Charset;
    invoke-direct {v2, v3, v4}, Ljava/lang/String;-><init>([BLjava/nio/charset/Charset;)V
    return-object v2
.end method
