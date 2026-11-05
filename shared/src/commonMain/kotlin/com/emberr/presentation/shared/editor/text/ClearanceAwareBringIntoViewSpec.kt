package com.emberr.presentation.shared.editor.text

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
internal class ClearanceAwareBringIntoViewSpec(
    private val topInsetPx: Float,
    private val bottomInsetPx: Float,
    private val maxHeightShownWholePx: Float
) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val visibleTop = topInsetPx
        val visibleBottom = containerSize - bottomInsetPx
        val leadingEdge = offset
        val trailingEdge = offset + size
        val topDelta = leadingEdge - visibleTop
        val bottomDelta = trailingEdge - visibleBottom
        return when {
            leadingEdge >= visibleTop && trailingEdge <= visibleBottom -> 0f
            leadingEdge < visibleTop && trailingEdge > visibleBottom -> 0f
            size > maxHeightShownWholePx && leadingEdge < visibleBottom && trailingEdge > visibleTop -> 0f
            abs(topDelta) < abs(bottomDelta) -> topDelta
            else -> bottomDelta
        }
    }
}
