package com.example

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object YtDlpEngine {

    suspend fun updateEngine(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext false

                val body = response.body
                if (body != null) {
                    val ytdlpFile = File(context.filesDir, "yt-dlp")
                    FileOutputStream(ytdlpFile).use { fos ->
                        body.byteStream().copyTo(fos)
                    }
                    ytdlpFile.setExecutable(true, false)
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }
}
