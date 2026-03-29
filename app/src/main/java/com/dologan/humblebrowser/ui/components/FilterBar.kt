package com.dologan.humblebrowser.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dologan.humblebrowser.ui.browser.BrowserViewModel
import kotlin.math.roundToLong

@Composable
fun FilterBar(
    platforms: List<String>,
    activePlatforms: Set<String>,
    onTogglePlatform: (String) -> Unit,
    extensions: List<String>,
    activeExtensions: Set<String>,
    onToggleExtension: (String) -> Unit,
    tags: List<String> = emptyList(),
    activeTags: Set<String> = emptySet(),
    onToggleTag: (String) -> Unit = {},
    libraryMaxFileSize: Long = 0L,
    sizeFilterMin: Long = 0L,
    sizeFilterMax: Long = Long.MAX_VALUE,
    onSizeFilterChange: (Long, Long) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (platforms.isNotEmpty()) {
            Text("Platform", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                platforms.forEach { platform ->
                    FilterChip(
                        selected = platform in activePlatforms,
                        onClick = { onTogglePlatform(platform) },
                        label = { Text(platform.replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
        }

        if (extensions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("File type", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                extensions.forEach { ext ->
                    FilterChip(
                        selected = ext in activeExtensions,
                        onClick = { onToggleExtension(ext) },
                        label = { Text(".$ext") },
                    )
                }
            }
        }

        // Tags row — auto-hidden when library has no tags
        if (tags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tags", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                tags.forEach { tag ->
                    FilterChip(
                        selected = tag in activeTags,
                        onClick = { onToggleTag(tag) },
                        label = { Text(tag) },
                        leadingIcon = if (tag == BrowserViewModel.FAVES_TAG) {
                            { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }
        }

        // File size range slider — shown only when the library has files with known sizes
        if (libraryMaxFileSize > 0L) {
            Spacer(modifier = Modifier.height(4.dp))
            val maxMb = (libraryMaxFileSize / (1024 * 1024)).coerceAtLeast(1).toFloat()
            val currentMinMb = (sizeFilterMin / (1024 * 1024f)).coerceIn(0f, maxMb)
            val currentMaxMb = if (sizeFilterMax == Long.MAX_VALUE) maxMb
                               else (sizeFilterMax / (1024f * 1024f)).coerceIn(currentMinMb, maxMb)

            val minLabel = formatMb(currentMinMb)
            val maxLabel = if (currentMaxMb >= maxMb) "Any" else formatMb(currentMaxMb)
            Text(
                text = "File size: $minLabel – $maxLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            RangeSlider(
                value = currentMinMb..currentMaxMb,
                onValueChange = { range ->
                    val newMin = (range.start * 1024 * 1024).roundToLong()
                    val newMax = if (range.endInclusive >= maxMb) Long.MAX_VALUE
                                 else (range.endInclusive * 1024 * 1024).roundToLong()
                    onSizeFilterChange(newMin, newMax)
                },
                valueRange = 0f..maxMb,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            )
        }
    }
}

private fun formatMb(mb: Float): String = when {
    mb < 1f -> "<1 MB"
    mb >= 1024f -> "%.1f GB".format(mb / 1024f)
    else -> "${mb.roundToLong()} MB"
}
