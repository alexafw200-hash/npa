package com.example

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object PythonRunner {
    fun executeYtDlp(context: Context, url: String): Flow<String> = flow {
        val filesDir = context.filesDir
        val pythonBin = File(filesDir, "python").absolutePath
        val ytdlp = File(filesDir, "yt-dlp").absolutePath
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        downloadsDir?.mkdirs()
        
        val outputPath = File(downloadsDir, "%(title)s.%(ext)s").absolutePath

        try {
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
                line?.let { emit(it) }
            }

            val exitCode = process.waitFor()
            emit("Process finished with code: $exitCode")

        } catch (e: Exception) {
            e.printStackTrace()
            emit("Error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}
