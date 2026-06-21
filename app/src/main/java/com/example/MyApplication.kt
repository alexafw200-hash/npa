package com.example

import android.app.Application
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Delete legacy dummy files from previous attempts
                val filesToRemove = listOf("python_bin", "python_stdlib.zip", "yt-dlp")
                filesToRemove.forEach { fileName ->
                    val file = java.io.File(filesDir, fileName)
                    if (file.exists() && file.length() < 100) { // Delete if it was our dummy text
                        file.delete()
                    }
                }
                
                YoutubeDL.getInstance().init(this@MyApplication)
                FFmpeg.getInstance().init(this@MyApplication)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
