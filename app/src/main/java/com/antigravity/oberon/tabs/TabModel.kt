package com.antigravity.oberon.tabs

import android.webkit.WebView

data class TabModel(
    val id: String,
    var title: String = "New Tab",
    var url: String = "about:blank",
    val isIncognito: Boolean = false,
    val webView: WebView
)
