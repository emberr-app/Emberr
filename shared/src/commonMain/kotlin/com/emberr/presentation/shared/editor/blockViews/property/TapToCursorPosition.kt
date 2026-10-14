package com.emberr.presentation.shared.editor.blockViews.property

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints

internal class TapToCursorPosition {
    var tapAreaCoordinates: LayoutCoordinates? = null
    var textAreaCoordinates: LayoutCoordinates? = null

    fun cursorOffsetAt(
        tapPosition: Offset,
        text: String,
        style: TextStyle,
        wrapsText: Boolean,
        textMeasurer: TextMeasurer
    ): Int? {
        val tapArea = tapAreaCoordinates?.takeIf { it.isAttached } ?: return null
        val textArea = textAreaCoordinates?.takeIf { it.isAttached } ?: return null
        val tapInText = textArea.localPositionOf(tapArea, tapPosition)
        val textLayout = textMeasurer.measure(
            text = text,
            style = style,
            softWrap = wrapsText,
            maxLines = if (wrapsText) Int.MAX_VALUE else 1,
            constraints = if (wrapsText) Constraints(maxWidth = textArea.size.width) else Constraints()
        )
        return textLayout.getOffsetForPosition(tapInText)
    }
}
