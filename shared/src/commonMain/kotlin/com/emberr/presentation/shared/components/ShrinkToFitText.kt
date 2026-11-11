package com.emberr.presentation.shared.components

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.modifiers.TextAutoSizeLayoutScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

private const val SMALLEST_SHRINK_SCALE = 0.85f
private const val SHRINK_STEP_SP = 0.5f

fun shrinkToFitOneLine(style: TextStyle): TextAutoSize? =
    if (style.fontSize.isSp) ShrinkToFitOneLineAutoSize(style.fontSize) else null

private class ShrinkToFitOneLineAutoSize(private val normalFontSize: TextUnit) : TextAutoSize {
    override fun TextAutoSizeLayoutScope.getFontSize(constraints: Constraints, text: AnnotatedString): TextUnit {
        val smallestSize = normalFontSize.value * SMALLEST_SHRINK_SCALE
        var trySize = normalFontSize.value
        while (trySize > smallestSize) {
            if (performLayout(constraints, text, trySize.sp).fitsOnOneLine()) return trySize.sp
            trySize -= SHRINK_STEP_SP
        }
        return smallestSize.sp
    }

    override fun equals(other: Any?): Boolean =
        other is ShrinkToFitOneLineAutoSize && other.normalFontSize == normalFontSize

    override fun hashCode(): Int = normalFontSize.hashCode()
}

private fun TextLayoutResult.fitsOnOneLine(): Boolean =
    !hasVisualOverflow && (lineCount == 0 || !isLineEllipsized(lineCount - 1))
