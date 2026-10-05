package com.antigravity.oberon.search

import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

object SuggestionsClient {
    private val executor = Executors.newSingleThreadExecutor()

    fun fetchSuggestions(
        query: String,
        engine: SearchEngine,
        callback: (List<String>) -> Unit
    ) {
        val trimmed = query.trim()
        if (trimmed.isEmpty() || trimmed.length < 2) {
            callback(emptyList())
            return
        }

        executor.execute {
            val suggestions = mutableListOf<String>()
            var connection: HttpURLConnection? = null
            try {
                val encoded = URLEncoder.encode(trimmed, "UTF-8")
                val requestUrl = URL(engine.suggestUrl.replace("%s", encoded))
                connection = requestUrl.openConnection() as HttpURLConnection
                connection.connectTimeout = 1500
                connection.readTimeout = 1500
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/124.0.0.0 Mobile Safari/537.36")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    // Parse OpenSearch/Google format: [ "query", [ "sugg1", "sugg2", ... ] ]
                    val rootArray = JSONArray(response)
                    if (rootArray.length() > 1) {
                        val itemsArray = rootArray.optJSONArray(1)
                        if (itemsArray != null) {
                            for (i in 0 until minOf(itemsArray.length(), 6)) {
                                val item = itemsArray.optString(i)
                                if (item.isNotBlank()) {
                                    suggestions.add(item)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fallback on network error or timeout
            } finally {
                connection?.disconnect()
            }
            callback(suggestions)
        }
    }
}
