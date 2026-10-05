package com.antigravity.oberon.search

import java.net.URLEncoder
import java.util.regex.Pattern

object OmniboxClassifier {

    private val IP_PORT_PATTERN = Pattern.compile(
        "^(\\d{1,3}\\.){3}\\d{1,3}(:\\d+)?(/.*)?$"
    )

    private val LOCALHOST_PATTERN = Pattern.compile(
        "^localhost(:\\d+)?(/.*)?$",
        Pattern.CASE_INSENSITIVE
    )

    private val DOMAIN_PATTERN = Pattern.compile(
        "^([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(:\\d+)?(/.*)?$"
    )

    fun classify(rawInput: String, engine: SearchEngine): String {
        val input = rawInput.trim()
        if (input.isEmpty()) {
            return ""
        }

        // 1. Direct standard URI schemes
        if (input.startsWith("http://", ignoreCase = true) ||
            input.startsWith("https://", ignoreCase = true) ||
            input.startsWith("file://", ignoreCase = true) ||
            input.startsWith("content://", ignoreCase = true) ||
            input.startsWith("about:", ignoreCase = true) ||
            input.startsWith("javascript:", ignoreCase = true)
        ) {
            return input
        }

        // 2. IP Addresses (e.g. 192.168.1.1, 127.0.0.1:8765) -> default HTTP
        if (IP_PORT_PATTERN.matcher(input).matches()) {
            return "http://$input"
        }

        // 3. Localhost (e.g. localhost:3000) -> default HTTP
        if (LOCALHOST_PATTERN.matcher(input).matches()) {
            return "http://$input"
        }

        // 4. Domains (e.g. github.com, sub.domain.org/path) -> default HTTPS
        if (!input.contains(" ") && DOMAIN_PATTERN.matcher(input).matches()) {
            return "https://$input"
        }

        // 5. General search query via active search engine
        val encodedQuery = try {
            URLEncoder.encode(input, "UTF-8")
        } catch (e: Exception) {
            input.replace(" ", "+")
        }
        return engine.searchUrl.replace("%s", encodedQuery)
    }
}
