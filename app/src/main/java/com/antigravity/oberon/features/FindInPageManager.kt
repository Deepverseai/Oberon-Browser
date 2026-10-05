package com.antigravity.oberon.features

import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import com.antigravity.oberon.R

class FindInPageManager(
    private val findBarView: View
) {
    private val etFindQuery: EditText = findBarView.findViewById(R.id.etFindQuery)
    private val tvMatchCount: TextView = findBarView.findViewById(R.id.tvMatchCount)
    private val btnFindPrev: ImageButton = findBarView.findViewById(R.id.btnFindPrev)
    private val btnFindNext: ImageButton = findBarView.findViewById(R.id.btnFindNext)
    private val btnFindClose: ImageButton = findBarView.findViewById(R.id.btnFindClose)

    private var activeWebView: WebView? = null

    init {
        etFindQuery.addTextChangedListener { text ->
            val query = text?.toString() ?: ""
            if (query.isNotEmpty()) {
                activeWebView?.findAllAsync(query)
            } else {
                activeWebView?.clearMatches()
                tvMatchCount.text = "0/0"
            }
        }

        etFindQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                activeWebView?.findNext(true)
                true
            } else {
                false
            }
        }

        btnFindPrev.setOnClickListener {
            activeWebView?.findNext(false)
        }

        btnFindNext.setOnClickListener {
            activeWebView?.findNext(true)
        }

        btnFindClose.setOnClickListener {
            hide()
        }
    }

    fun show(webView: WebView) {
        activeWebView = webView
        findBarView.visibility = View.VISIBLE
        etFindQuery.requestFocus()

        webView.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
            val current = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
            tvMatchCount.text = "$current/$numberOfMatches"
        }

        val imm = findBarView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.showSoftInput(etFindQuery, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }

    fun hide() {
        findBarView.visibility = View.GONE
        etFindQuery.setText("")
        activeWebView?.clearMatches()
        val imm = findBarView.context.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(etFindQuery.windowToken, 0)
        activeWebView = null
    }

    fun isShowing(): Boolean = findBarView.visibility == View.VISIBLE
}
