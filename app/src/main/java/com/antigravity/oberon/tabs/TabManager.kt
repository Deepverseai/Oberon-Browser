package com.antigravity.oberon.tabs

import android.webkit.WebView
import java.util.UUID

interface OnTabChangeListener {
    fun onTabCreated(tab: TabModel)
    fun onTabSelected(tab: TabModel)
    fun onTabClosed(tab: TabModel, newActiveTab: TabModel?)
    fun onTabUpdated(tab: TabModel)
    fun onTabCountChanged(count: Int)
}

class TabManager {
    private val tabs = mutableListOf<TabModel>()
    private var activeTabId: String? = null
    private val listeners = mutableListOf<OnTabChangeListener>()

    fun addListener(listener: OnTabChangeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: OnTabChangeListener) {
        listeners.remove(listener)
    }

    fun getActiveTab(): TabModel? {
        return tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()
    }

    fun getAllTabs(): List<TabModel> = tabs.toList()

    fun getTabCount(): Int = tabs.size

    fun createTab(
        url: String = "about:blank",
        isIncognito: Boolean = false,
        webViewFactory: (Boolean) -> WebView
    ): TabModel {
        val id = UUID.randomUUID().toString()
        val webView = webViewFactory(isIncognito)
        val tab = TabModel(
            id = id,
            title = if (isIncognito) "Incognito Tab" else "New Tab",
            url = url,
            isIncognito = isIncognito,
            webView = webView
        )
        tabs.add(tab)
        selectTab(id)
        listeners.forEach {
            it.onTabCreated(tab)
            it.onTabCountChanged(tabs.size)
        }
        pauseBackgroundTabs()
        return tab
    }

    fun selectTab(id: String): TabModel? {
        val target = tabs.find { it.id == id } ?: return null
        activeTabId = id
        
        // Resume active tab
        target.webView.onResume()
        
        listeners.forEach { it.onTabSelected(target) }
        pauseBackgroundTabs()
        return target
    }

    fun closeTab(id: String): TabModel? {
        val tabToClose = tabs.find { it.id == id } ?: return null
        val wasActive = (activeTabId == id)
        val index = tabs.indexOf(tabToClose)

        // Destroy WebView cleanly to release native graphics buffer
        tabToClose.webView.stopLoading()
        tabToClose.webView.loadUrl("about:blank")
        tabToClose.webView.onPause()
        tabToClose.webView.destroy()
        tabs.remove(tabToClose)

        val newActiveTab: TabModel? = if (wasActive) {
            if (tabs.isNotEmpty()) {
                val newIndex = index.coerceAtMost(tabs.size - 1)
                val nextTab = tabs[newIndex]
                activeTabId = nextTab.id
                nextTab.webView.onResume()
                nextTab
            } else {
                activeTabId = null
                null
            }
        } else {
            getActiveTab()
        }

        listeners.forEach {
            it.onTabClosed(tabToClose, newActiveTab)
            it.onTabCountChanged(tabs.size)
        }
        pauseBackgroundTabs()
        return newActiveTab
    }

    fun updateTabMetadata(id: String, title: String? = null, url: String? = null) {
        val tab = tabs.find { it.id == id } ?: return
        title?.let { tab.title = it }
        url?.let { tab.url = it }
        listeners.forEach { it.onTabUpdated(tab) }
    }

    /**
     * Tab Virtualization:
     * When total tabs exceed 3, pause timers and rendering on background tabs
     * to prevent OS Low Memory Killer (OOM) killing the app process.
     */
    fun pauseBackgroundTabs() {
        if (tabs.size > 3) {
            tabs.forEach { tab ->
                if (tab.id != activeTabId) {
                    tab.webView.onPause()
                    tab.webView.pauseTimers()
                } else {
                    tab.webView.resumeTimers()
                    tab.webView.onResume()
                }
            }
        }
    }
}
