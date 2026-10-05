package com.antigravity.oberon.downloads

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.antigravity.oberon.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

data class DownloadedFileItem(
    val id: Long,
    val title: String,
    val localUri: String?,
    val mediaType: String?
)

class DownloadsBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_downloads, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val rvDownloads = view.findViewById<RecyclerView>(R.id.rvDownloads)
        rvDownloads.layoutManager = LinearLayoutManager(requireContext())

        val items = queryCompletedDownloads(requireContext())
        rvDownloads.adapter = DownloadsAdapter(items) { item ->
            openDownloadedFile(requireContext(), item)
        }
    }

    private fun queryCompletedDownloads(context: Context): List<DownloadedFileItem> {
        val list = mutableListOf<DownloadedFileItem>()
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager ?: return list
        val query = DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_SUCCESSFUL)
        val cursor = dm.query(query)
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))
                val title = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: "File"
                val localUri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                val mediaType = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE))
                list.add(DownloadedFileItem(id, title, localUri, mediaType))
            }
        }
        return list
    }

    private fun openDownloadedFile(context: Context, item: DownloadedFileItem) {
        val uriString = item.localUri ?: return
        try {
            val file = File(Uri.parse(uriString).path ?: "")
            val contentUri = FileProvider.getUriForFile(
                context,
                "com.antigravity.oberon.fileprovider",
                file
            )

            val mime = if (item.title.endsWith(".apk", ignoreCase = true)) {
                "application/vnd.android.package-archive"
            } else {
                item.mediaType ?: "*/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private class DownloadsAdapter(
        private val items: List<DownloadedFileItem>,
        private val onItemClick: (DownloadedFileItem) -> Unit
    ) : RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTitle: TextView = itemView.findViewById(android.R.id.text1)
            val tvSub: TextView = itemView.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvSub.text = item.mediaType ?: "Downloaded"
            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount(): Int = items.size
    }
}
