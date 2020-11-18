package com.example.chatbot

import android.app.DownloadManager
import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log

private const val TAG = "DownloadPdfService"
class DownloadPdfService : IntentService("DownloadPdfService") {

    override fun onHandleIntent(intent: Intent?) {
        intent?.let {
            startDownload(it.getStringExtra(DOWNLOAD_PATH), it.getStringExtra(FILENAME))
        }
    }

    private fun startDownload(downloadPath: String, filename:String) {
        Log.d(TAG, "startDownload: $downloadPath $DESTINATION_PATH")
        val uri = Uri.parse(downloadPath)
        val request = DownloadManager.Request(uri)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED )
        request.setTitle(filename)
        request.setDestinationInExternalPublicDir(DESTINATION_PATH, filename)
        getSystemService(DownloadManager::class.java).enqueue(request)
    }

    companion object{
        const val DOWNLOAD_PATH = "download_path"
        const val FILENAME = "filename"
        val DESTINATION_PATH = Environment.DIRECTORY_DOWNLOADS
        fun getIntent(context: Context, downloadPath:String, filename: String):Intent =
            Intent(context, DownloadPdfService::class.java).apply {
                putExtra(DOWNLOAD_PATH, downloadPath)
                putExtra(FILENAME, filename)
            }
    }
}