package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _urlText = MutableStateFlow("")
    val urlText: StateFlow<String> = _urlText.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    fun updateUrl(url: String) {
        _urlText.value = url
    }

    fun startDownload() {
        val url = _urlText.value
        if (_isDownloading.value || url.isBlank()) return

        _isDownloading.value = true
        _logs.value = listOf("Starting download...")

        viewModelScope.launch {
            PythonRunner.executeYtDlp(getApplication(), url).collect { logline ->
                _logs.value = _logs.value + logline
            }
            _isDownloading.value = false
        }
    }
}
