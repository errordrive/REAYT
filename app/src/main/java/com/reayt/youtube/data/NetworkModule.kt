package com.reayt.youtube.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.TimeUnit

object NetworkModule {

    const val ADGUARD_DOH_URL = "https://dns.adguard-dns.com/dns-query"
    const val UA_WEB = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
    const val UA_IOS = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)"

    private val bootstrapClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

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

    val apiClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(dohDns)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
