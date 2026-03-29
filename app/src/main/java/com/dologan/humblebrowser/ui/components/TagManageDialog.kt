package com.dologan.humblebrowser.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dologan.humblebrowser.ui.browser.BrowserViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagManageDialog(
    itemName: String,
    currentTags: Set<String>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newTag by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tags — $itemName") },
        text = {
            Column {
                if (currentTags.isEmpty()) {
                    Text(
                        text = "No tags yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val sorted = currentTags.sortedWith(
                            compareBy { if (it == BrowserViewModel.FAVES_TAG) "" else it }
                        )
                        sorted.forEach { tag ->
                            AssistChip(
                                onClick = {},
                                label = { Text(tag) },
                                leadingIcon = if (tag == BrowserViewModel.FAVES_TAG) {
                                    { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(ChipDefaults.IconSize)) }
                                } else null,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onRemoveTag(tag) },
                                        modifier = Modifier.size(ChipDefaults.IconSize),
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove $tag", modifier = Modifier.size(ChipDefaults.IconSize))
                                    }
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick-add Faves if not already tagged
                if (BrowserViewModel.FAVES_TAG !in currentTags) {
                    TextButton(
                        onClick = { onAddTag(BrowserViewModel.FAVES_TAG) },
                        modifier = Modifier.padding(bottom = 4.dp),
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(" Add to Faves", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = { newTag = it },
                        label = { Text("New tag") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            if (newTag.isNotBlank()) {
                                onAddTag(newTag.trim())
                                newTag = ""
                            }
                        },
                        enabled = newTag.isNotBlank(),
                    ) { Text("Add") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}
