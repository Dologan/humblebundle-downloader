package com.dologan.humblebrowser.util

object FileNameCleaner {

    private val ALLOWED_CHARS = Regex("[^a-zA-Z0-9 _.,\\-\\[\\]()&!']")

    fun clean(name: String): String {
        return name
            .replace("+", "_")
            .replace(":", " -")
            .replace(ALLOWED_CHARS, "")
            .trim()
            .trimEnd('.')
            .ifBlank { "Unknown" }
    }
}
