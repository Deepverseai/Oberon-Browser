package com.antigravity.oberon.preview

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.antigravity.oberon.R

enum class PreviewMode {
    WEBSITE,
    APP
}

class PreviewModeManager(private val context: Context) {
    private var currentMode = PreviewMode.WEBSITE

    fun getMode(): PreviewMode = currentMode

    fun setPreviewMode(mode: PreviewMode, webView: WebView, container: FrameLayout) {
        currentMode = mode
        val density = context.resources.displayMetrics.density

        when (mode) {
            PreviewMode.WEBSITE -> {
                container.setBackgroundColor(Color.TRANSPARENT)
                val params = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webView.layoutParams = params
                webView.setPadding(0, 0, 0, 0)
            }
            PreviewMode.APP -> {
                // Simulated Mobile Device Frame: 390 x 844 dp (iPhone 14 / standard modern mobile viewport)
                val targetWidthPx = (390 * density).toInt()
                val targetHeightPx = (844 * density).toInt()

                val availableWidth = container.width
                val availableHeight = container.height

                // Scale to fit available screen if needed
                val finalWidth = if (availableWidth > 0 && availableWidth < targetWidthPx) availableWidth else targetWidthPx
                val finalHeight = if (availableHeight > 0 && availableHeight < targetHeightPx) availableHeight else targetHeightPx

                container.setBackgroundColor(ContextCompat.getColor(context, R.color.obsidian_base))

                val params = FrameLayout.LayoutParams(finalWidth, finalHeight).apply {
                    gravity = Gravity.CENTER
                }
                webView.layoutParams = params
            }
        }
    }
}
