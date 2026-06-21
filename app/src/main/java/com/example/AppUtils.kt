package com.example

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object AppUtils {

    fun initAssets(context: Context) {
        val filesDir = context.filesDir
        val pythonBin = File(filesDir, "python_bin")
        val pythonStdlib = File(filesDir, "python_stdlib.zip")
        val ytdlp = File(filesDir, "yt-dlp")

        if (!pythonBin.exists()) {
            copyAssetToFile(context, "python_bin", pythonBin)
            pythonBin.setExecutable(true, false)
        }

        if (!pythonStdlib.exists()) {
            copyAssetToFile(context, "python_stdlib.zip", pythonStdlib)
        }

        if (!ytdlp.exists()) {
            copyAssetToFile(context, "yt-dlp", ytdlp)
            ytdlp.setExecutable(true, false)
        }
    }

    private fun copyAssetToFile(context: Context, assetName: String, outFile: File) {
        try {
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(outFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Creates dummy files for testing in AI Studio
            outFile.parentFile?.mkdirs()
            outFile.writeText("# Dummy file for $assetName")
        }
    }
}
