package com.dologan.humblebrowser.util

import com.dologan.humblebrowser.data.db.entities.ExclusionRuleEntity

class ExclusionMatcher(rules: List<ExclusionRuleEntity>) {

    private val matchers: List<(String) -> Boolean> = rules
        .filter { it.enabled }
        .map { rule ->
            if (rule.isRegex) {
                val regex = Regex(rule.pattern)
                return@map { path: String -> regex.containsMatchIn(path) }
            } else {
                val regex = globToRegex(rule.pattern)
                return@map { path: String -> regex.matches(path) }
            }
        }

    fun isExcluded(path: String): Boolean {
        return matchers.any { it(path) }
    }

    companion object {
        fun globToRegex(glob: String): Regex {
            val sb = StringBuilder("^")
            var i = 0
            while (i < glob.length) {
                when (val c = glob[i]) {
                    '*' -> {
                        if (i + 1 < glob.length && glob[i + 1] == '*') {
                            // ** matches across directory boundaries
                            sb.append(".*")
                            i++ // skip second *
                            if (i + 1 < glob.length && glob[i + 1] == '/') {
                                i++ // skip trailing /
                            }
                        } else {
                            // * matches within a single directory
                            sb.append("[^/]*")
                        }
                    }
                    '?' -> sb.append("[^/]")
                    '.' -> sb.append("\\.")
                    '/' -> sb.append("/")
                    '[' -> sb.append("[")
                    ']' -> sb.append("]")
                    '{' -> sb.append("(")
                    '}' -> sb.append(")")
                    ',' -> sb.append("|")
                    '\\' -> {
                        if (i + 1 < glob.length) {
                            sb.append("\\").append(glob[++i])
                        }
                    }
                    else -> {
                        if ("^$.|+()".contains(c)) {
                            sb.append("\\")
                        }
                        sb.append(c)
                    }
                }
                i++
            }
            sb.append("$")
            return Regex(sb.toString())
        }

        fun parseExclusionsText(text: String): List<ExclusionRuleEntity> {
            return text.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .map { line ->
                    if (line.startsWith("/") && line.endsWith("/") && line.length > 2) {
                        ExclusionRuleEntity(
                            pattern = line.substring(1, line.length - 1),
                            isRegex = true,
                        )
                    } else {
                        ExclusionRuleEntity(
                            pattern = line,
                            isRegex = false,
                        )
                    }
                }
        }
    }
}
