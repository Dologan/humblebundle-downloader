package com.dologan.humblebrowser.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun LargeFileConfirmDialog(
    filename: String,
    fileSize: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Large File Download") },
        text = {
            Text("\"$filename\" is $fileSize. Are you sure you want to download it?")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Download") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
