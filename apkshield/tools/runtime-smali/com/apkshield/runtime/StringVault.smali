.class public final Lcom/apkshield/runtime/StringVault;
.super Ljava/lang/Object;

.method private constructor <init>()V
    .locals 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    return-void
.end method

.method public static open(Ljava/lang/String;)Ljava/lang/String;
    .locals 8
    invoke-static {p0}, Ljava/util/Base64;->getDecoder()Ljava/util/Base64$Decoder;
    move-result-object v0
    invoke-virtual {v0, p0}, Ljava/util/Base64$Decoder;->decode(Ljava/lang/String;)[B
    move-result-object v0
    array-length v1, v0
    if-nez v1, :ret_empty
    new-instance v1, Ljava/lang/String;
    invoke-direct {v1}, Ljava/lang/String;-><init>()V
    return-object v1
    :ret_empty
    const/4 v2, 0x0
    aget-byte v2, v0, v2
    and-int/lit16 v2, v2, 0xff
    add-int/lit8 v3, v1, -0x1
    new-array v3, v3, [B
    const/4 v4, 0x1
    :loop
    if-ge v4, v1, :done
    aget-byte v5, v0, v4
    xor-int/2addr v5, v2
    add-int/lit8 v6, v4, -0x1
    and-int/lit8 v7, v6, 0x1f
    xor-int/2addr v5, v7
    int-to-byte v5, v5
    aput-byte v5, v3, v6
    add-int/lit8 v4, v4, 0x1
    goto :loop
    :done
    new-instance v1, Ljava/lang/String;
    sget-object v4, Ljava/nio/charset/StandardCharsets;->UTF_8:Ljava/nio/charset/Charset;
    invoke-direct {v1, v3, v4}, Ljava/lang/String;-><init>([BLjava/nio/charset/Charset;)V
    return-object v1
.end method
