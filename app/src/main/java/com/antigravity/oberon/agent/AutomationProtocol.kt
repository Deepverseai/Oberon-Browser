package com.antigravity.oberon.agent

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.WebView
import com.antigravity.oberon.preview.PreviewMode
import com.antigravity.oberon.preview.PreviewModeManager
import com.antigravity.oberon.preview.VirtualCursor
import com.antigravity.oberon.tabs.TabManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AutomationProtocol(
    private val tabManager: TabManager,
    private val previewModeManager: PreviewModeManager,
    private val onNewTab: (url: String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val telemetryLogs = mutableListOf<String>()

    fun recordLog(message: String) {
        synchronized(telemetryLogs) {
            telemetryLogs.add(message)
            if (telemetryLogs.size > 200) {
                telemetryLogs.removeAt(0)
            }
        }
    }

    fun handleCommand(jsonCommand: String): String {
        return try {
            val req = JSONObject(jsonCommand)
            val action = req.optString("action", "")
            when (action) {
                "navigate" -> executeNavigate(req.optString("url"))
                "new_tab" -> executeNewTab(req.optString("url"))
                "click" -> executeClick(req.optString("selector"))
                "type" -> executeType(req.optString("selector"), req.optString("text"))
                "extract_text" -> executeExtractText()
                "get_html" -> executeGetHtml()
                "screenshot" -> executeScreenshot()
                "set_preview_mode" -> executeSetPreviewMode(req.optString("mode"))
                "get_telemetry" -> executeGetTelemetry()
                else -> JSONObject().put("status", "error").put("message", "Unknown action: $action").toString()
            }
        } catch (e: Exception) {
            JSONObject().put("status", "error").put("message", e.message).toString()
        }
    }

    private fun executeNavigate(url: String): String {
        val latch = CountDownLatch(1)
        mainHandler.post {
            tabManager.getActiveTab()?.webView?.loadUrl(url)
            latch.countDown()
        }
        latch.await(2, TimeUnit.SECONDS)
        return JSONObject().put("status", "ok").put("url", url).toString()
    }

    private fun executeNewTab(url: String): String {
        val latch = CountDownLatch(1)
        mainHandler.post {
            onNewTab(url.ifBlank { "https://www.google.com" })
            latch.countDown()
        }
        latch.await(2, TimeUnit.SECONDS)
        return JSONObject().put("status", "ok").put("message", "Tab created").toString()
    }

    private fun executeClick(selector: String): String {
        val latch = CountDownLatch(1)
        var resultJson = JSONObject().put("status", "error")

        mainHandler.post {
            val webView = tabManager.getActiveTab()?.webView
            if (webView == null) {
                latch.countDown()
                return@post
            }

            VirtualCursor.injectCursor(webView)

            val clickScript = """
                (function() {
                    const el = document.querySelector('$selector');
                    if (!el) return JSON.stringify({found: false});
                    const rect = el.getBoundingClientRect();
                    const x = rect.left + rect.width / 2;
                    const y = rect.top + rect.height / 2;
                    if (window.__oberon_glide) window.__oberon_glide(x, y);
                    setTimeout(() => {
                        if (window.__oberon_ripple) window.__oberon_ripple(x, y);
                        el.click();
                    }, 250);
                    return JSON.stringify({found: true, x: x, y: y});
                })();
            """.trimIndent()

            webView.evaluateJavascript(clickScript) { res ->
                resultJson = JSONObject().put("status", "ok").put("result", res)
                latch.countDown()
            }
        }

        latch.await(3, TimeUnit.SECONDS)
        return resultJson.toString()
    }

    private fun executeType(selector: String, text: String): String {
        val latch = CountDownLatch(1)
        var resultJson = JSONObject().put("status", "error")

        mainHandler.post {
            val webView = tabManager.getActiveTab()?.webView
            if (webView == null) {
                latch.countDown()
                return@post
            }

            val escapedText = JSONObject.quote(text)
            val typeScript = """
                (function() {
                    const el = document.querySelector('$selector');
                    if (!el) return JSON.stringify({found: false});
                    el.focus();
                    el.value = $escapedText;
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    return JSON.stringify({found: true});
                })();
            """.trimIndent()

            webView.evaluateJavascript(typeScript) { res ->
                resultJson = JSONObject().put("status", "ok").put("result", res)
                latch.countDown()
            }
        }

        latch.await(3, TimeUnit.SECONDS)
        return resultJson.toString()
    }

    private fun executeExtractText(): String {
        val latch = CountDownLatch(1)
        var textResult = ""

        mainHandler.post {
            val webView = tabManager.getActiveTab()?.webView
            webView?.evaluateJavascript("(function() { return document.body.innerText; })();") { res ->
                textResult = res ?: ""
                latch.countDown()
            } ?: latch.countDown()
        }

        latch.await(3, TimeUnit.SECONDS)
        return JSONObject().put("status", "ok").put("text", textResult).toString()
    }

    private fun executeGetHtml(): String {
        val latch = CountDownLatch(1)
        var htmlResult = ""

        mainHandler.post {
            val webView = tabManager.getActiveTab()?.webView
            webView?.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { res ->
                htmlResult = res ?: ""
                latch.countDown()
            } ?: latch.countDown()
        }

        latch.await(3, TimeUnit.SECONDS)
        return JSONObject().put("status", "ok").put("html", htmlResult).toString()
    }

    private fun executeScreenshot(): String {
        val latch = CountDownLatch(1)
        var base64Img = ""

        mainHandler.post {
            val webView = tabManager.getActiveTab()?.webView
            if (webView != null && webView.width > 0 && webView.height > 0) {
                val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                webView.draw(canvas)

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 85, stream)
                base64Img = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
            }
            latch.countDown()
        }

        latch.await(3, TimeUnit.SECONDS)
        return JSONObject().put("status", "ok").put("image_base64", base64Img).toString()
    }

    private fun executeSetPreviewMode(modeStr: String): String {
        val latch = CountDownLatch(1)
        val mode = if (modeStr.equals("app", ignoreCase = true)) PreviewMode.APP else PreviewMode.WEBSITE

        mainHandler.post {
            val webView = tabManager.getActiveTab()?.webView
            val container = webView?.parent as? android.widget.FrameLayout
            if (webView != null && container != null) {
                previewModeManager.setPreviewMode(mode, webView, container)
            }
            latch.countDown()
        }

        latch.await(2, TimeUnit.SECONDS)
        return JSONObject().put("status", "ok").put("mode", mode.name.lowercase()).toString()
    }

    private fun executeGetTelemetry(): String {
        val logsArray = JSONArray()
        synchronized(telemetryLogs) {
            telemetryLogs.forEach { logsArray.put(it) }
        }
        return JSONObject().put("status", "ok").put("logs", logsArray).toString()
    }
}
