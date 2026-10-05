package com.antigravity.oberon.search

import android.content.Context
import android.content.SharedPreferences

data class SearchEngine(
    val id: String,
    val name: String,
    val searchUrl: String,
    val suggestUrl: String
)

class SearchEngineManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("oberon_search_prefs", Context.MODE_PRIVATE)

    companion object {
        val GOOGLE = SearchEngine(
            id = "google",
            name = "Google",
            searchUrl = "https://www.google.com/search?q=%s",
            suggestUrl = "https://suggestqueries.google.com/complete/search?client=chrome&q=%s"
        )
        val DUCKDUCKGO = SearchEngine(
            id = "duckduckgo",
            name = "DuckDuckGo",
            searchUrl = "https://duckduckgo.com/?q=%s",
            suggestUrl = "https://duckduckgo.com/ac/?q=%s&type=list"
        )
        val BING = SearchEngine(
            id = "bing",
            name = "Bing",
            searchUrl = "https://www.bing.com/search?q=%s",
            suggestUrl = "https://api.bing.com/osjson.aspx?query=%s"
        )
        val BRAVE = SearchEngine(
            id = "brave",
            name = "Brave Search",
            searchUrl = "https://search.brave.com/search?q=%s",
            suggestUrl = "https://search.brave.com/api/suggest?q=%s"
        )

        val DEFAULT_ENGINES = listOf(GOOGLE, DUCKDUCKGO, BING, BRAVE)
        private const val KEY_ACTIVE_ENGINE = "active_engine_id"
    }

    fun getActiveEngine(): SearchEngine {
        val activeId = prefs.getString(KEY_ACTIVE_ENGINE, GOOGLE.id)
        return DEFAULT_ENGINES.find { it.id == activeId } ?: GOOGLE
    }

    fun setActiveEngine(id: String) {
        prefs.edit().putString(KEY_ACTIVE_ENGINE, id).apply()
    }

    fun getAllEngines(): List<SearchEngine> = DEFAULT_ENGINES
}
