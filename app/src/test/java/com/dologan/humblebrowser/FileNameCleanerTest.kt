package com.dologan.humblebrowser

import com.dologan.humblebrowser.util.FileNameCleaner
import org.junit.Assert.assertEquals
import org.junit.Test

class FileNameCleanerTest {

    @Test
    fun `replaces plus with underscore`() {
        assertEquals("Game_Extra", FileNameCleaner.clean("Game+Extra"))
    }

    @Test
    fun `replaces colon with dash`() {
        assertEquals("Game - Title", FileNameCleaner.clean("Game: Title"))
    }

    @Test
    fun `removes invalid characters`() {
        assertEquals("Hello World", FileNameCleaner.clean("Hello <World>"))
    }

    @Test
    fun `trims whitespace and trailing dots`() {
        assertEquals("Test", FileNameCleaner.clean("  Test.  "))
    }

    @Test
    fun `blank becomes Unknown`() {
        assertEquals("Unknown", FileNameCleaner.clean("<<<>>>"))
    }

    @Test
    fun `preserves allowed special chars`() {
        assertEquals("Game [Deluxe] (2024)", FileNameCleaner.clean("Game [Deluxe] (2024)"))
    }
}
