package com.dologan.humblebrowser.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Draws a vertical scrollbar indicator on the right edge of a LazyColumn.
 * Shows when scrolling, fades out after idle.
 */
fun Modifier.verticalScrollbar(
    state: LazyListState,
    color: Color = Color.Gray.copy(alpha = 0.5f),
): Modifier = composed {
    val isScrolling = state.isScrollInProgress
    val alpha by animateFloatAsState(
        targetValue = if (isScrolling) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isScrolling) 150 else 1000,
            delayMillis = if (isScrolling) 0 else 500,
        ),
        label = "scrollbar_alpha",
    )

    drawWithContent {
        drawContent()

        val totalItems = state.layoutInfo.totalItemsCount
        val visibleItems = state.layoutInfo.visibleItemsInfo
        if (totalItems == 0 || visibleItems.isEmpty()) return@drawWithContent

        val viewportHeight = size.height
        val firstVisible = state.firstVisibleItemIndex
        val scrollFraction = firstVisible.toFloat() / totalItems.coerceAtLeast(1)
        val thumbHeight = (visibleItems.size.toFloat() / totalItems * viewportHeight)
            .coerceIn(24.dp.toPx(), viewportHeight)
        val thumbOffset = scrollFraction * (viewportHeight - thumbHeight)
        val scrollbarWidth = 4.dp.toPx()

        drawRoundRect(
            color = color.copy(alpha = color.alpha * alpha),
            topLeft = Offset(size.width - scrollbarWidth - 2.dp.toPx(), thumbOffset),
            size = Size(scrollbarWidth, thumbHeight),
            cornerRadius = CornerRadius(scrollbarWidth / 2f),
        )
    }
}
