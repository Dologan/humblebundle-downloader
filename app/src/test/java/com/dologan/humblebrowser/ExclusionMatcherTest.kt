package com.dologan.humblebrowser

import com.dologan.humblebrowser.data.db.entities.ExclusionRuleEntity
import com.dologan.humblebrowser.util.ExclusionMatcher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExclusionMatcherTest {

    @Test
    fun `glob star matches files with extension`() {
        val matcher = ExclusionMatcher(listOf(
            ExclusionRuleEntity(pattern = "*.exe", isRegex = false, enabled = true),
        ))
        assertTrue(matcher.isExcluded("game.exe"))
        assertFalse(matcher.isExcluded("game.pdf"))
        assertFalse(matcher.isExcluded("Bundle/Product/game.exe")) // single * doesn't cross /
    }

    @Test
    fun `glob double star matches across directories`() {
        val matcher = ExclusionMatcher(listOf(
            ExclusionRuleEntity(pattern = "**/*.exe", isRegex = false, enabled = true),
        ))
        assertTrue(matcher.isExcluded("Bundle/Product/game.exe"))
        assertTrue(matcher.isExcluded("game.exe"))
        assertFalse(matcher.isExcluded("Bundle/Product/game.pdf"))
    }

    @Test
    fun `glob directory pattern`() {
        val matcher = ExclusionMatcher(listOf(
            ExclusionRuleEntity(pattern = "Windows Games/**", isRegex = false, enabled = true),
        ))
        assertTrue(matcher.isExcluded("Windows Games/Product/file.zip"))
        assertFalse(matcher.isExcluded("Mac Games/Product/file.zip"))
    }

    @Test
    fun `regex pattern`() {
        val matcher = ExclusionMatcher(listOf(
            ExclusionRuleEntity(pattern = ".*\\.(msi|dmg)$", isRegex = true, enabled = true),
        ))
        assertTrue(matcher.isExcluded("Bundle/Product/installer.msi"))
        assertTrue(matcher.isExcluded("Bundle/Product/app.dmg"))
        assertFalse(matcher.isExcluded("Bundle/Product/book.pdf"))
    }

    @Test
    fun `disabled rules are ignored`() {
        val matcher = ExclusionMatcher(listOf(
            ExclusionRuleEntity(pattern = "*.exe", isRegex = false, enabled = false),
        ))
        assertFalse(matcher.isExcluded("game.exe"))
    }

    @Test
    fun `empty rules exclude nothing`() {
        val matcher = ExclusionMatcher(emptyList())
        assertFalse(matcher.isExcluded("anything/at/all.txt"))
    }

    @Test
    fun `parseExclusionsText handles comments and blank lines`() {
        val text = """
            # Comment
            *.exe

            /.*\.dmg$/
            Games/**
        """.trimIndent()

        val rules = ExclusionMatcher.parseExclusionsText(text)
        assertTrue(rules.size == 3)
        assertFalse(rules[0].isRegex)
        assertTrue(rules[0].pattern == "*.exe")
        assertTrue(rules[1].isRegex)
        assertTrue(rules[1].pattern == ".*\\.dmg\$")
        assertFalse(rules[2].isRegex)
        assertTrue(rules[2].pattern == "Games/**")
    }
}
