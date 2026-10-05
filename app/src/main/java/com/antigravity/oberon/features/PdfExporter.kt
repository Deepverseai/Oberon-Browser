package com.antigravity.oberon.features

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.widget.Toast

object PdfExporter {

    fun exportPdf(activity: Activity, webView: WebView) {
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(activity, "Print service is not available on this device", Toast.LENGTH_SHORT).show()
            return
        }

        val jobName = "Oberon_${webView.title ?: "Document"}_${System.currentTimeMillis()}"
        val printAdapter = webView.createPrintDocumentAdapter(jobName)

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        try {
            printManager.print(jobName, printAdapter, printAttributes)
        } catch (e: Exception) {
            Toast.makeText(activity, "Failed to start PDF export: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
