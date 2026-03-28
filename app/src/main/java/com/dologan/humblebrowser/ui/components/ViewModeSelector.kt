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
    val modes = listOf(
        ViewMode.BY_BUNDLE to "By Bundle",
        ViewMode.BY_TYPE to "By Type",
        ViewMode.ALPHABETICAL to "A-Z",
    )

    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                onClick = { onModeSelected(mode) },
                selected = currentMode == mode,
            ) {
                Text(label)
            }
        }
    }
}
