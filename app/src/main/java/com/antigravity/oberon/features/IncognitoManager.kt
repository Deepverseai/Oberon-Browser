package com.antigravity.oberon.features

import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView

object IncognitoManager {

    fun setupIncognitoSettings(webView: WebView) {
        val settings = webView.settings
        settings.databaseEnabled = false
        settings.domStorageEnabled = false
        settings.saveFormData = false
        settings.savePassword = false

        CookieManager.getInstance().setAcceptCookie(false)
        CookieManager.getInstance().removeSessionCookies(null)
    }

    fun cleanIncognitoSession(webView: WebView) {
        webView.clearCache(true)
        webView.clearFormData()
        webView.clearHistory()
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeSessionCookies(null)
    }
}
