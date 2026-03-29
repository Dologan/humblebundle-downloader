package com.dologan.humblebrowser.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dologan.humblebrowser.domain.model.ViewMode

@Composable
fun ViewModeSelector(
    currentMode: ViewMode,
    onModeSelected: (ViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    // ALPHABETICAL and ALPHABETICAL_DESC both highlight the A-Z button
    val modes = listOf(
        ViewMode.BY_BUNDLE to "By Bundle",
        ViewMode.BY_TYPE to "By Type",
        ViewMode.ALPHABETICAL to "A-Z",
    )

    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, (mode, label) ->
            val isAlphaMode = mode == ViewMode.ALPHABETICAL
            val isSelected = if (isAlphaMode) {
                currentMode == ViewMode.ALPHABETICAL || currentMode == ViewMode.ALPHABETICAL_DESC
            } else {
                currentMode == mode
            }
            val displayLabel = if (isAlphaMode && currentMode == ViewMode.ALPHABETICAL_DESC) "Z-A" else label

            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                onClick = {
                    if (isAlphaMode) {
                        // Toggle between A-Z and Z-A when tapping the same button
                        onModeSelected(
                            if (currentMode == ViewMode.ALPHABETICAL) ViewMode.ALPHABETICAL_DESC
                            else ViewMode.ALPHABETICAL
                        )
                    } else {
                        onModeSelected(mode)
                    }
                },
                selected = isSelected,
            ) {
                Text(displayLabel)
            }
        }
    }
}
