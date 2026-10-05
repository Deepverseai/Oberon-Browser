package com.antigravity.oberon

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.antigravity.oberon.browser.FileUploaderManager
import com.antigravity.oberon.browser.OberonChromeClient
import com.antigravity.oberon.browser.OberonClient
import com.antigravity.oberon.browser.SslSecurityManager
import com.antigravity.oberon.search.OmniboxClassifier
import com.antigravity.oberon.search.SearchEngineManager
import com.antigravity.oberon.search.SuggestionsClient
import com.antigravity.oberon.tabs.OnTabChangeListener
import com.antigravity.oberon.tabs.TabManager
import com.antigravity.oberon.tabs.TabModel
import com.antigravity.oberon.ui.TabSwitcherBottomSheet
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tabManager: TabManager
    private lateinit var searchEngineManager: SearchEngineManager
    private lateinit var fileUploaderManager: FileUploaderManager

    private lateinit var webViewContainer: FrameLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var pbLoading: ProgressBar
    private lateinit var omniboxInput: AutoCompleteTextView
    private lateinit var ivSslStatus: ImageView
    private lateinit var btnClearQuery: ImageButton
    private lateinit var btnVoiceSearch: ImageButton
    private lateinit var tvTabCountBadge: TextView
    private lateinit var btnTabSwitcher: FrameLayout
    private lateinit var btnNavBack: ImageButton
    private lateinit var btnNavForward: ImageButton
    private lateinit var btnNavHome: ImageButton
    private lateinit var btnNavReload: ImageButton
    private lateinit var btnPreviewMode: ImageButton
    private lateinit var btnOverflowMenu: ImageButton

    companion object {
        const val VOICE_SEARCH_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tabManager = TabManager()
        searchEngineManager = SearchEngineManager(this)
        fileUploaderManager = FileUploaderManager(this)

        initViews()
        setupOmnibox()
        setupNavigationControls()
        setupTabManager()
        setupBackPressedHandler()

        // Handle incoming intent URL or create initial tab
        val initialUrl = intent?.dataString ?: "https://www.google.com"
        tabManager.createTab(initialUrl, isIncognito = false) { isIncognito ->
            createConfiguredWebView(isIncognito)
        }
        tabManager.getActiveTab()?.webView?.loadUrl(initialUrl)
    }

    private fun initViews() {
        webViewContainer = findViewById(R.id.webViewContainer)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        pbLoading = findViewById(R.id.pbLoading)

        val omniboxView = findViewById<View>(R.id.layoutOmnibox)
        omniboxInput = omniboxView.findViewById(R.id.omniboxInput)
        ivSslStatus = omniboxView.findViewById(R.id.ivSslStatus)
        btnClearQuery = omniboxView.findViewById(R.id.btnClearQuery)
        btnVoiceSearch = omniboxView.findViewById(R.id.btnVoiceSearch)

        tvTabCountBadge = findViewById(R.id.tvTabCountBadge)
        btnTabSwitcher = findViewById(R.id.btnTabSwitcher)

        btnNavBack = findViewById(R.id.btnNavBack)
        btnNavForward = findViewById(R.id.btnNavForward)
        btnNavHome = findViewById(R.id.btnNavHome)
        btnNavReload = findViewById(R.id.btnNavReload)
        btnPreviewMode = findViewById(R.id.btnPreviewMode)
        btnOverflowMenu = findViewById(R.id.btnOverflowMenu)

        swipeRefreshLayout.setOnRefreshListener {
            tabManager.getActiveTab()?.webView?.reload()
        }
    }

    private fun setupOmnibox() {
        omniboxInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH) {
                val input = omniboxInput.text.toString()
                loadInput(input)
                true
            } else {
                false
            }
        }

        omniboxInput.addTextChangedListener { text ->
            val hasText = !text.isNullOrEmpty()
            btnClearQuery.visibility = if (hasText) View.VISIBLE else View.GONE
            btnVoiceSearch.visibility = if (hasText) View.GONE else View.VISIBLE

            if (hasText && omniboxInput.hasFocus()) {
                val engine = searchEngineManager.getActiveEngine()
                SuggestionsClient.fetchSuggestions(text.toString(), engine) { suggestions ->
                    runOnUiThread {
                        val adapter = android.widget.ArrayAdapter(
                            this,
                            android.R.layout.simple_dropdown_item_1line,
                            suggestions
                        )
                        omniboxInput.setAdapter(adapter)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }

        btnClearQuery.setOnClickListener {
            omniboxInput.setText("")
        }

        btnVoiceSearch.setOnClickListener {
            val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak web address or search")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            }
            try {
                startActivityForResult(voiceIntent, VOICE_SEARCH_REQUEST_CODE)
            } catch (e: Exception) {
                // Speech recognizer not installed
            }
        }

        ivSslStatus.setOnClickListener {
            val activeTab = tabManager.getActiveTab()
            val cert = activeTab?.webView?.certificate
            val url = activeTab?.url ?: ""
            SslSecurityManager.showCertificateDetails(this, cert, url)
        }
    }

    private fun loadInput(raw: String) {
        val engine = searchEngineManager.getActiveEngine()
        val targetUrl = OmniboxClassifier.classify(raw, engine)
        if (targetUrl.isNotEmpty()) {
            tabManager.getActiveTab()?.webView?.loadUrl(targetUrl)
            omniboxInput.clearFocus()
            val imm = getSystemService(INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(omniboxInput.windowToken, 0)
        }
    }

    private fun setupNavigationControls() {
        btnNavBack.setOnClickListener {
            val webView = tabManager.getActiveTab()?.webView
            if (webView?.canGoBack() == true) {
                webView.goBack()
            }
        }

        btnNavForward.setOnClickListener {
            val webView = tabManager.getActiveTab()?.webView
            if (webView?.canGoForward() == true) {
                webView.goForward()
            }
        }

        btnNavHome.setOnClickListener {
            tabManager.getActiveTab()?.webView?.loadUrl("https://www.google.com")
        }

        btnNavReload.setOnClickListener {
            tabManager.getActiveTab()?.webView?.reload()
        }

        btnTabSwitcher.setOnClickListener {
            val sheet = TabSwitcherBottomSheet(tabManager) { isIncognito ->
                val newTab = tabManager.createTab(
                    url = "https://www.google.com",
                    isIncognito = isIncognito
                ) { incog -> createConfiguredWebView(incog) }
                newTab.webView.loadUrl("https://www.google.com")
            }
            sheet.show(supportFragmentManager, "TabSwitcherSheet")
        }
    }

    private fun setupTabManager() {
        tabManager.addListener(object : OnTabChangeListener {
            override fun onTabCreated(tab: TabModel) {
                updateActiveWebViewDisplay(tab)
            }

            override fun onTabSelected(tab: TabModel) {
                updateActiveWebViewDisplay(tab)
            }

            override fun onTabClosed(tab: TabModel, newActiveTab: TabModel?) {
                if (newActiveTab != null) {
                    updateActiveWebViewDisplay(newActiveTab)
                }
            }

            override fun onTabUpdated(tab: TabModel) {
                if (tab.id == tabManager.getActiveTab()?.id) {
                    omniboxInput.setText(tab.url)
                }
            }

            override fun onTabCountChanged(count: Int) {
                tvTabCountBadge.text = count.toString()
            }
        })
    }

    private fun updateActiveWebViewDisplay(tab: TabModel) {
        webViewContainer.removeAllViews()
        val parent = tab.webView.parent as? FrameLayout
        parent?.removeView(tab.webView)
        webViewContainer.addView(
            tab.webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        omniboxInput.setText(tab.url)
        updateNavButtonsState()
    }

    private fun updateNavButtonsState() {
        val webView = tabManager.getActiveTab()?.webView
        btnNavBack.alpha = if (webView?.canGoBack() == true) 1.0f else 0.4f
        btnNavForward.alpha = if (webView?.canGoForward() == true) 1.0f else 0.4f
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun createConfiguredWebView(isIncognito: Boolean): WebView {
        val webView = WebView(this)
        val settings = webView.settings

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = !isIncognito
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportMultipleWindows(false)
        settings.allowFileAccess = false
        settings.allowContentAccess = true

        // User-Agent stealth camouflage
        val defaultUa = settings.userAgentString
        settings.userAgentString = OberonClient.cleanUserAgent(defaultUa)

        // Incognito cookie isolation
        if (isIncognito) {
            CookieManager.getInstance().setAcceptCookie(false)
        } else {
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }

        webView.webViewClient = OberonClient(
            context = this,
            onPageStartedCallback = { url ->
                pbLoading.visibility = View.VISIBLE
                omniboxInput.setText(url)
                updateNavButtonsState()
            },
            onPageFinishedCallback = { url ->
                pbLoading.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                tabManager.updateTabMetadata(tabManager.getActiveTab()?.id ?: "", url = url)
                updateNavButtonsState()
            },
            onSslStateChangedCallback = { state, _ ->
                SslSecurityManager.updateSslIcon(ivSslStatus, state)
            }
        )

        webView.webChromeClient = OberonChromeClient(
            onProgressChangedCallback = { progress ->
                pbLoading.progress = progress
                if (progress >= 100) {
                    pbLoading.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                }
            },
            onReceivedTitleCallback = { title ->
                tabManager.updateTabMetadata(tabManager.getActiveTab()?.id ?: "", title = title)
            },
            fileUploaderManager = fileUploaderManager
        )

        return webView
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentTab = tabManager.getActiveTab()
                if (currentTab != null && currentTab.webView.canGoBack()) {
                    currentTab.webView.goBack()
                } else if (tabManager.getTabCount() > 1) {
                    // Close tab if more than 1 tab open
                    currentTab?.let { tabManager.closeTab(it.id) }
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        fileUploaderManager.onActivityResult(requestCode, resultCode, data)

        if (requestCode == VOICE_SEARCH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                omniboxInput.setText(spokenText)
                loadInput(spokenText)
            }
        }
    }
}
