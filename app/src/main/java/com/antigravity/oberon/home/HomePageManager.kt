package com.antigravity.oberon.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.antigravity.oberon.R
import com.antigravity.oberon.db.BookmarkDatabase
import com.google.android.material.card.MaterialCardView

data class ShortcutTile(
    val title: String,
    val url: String,
    val subtitle: String
)

class HomePageManager(
    private val context: Context,
    private val container: LinearLayout,
    private val bookmarkDb: BookmarkDatabase,
    private val onUrlSelected: (String) -> Unit
) {
    private val defaultShortcuts = listOf(
        ShortcutTile("Google", "https://www.google.com", "Search"),
        ShortcutTile("GitHub", "https://github.com", "Code"),
        ShortcutTile("Wikipedia", "https://wikipedia.org", "Knowledge"),
        ShortcutTile("Reddit", "https://reddit.com", "Community"),
        ShortcutTile("Port 3000", "http://127.0.0.1:3000", "React/Next Dev"),
        ShortcutTile("Port 5173", "http://127.0.0.1:5173", "Vite Dev"),
        ShortcutTile("Port 8000", "http://127.0.0.1:8000", "FastAPI / Python")
    )

    fun render(rootView: View) {
        val glShortcuts = rootView.findViewById<androidx.gridlayout.widget.GridLayout>(R.id.glShortcuts)
        val llTopVisited = rootView.findViewById<LinearLayout>(R.id.llTopVisited)

        glShortcuts.removeAllViews()
        defaultShortcuts.forEach { tile ->
            val card = createShortcutCard(tile.title, tile.subtitle) {
                onUrlSelected(tile.url)
            }
            glShortcuts.addView(card)
        }

        // Render Top Visited
        llTopVisited.removeAllViews()
        val topSites = bookmarkDb.getTopVisited(5)
        if (topSites.isEmpty()) {
            val emptyTv = TextView(context).apply {
                text = "Visited sites will automatically appear here."
                setTextColor(ContextCompat.getColor(context, R.color.text_tertiary))
                textSize = 12f
                setPadding(8, 16, 8, 16)
            }
            llTopVisited.addView(emptyTv)
        } else {
            topSites.forEach { site ->
                val row = TextView(context).apply {
                    text = "${site.title} (${site.visitCount} visits)"
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    textSize = 13f
                    setPadding(12, 16, 12, 16)
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener { onUrlSelected(site.url) }
                }
                llTopVisited.addView(row)
            }
        }
    }

    private fun createShortcutCard(title: String, subtitle: String, onClick: () -> Unit): View {
        val card = MaterialCardView(context).apply {
            radius = 16f
            cardElevation = 2f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.charcoal_surface))
            strokeColor = ContextCompat.getColor(context, R.color.border_subtle)
            strokeWidth = 1

            val params = androidx.gridlayout.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = androidx.gridlayout.widget.GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = androidx.gridlayout.widget.GridLayout.spec(androidx.gridlayout.widget.GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            layoutParams = params

            val inner = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)

                val tvTitle = TextView(context).apply {
                    text = title
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    textSize = 13f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
                val tvSub = TextView(context).apply {
                    text = subtitle
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    textSize = 10f
                    setPadding(0, 4, 0, 0)
                }
                addView(tvTitle)
                addView(tvSub)
            }
            addView(inner)
            setOnClickListener { onClick() }
        }
        return card
    }
}
