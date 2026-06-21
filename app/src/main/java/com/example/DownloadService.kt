package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class DownloadService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    companion object {
        private const val CHANNEL_ID = "download_channel"
        private const val NOTIFICATION_ID = 1

        private val _downloadStatus = MutableStateFlow("Idle")
        val downloadStatus: StateFlow<String> = _downloadStatus

        private val _downloadProgress = MutableStateFlow(0f)
        val downloadProgress: StateFlow<Float> = _downloadProgress

        const val EXTRA_URL = "EXTRA_URL"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL)
        if (url != null) {
            val notification = createNotification("Starting download...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            startDownload(url)
        }
        return START_NOT_STICKY
    }

    private fun startDownload(url: String) {
        scope.launch {
            _downloadStatus.value = "Preparing..."
            _downloadProgress.value = 0f

            val pythonBin = File(filesDir, "python_bin").absolutePath
            val ytdlp = File(filesDir, "yt-dlp").absolutePath
            val downloadsDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            // It will ensure the downloads dir exists
            downloadsDir?.mkdirs()
            val outputPath = File(downloadsDir, "%(title)s.%(ext)s").absolutePath

            try {
                File(filesDir, "python_bin").setExecutable(true, false)
                File(filesDir, "yt-dlp").setExecutable(true, false)

                val processBuilder = ProcessBuilder(
                    pythonBin, ytdlp, url, "-o", outputPath
                )
                
                val env = processBuilder.environment()
                env["PYTHONHOME"] = filesDir.absolutePath
                env["PYTHONPATH"] = File(filesDir, "python_stdlib.zip").absolutePath

                processBuilder.redirectErrorStream(true)
                val process = processBuilder.start()

                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue
                    parseProgress(currentLine)
                }

                val exitCode = process.waitFor()
                if (exitCode == 0) {
                    _downloadStatus.value = "Completed!"
                    _downloadProgress.value = 1f
                    updateNotification("Download completed!")
                } else {
                    _downloadStatus.value = "Failed (Code: $exitCode)"
                    updateNotification("Download failed (Code: $exitCode)")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _downloadStatus.value = "Error: ${e.localizedMessage}"
                updateNotification("Download Error")
            } finally {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                } else {
                    stopForeground(true)
                }
                stopSelf()
            }
        }
    }

    private fun parseProgress(line: String) {
        if (line.contains("[download]") && line.contains("%")) {
            try {
                val percentageStr = line.substringAfter("[download]").substringBefore("%").trim()
                val value = percentageStr.toFloatOrNull()
                if (value != null) {
                    _downloadProgress.value = value / 100f
                    _downloadStatus.value = "Downloading: $percentageStr%"
                    updateNotification("Downloading: $percentageStr%")
                }
            } catch (e: Exception) {
                // Ignore parse errors from single lines
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Native Video DL")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
