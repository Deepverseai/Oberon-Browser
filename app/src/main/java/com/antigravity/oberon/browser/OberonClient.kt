package com.antigravity.oberon.browser

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslCertificate
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

class OberonClient(
    private val context: Context,
    private val onPageStartedCallback: (url: String) -> Unit,
    private val onPageFinishedCallback: (url: String) -> Unit,
    private val onSslStateChangedCallback: (SslState, SslCertificate?) -> Unit,
    private val adBlockInterceptor: ((url: String) -> Boolean)? = null
) : WebViewClient() {

    companion object {
        /**
         * Cleans Android WebView signature to look like regular Google Chrome on Android
         * Removing the "; wv" and "Version/4.0" giveaways that bot-detectors flag.
         */
        fun cleanUserAgent(defaultUa: String): String {
            return defaultUa
                .replace("; wv", "")
                .replace(Regex("Version/\\d+\\.\\d+\\s+"), "")
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme ?: return false

        // Handle standard web schemes inside WebView
        if (scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true) || scheme.equals("about", ignoreCase = true)) {
            return false
        }

        // Handle external app deep-links (tel, mailto, whatsapp, intent)
        return try {
            val intent = if (scheme.equals("intent", ignoreCase = true)) {
                Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
            } else {
                Intent(Intent.ACTION_VIEW, uri)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            true // Handled or failed gracefully without crashing
        }
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
        val url = request?.url?.toString() ?: return null
        if (adBlockInterceptor != null && adBlockInterceptor.invoke(url)) {
            // Return empty 200 response to cleanly block ad without console error
            return WebResourceResponse("text/plain", "utf-8", 200, "OK", emptyMap(), null)
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let {
            onPageStartedCallback(it)
            val isHttps = it.startsWith("https://", ignoreCase = true)
            val cert = view?.certificate
            val state = if (isHttps) SslState.SECURE else SslState.INSECURE
            onSslStateChangedCallback(state, cert)
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        url?.let {
            onPageFinishedCallback(it)
            val isHttps = it.startsWith("https://", ignoreCase = true)
            val cert = view?.certificate
            val state = if (isHttps) SslState.SECURE else SslState.INSECURE
            onSslStateChangedCallback(state, cert)
        }
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        onSslStateChangedCallback(SslState.ERROR, error?.certificate)
        // Default to canceling for security
        handler?.cancel()
    }
}
