package com.antigravity.oberon.browser

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.core.content.FileProvider
import java.io.File

class FileUploaderManager(private val activity: Activity) {

    var uploadMessageCallback: ValueCallback<Array<Uri>>? = null
    private var cameraImageUri: Uri? = null

    companion object {
        const val FILE_CHOOSER_REQUEST_CODE = 1001
    }

    fun openFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams?
    ): Boolean {
        uploadMessageCallback?.onReceiveValue(null)
        uploadMessageCallback = filePathCallback

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            val photoFile = File.createTempFile("photo_", ".jpg", activity.cacheDir)
            cameraImageUri = FileProvider.getUriForFile(
                activity,
                "com.antigravity.oberon.fileprovider",
                photoFile
            )
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri)
        } catch (e: Exception) {
            cameraImageUri = null
        }

        val contentSelectionIntent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }

        val intentArray: Array<Intent?> = if (cameraImageUri != null) {
            arrayOf(takePictureIntent)
        } else {
            emptyArray()
        }

        val chooserIntent = Intent(Intent.ACTION_CHOOSER).apply {
            putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            putExtra(Intent.EXTRA_TITLE, "Upload File")
            putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
        }

        return try {
            activity.startActivityForResult(chooserIntent, FILE_CHOOSER_REQUEST_CODE)
            true
        } catch (e: Exception) {
            uploadMessageCallback?.onReceiveValue(null)
            uploadMessageCallback = null
            false
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != FILE_CHOOSER_REQUEST_CODE) return

        if (uploadMessageCallback == null) return

        var results: Array<Uri>? = null
        if (resultCode == Activity.RESULT_OK) {
            if (data == null || data.data == null) {
                // If there is no data, then we may have taken a photo
                if (cameraImageUri != null) {
                    results = arrayOf(cameraImageUri!!)
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                } else if (data.clipData != null) {
                    val count = data.clipData!!.itemCount
                    val list = mutableListOf<Uri>()
                    for (i in 0 until count) {
                        list.add(data.clipData!!.getItemAt(i).uri)
                    }
                    results = list.toTypedArray()
                }
            }
        }

        uploadMessageCallback?.onReceiveValue(results)
        uploadMessageCallback = null
    }
}
