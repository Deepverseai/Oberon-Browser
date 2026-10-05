package com.antigravity.oberon.features

import android.net.Uri

object AdBlocker {
    var isEnabled: Boolean = true

    // Top ad and tracking network hosts
    private val AD_HOSTS = hashSetOf(
        "doubleclick.net",
        "googleads.g.doubleclick.net",
        "pagead2.googlesyndication.com",
        "adnxs.com",
        "criteo.com",
        "criteo.net",
        "outbrain.com",
        "taboola.com",
        "scorecardresearch.com",
        "quantserve.com",
        "rubiconproject.com",
        "pubmatic.com",
        "advertising.com",
        "admob.com",
        "amazon-adsystem.com",
        "casalemedia.com",
        "openx.net",
        "smartadserver.com",
        "yieldmo.com",
        "moatads.com",
        "chartbeat.net",
        "adservice.google.com"
    )

    fun isAd(url: String): Boolean {
        if (!isEnabled) return false
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false
            AD_HOSTS.any { adHost ->
                host == adHost || host.endsWith(".$adHost")
            }
        } catch (e: Exception) {
            false
        }
    }
}
