package com.antigravity.oberon.browser

import android.app.AlertDialog
import android.content.Context
import android.net.http.SslCertificate
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.antigravity.oberon.R
import java.text.DateFormat

enum class SslState {
    SECURE,
    INSECURE,
    ERROR
}

object SslSecurityManager {

    fun updateSslIcon(iconView: ImageView, state: SslState) {
        val context = iconView.context
        when (state) {
            SslState.SECURE -> {
                iconView.setImageResource(android.R.drawable.ic_secure)
                iconView.setColorFilter(ContextCompat.getColor(context, R.color.accent_sage))
                iconView.contentDescription = context.getString(R.string.ssl_secure)
            }
            SslState.INSECURE -> {
                iconView.setImageResource(android.R.drawable.ic_partial_secure)
                iconView.setColorFilter(ContextCompat.getColor(context, R.color.status_warning))
                iconView.contentDescription = context.getString(R.string.ssl_insecure)
            }
            SslState.ERROR -> {
                iconView.setImageResource(android.R.drawable.stat_notify_error)
                iconView.setColorFilter(ContextCompat.getColor(context, R.color.status_error))
                iconView.contentDescription = "SSL Certificate Error"
            }
        }
    }

    fun showCertificateDetails(context: Context, certificate: SslCertificate?, url: String) {
        if (certificate == null) {
            AlertDialog.Builder(context)
                .setTitle("Site Security")
                .setMessage("No SSL certificate found for:\n$url\n\nConnection is unencrypted.")
                .setPositiveButton("Close", null)
                .show()
            return
        }

        val issuedTo = certificate.issuedTo.dName
        val issuedBy = certificate.issuedBy.dName
        val validFrom = DateFormat.getDateInstance().format(certificate.validNotBeforeDate)
        val validTo = DateFormat.getDateInstance().format(certificate.validNotAfterDate)

        val message = """
            Issued To:
            $issuedTo
            
            Issued By:
            $issuedBy
            
            Validity:
            From $validFrom to $validTo
        """.trimIndent()

        AlertDialog.Builder(context)
            .setTitle("SSL Certificate")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
