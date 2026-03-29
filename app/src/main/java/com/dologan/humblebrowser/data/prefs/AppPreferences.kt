package com.dologan.humblebrowser.data.prefs

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(private val prefs: SharedPreferences) {

    private val _largeFileSizeBytes = MutableStateFlow(
        prefs.getLong(KEY_LARGE_FILE_SIZE_MB, DEFAULT_LARGE_FILE_MB) * 1024 * 1024
    )
    val largeFileSizeBytes: StateFlow<Long> = _largeFileSizeBytes.asStateFlow()

    fun getLargeFileSizeMb(): Long =
        prefs.getLong(KEY_LARGE_FILE_SIZE_MB, DEFAULT_LARGE_FILE_MB)

    fun setLargeFileSizeMb(mb: Long) {
        prefs.edit().putLong(KEY_LARGE_FILE_SIZE_MB, mb).apply()
        _largeFileSizeBytes.value = mb * 1024 * 1024
    }

    companion object {
        const val DEFAULT_LARGE_FILE_MB = 50L
        private const val KEY_LARGE_FILE_SIZE_MB = "large_file_size_mb"
    }
}
