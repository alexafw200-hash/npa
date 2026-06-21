package com.example

object VideoUrlParser {
    fun extractUniversalVideoUrl(rawText: String): String? {
        val urlRegex = "(https?://[^\\s]+)".toRegex()
        val allUrls = urlRegex.findAll(rawText).map { it.value }.toList()
        
        val cleanUrls = allUrls.filter { url ->
            !url.contains("play.google") && 
            !url.contains("apple.com") && 
            !url.contains("tiktoklite") && 
            !url.contains("/download")
        }
        return cleanUrls.firstOrNull() // استخراج رابط الفيديو الصافي
    }
}
