package com.sp0.plus.data

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Shared HTTP stack. Resolves hostnames through AdGuard DNS (DNS-over-HTTPS) so ad/tracker
 * domains are blocked for this app's own traffic (API calls and audio streaming), regardless
 * of the system Private DNS setting. Falls back to system DNS if the DoH resolver is unreachable.
 */
object NetworkModule {

    const val ADGUARD_DOH_URL = "https://dns.adguard-dns.com/dns-query"
    const val PRIVATE_DNS_HOSTNAME = "dns.adguard-dns.com"

    private const val UA_ANDROID_MUSIC =
        "com.google.android.apps.youtube.music/7.27.52 (Linux; U; Android 11) gzip"
    private const val UA_IOS =
        "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"
    private const val UA_ANDROID_VR =
        "com.google.android.apps.youtube.vr.oculus/1.60.19 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip"
    const val UA_WEB =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"

    private val bootstrapClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    private val dohDns: DnsOverHttps by lazy {
        DnsOverHttps.Builder()
            .client(bootstrapClient)
            .url(ADGUARD_DOH_URL.toHttpUrl())
            .bootstrapDnsHosts(
                InetAddress.getByName("94.140.14.14"),
                InetAddress.getByName("94.140.15.15"),
                InetAddress.getByName("2a10:50c0::ad1:ff"),
                InetAddress.getByName("2a10:50c0::ad2:ff")
            )
            .build()
    }

    private val adblockDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            if (!SettingsManager.adblockDnsEnabled.value) return Dns.SYSTEM.lookup(hostname)
            return try {
                dohDns.lookup(hostname)
            } catch (_: Exception) {
                Dns.SYSTEM.lookup(hostname)
            }
        }
    }

    /** Client for InnerTube API calls. */
    val apiClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(adblockDns)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Client for ExoPlayer. googlevideo URLs carry a `c=` param naming the client that
     * requested them; the User-Agent must match that client or the server answers 403.
     */
    val playerClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(adblockDns)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request()
                val ua = when (req.url.queryParameter("c")) {
                    "ANDROID_MUSIC" -> UA_ANDROID_MUSIC
                    "IOS" -> UA_IOS
                    "ANDROID_VR" -> UA_ANDROID_VR
                    else -> null
                }
                chain.proceed(
                    if (ua != null) req.newBuilder().header("User-Agent", ua).build() else req
                )
            }
            .build()
    }
}
