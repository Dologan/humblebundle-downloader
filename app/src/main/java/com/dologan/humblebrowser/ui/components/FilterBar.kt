package com.dologan.humblebrowser.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FilterBar(
    platforms: List<String>,
    activePlatforms: Set<String>,
    onTogglePlatform: (String) -> Unit,
    extensions: List<String>,
    activeExtensions: Set<String>,
    onToggleExtension: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (platforms.isNotEmpty()) {
            Text(
                text = "Platform",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            Text(
                text = "File type",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
    }
}
