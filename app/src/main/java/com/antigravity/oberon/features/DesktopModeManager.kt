package com.antigravity.oberon.features

import android.webkit.WebView

object DesktopModeManager {

    private const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val desktopTabs = mutableSetOf<String>()

    fun isDesktopMode(tabId: String): Boolean = desktopTabs.contains(tabId)

    fun toggleDesktopMode(tabId: String, webView: WebView, defaultMobileUa: String): Boolean {
        val willBeDesktop = !isDesktopMode(tabId)
        if (willBeDesktop) {
            desktopTabs.add(tabId)
            webView.settings.userAgentString = DESKTOP_USER_AGENT
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
        } else {
            desktopTabs.remove(tabId)
            webView.settings.userAgentString = defaultMobileUa
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
        }
        webView.reload()
        return willBeDesktop
    }
}
