package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

@Composable
internal fun <T : Any> MovieAdaptiveGrid(
    items: List<T>,
    minimumCellWidth: Dp,
    columnGap: Dp,
    rowGap: Dp,
    modifier: Modifier = Modifier,
    itemKey: (T) -> Any = { it },
    cell: @Composable (item: T, cellWidth: Dp) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columnCount = ((maxWidth + columnGap) / (minimumCellWidth + columnGap)).toInt().coerceAtLeast(1)
        val cellWidth = (maxWidth - columnGap * (columnCount - 1)) / columnCount
        Column(verticalArrangement = Arrangement.spacedBy(rowGap)) {
            items.chunked(columnCount).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(columnGap)) {
                    rowItems.forEach { item ->
                        key(itemKey(item)) { cell(item, cellWidth) }
                    }
                }
            }
        }
    }
}
