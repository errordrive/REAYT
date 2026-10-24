package com.reayt.youtube.data

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.dnsoverhttps.DnsOverHttps
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object NetworkModule {

    const val ADGUARD_DOH_URL = "https://dns.adguard-dns.com/dns-query"
    const val UA_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
    const val UA_IOS = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"
    const val UA_VISIONOS = "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15"
    const val UA_ANDROID_VR = "Mozilla/5.0 (Linux; Android 12; Quest 3) AppleWebKit/537.36"

    @Volatile
    var isDohEnabled: Boolean = true

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val bootstrapClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)

        appContext?.let { ctx ->
            try {
                val cacheDir = File(ctx.cacheDir, "doh_cache")
                builder.cache(Cache(cacheDir, 5L * 1024L * 1024L))
            } catch (_: Throwable) {}
        }
        builder.build()
    }

    private val dohDns: DnsOverHttps by lazy {
        try {
            DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(ADGUARD_DOH_URL.toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByAddress("94.140.14.14", byteArrayOf(94.toByte(), 140.toByte(), 14.toByte(), 14.toByte())),
                    InetAddress.getByAddress("94.140.15.15", byteArrayOf(94.toByte(), 140.toByte(), 15.toByte(), 15.toByte()))
                )
                .build()
        } catch (_: Throwable) {
            DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url(ADGUARD_DOH_URL.toHttpUrl())
                .build()
        }
    }

    private val fallbackDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            return try {
                Dns.SYSTEM.lookup(hostname)
            } catch (e: Exception) {
                if (isDohEnabled) {
                    try {
                        return dohDns.lookup(hostname)
                    } catch (dohEx: Exception) {
                        e.addSuppressed(dohEx)
                    }
                }
                throw e
            }
        }
    }

    val apiClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .dns(fallbackDns)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)

        appContext?.let { ctx ->
            try {
                val httpCacheDir = File(ctx.cacheDir, "http_cache")
                builder.cache(Cache(httpCacheDir, 50L * 1024L * 1024L))
            } catch (_: Throwable) {}
        }
        builder.build()
    }
}

/**
 * Suspend extension function that bridges OkHttp asynchronous call with coroutine cancellation.
 */
suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation {
        try {
            cancel()
        } catch (_: Throwable) {}
    }
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) {
                continuation.resume(response)
            }
        }

        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isCancelled) return
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    })
}
