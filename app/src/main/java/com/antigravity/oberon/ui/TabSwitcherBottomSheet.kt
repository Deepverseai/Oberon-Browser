package com.antigravity.oberon.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.antigravity.oberon.R
import com.antigravity.oberon.tabs.TabManager
import com.antigravity.oberon.tabs.TabModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView

class TabSwitcherBottomSheet(
    private val tabManager: TabManager,
    private val onNewTabRequested: (isIncognito: Boolean) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var adapter: TabAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_tabs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val rvTabs = view.findViewById<RecyclerView>(R.id.rvTabs)
        val btnNewTab = view.findViewById<ImageButton>(R.id.btnNewTab)
        val btnNewIncognito = view.findViewById<ImageButton>(R.id.btnNewIncognitoTab)

        adapter = TabAdapter(
            tabs = tabManager.getAllTabs(),
            activeTabId = tabManager.getActiveTab()?.id,
            onTabSelected = { tab ->
                tabManager.selectTab(tab.id)
                dismiss()
            },
            onTabClosed = { tab ->
                val newActive = tabManager.closeTab(tab.id)
                if (newActive == null && tabManager.getTabCount() == 0) {
                    onNewTabRequested(false)
                }
                adapter.updateData(tabManager.getAllTabs(), tabManager.getActiveTab()?.id)
            }
        )
        rvTabs.adapter = adapter

        btnNewTab.setOnClickListener {
            onNewTabRequested(false)
            dismiss()
        }

        btnNewIncognito.setOnClickListener {
            onNewTabRequested(true)
            dismiss()
        }
    }

    private class TabAdapter(
        private var tabs: List<TabModel>,
        private var activeTabId: String?,
        private val onTabSelected: (TabModel) -> Unit,
        private val onTabClosed: (TabModel) -> Unit
    ) : RecyclerView.Adapter<TabAdapter.TabViewHolder>() {

        fun updateData(newTabs: List<TabModel>, newActiveId: String?) {
            tabs = newTabs
            activeTabId = newActiveId
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tab, parent, false)
            return TabViewHolder(view)
        }

        override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
            val tab = tabs[position]
            val isActive = (tab.id == activeTabId)
            holder.bind(tab, isActive, onTabSelected, onTabClosed)
        }

        override fun getItemCount(): Int = tabs.size

        class TabViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val card: MaterialCardView = itemView.findViewById(R.id.cardTab)
            private val tvTitle: TextView = itemView.findViewById(R.id.tvTabTitle)
            private val tvUrl: TextView = itemView.findViewById(R.id.tvTabUrl)
            private val btnClose: ImageButton = itemView.findViewById(R.id.btnCloseTab)
            private val ivIncognito: ImageView = itemView.findViewById(R.id.ivIncognitoBadge)

            fun bind(
                tab: TabModel,
                isActive: Boolean,
                onSelected: (TabModel) -> Unit,
                onClosed: (TabModel) -> Unit
            ) {
                tvTitle.text = tab.title
                tvUrl.text = tab.url
                ivIncognito.visibility = if (tab.isIncognito) View.VISIBLE else View.GONE

                val context = itemView.context
                val strokeColor = if (isActive) {
                    ContextCompat.getColor(context, R.color.accent_sage)
                } else {
                    ContextCompat.getColor(context, R.color.border_subtle)
                }
                card.strokeColor = strokeColor
                card.strokeWidth = if (isActive) 4 else 2

                card.setOnClickListener { onSelected(tab) }
                btnClose.setOnClickListener { onClosed(tab) }
            }
        }
    }
}
