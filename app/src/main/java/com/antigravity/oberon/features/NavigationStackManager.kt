package com.antigravity.oberon.features

import android.app.AlertDialog
import android.content.Context
import android.webkit.WebBackForwardList
import android.webkit.WebView
import android.widget.ImageButton

object NavigationStackManager {

    fun setupHistoryJump(
        context: Context,
        btnBack: ImageButton,
        btnForward: ImageButton,
        getWebView: () -> WebView?
    ) {
        btnBack.setOnLongClickListener {
            val webView = getWebView() ?: return@setOnLongClickListener false
            showHistoryDialog(context, webView, isForward = false)
            true
        }

        btnForward.setOnLongClickListener {
            val webView = getWebView() ?: return@setOnLongClickListener false
            showHistoryDialog(context, webView, isForward = true)
            true
        }
    }

    private fun showHistoryDialog(context: Context, webView: WebView, isForward: Boolean) {
        val history: WebBackForwardList = webView.copyBackForwardList()
        val currentIndex = history.currentIndex
        val items = mutableListOf<String>()
        val offsets = mutableListOf<Int>()

        if (isForward) {
            for (i in currentIndex + 1 until history.size) {
                val item = history.getItemAtIndex(i)
                items.add(item.title.ifBlank { item.url })
                offsets.add(i - currentIndex)
            }
        } else {
            for (i in currentIndex - 1 downTo 0) {
                val item = history.getItemAtIndex(i)
                items.add(item.title.ifBlank { item.url })
                offsets.add(i - currentIndex)
            }
        }

        if (items.isEmpty()) {
            return
        }

        AlertDialog.Builder(context)
            .setTitle(if (isForward) "Forward History" else "Recent History")
            .setItems(items.toTypedArray()) { _, which ->
                val offset = offsets[which]
                webView.goBackOrForward(offset)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
