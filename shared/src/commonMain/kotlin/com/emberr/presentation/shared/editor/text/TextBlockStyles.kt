package com.emberr.presentation.shared.editor.text

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emberr.domain.model.BulletedListBlock
import com.emberr.domain.model.CalloutBlock
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.CodeBlock
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NumberedListBlock
import com.emberr.domain.model.QuoteBlock
import com.emberr.domain.model.TableBlock
import com.emberr.domain.model.TextAlignment
import com.emberr.domain.model.ToggleBlock
import com.emberr.domain.model.highlightColorNameOrNull
import com.emberr.domain.model.textAlignmentOrNull
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.editor.blockViews.callout.accentColor
import com.emberr.ui.theme.LocalEmberrFontStyle
import com.emberr.ui.theme.fontFamilyFor
import com.emberr.ui.theme.highlightBackgroundFor
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.unit.Dp
import com.emberr.presentation.shared.editor.DefaultBlockShape

private fun TextAlignment.toComposeTextAlign(): TextAlign = when (this) {
    TextAlignment.LEFT -> TextAlign.Left
    TextAlignment.RIGHT -> TextAlign.Right
    TextAlignment.CENTER -> TextAlign.Center
    TextAlignment.JUSTIFY -> TextAlign.Justify
}

@Composable
internal fun blockTextStyle(block: NoteBlock, isDarkTheme: Boolean): TextStyle {
    val baseStyle = when (block) {
        is HeadingBlock -> TextStyle(
            fontFamily = fontFamilyFor(LocalEmberrFontStyle.current),
            fontSize = if (block.level == 1) {
                if (isDesktopPlatform) 32.sp else 26.sp
            } else {
                if (isDesktopPlatform) 24.sp else 20.sp
            },
            lineHeight = if (block.level == 1) {
                if (isDesktopPlatform) 40.sp else 32.sp
            } else {
                if (isDesktopPlatform) 32.sp else 26.sp
            },
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        is CodeBlock -> MaterialTheme.typography.bodyLarge.copy(
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
        is QuoteBlock -> MaterialTheme.typography.bodyLarge.copy(
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurface
        )
        is CalloutBlock -> MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.SemiBold,
            color = block.calloutType.accentColor()
        )
        else -> MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    val isCheckboxChecked = block is CheckboxBlock && block.isChecked
    val applyStrikeThrough = block.isStrikeThrough || isCheckboxChecked

    return baseStyle.copy(
        fontWeight = if (block.isBold) FontWeight.Bold else baseStyle.fontWeight,
        fontStyle = if (block.isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = when {
            applyStrikeThrough && block.isUnderlined -> TextDecoration.LineThrough + TextDecoration.Underline
            applyStrikeThrough -> TextDecoration.LineThrough
            block.isUnderlined -> TextDecoration.Underline
            else -> TextDecoration.None
        },
        textAlign = block.textAlignmentOrNull()?.toComposeTextAlign() ?: TextAlign.Unspecified,
        color = if (isCheckboxChecked) MaterialTheme.colorScheme.outline else baseStyle.color,
        background = if (block.isHighlighted) {
            highlightBackgroundFor(block.highlightColorNameOrNull(), isDarkTheme)
        } else Color.Unspecified
    )
}

internal fun blockVerticalPadding(block: NoteBlock): Dp = when (block) {
    is HeadingBlock -> 8.dp
    is CodeBlock -> 4.dp
    else -> 4.dp
}

internal fun blockStartPadding(block: NoteBlock, displayedIndentationLevel: Int, desktopExtraPadding: Dp): Dp = when {
    block is TableBlock || block is DatabaseBlock -> (displayedIndentationLevel * 28).dp + desktopExtraPadding
    block is CheckboxBlock -> (18 + (displayedIndentationLevel * 28)).dp + desktopExtraPadding
    block is BulletedListBlock -> (18 + (displayedIndentationLevel * 28)).dp + desktopExtraPadding
    block is NumberedListBlock -> (18 + (displayedIndentationLevel * 28)).dp + desktopExtraPadding
    block is ToggleBlock -> (18 + (displayedIndentationLevel * 28)).dp + desktopExtraPadding
    else -> (16 + (displayedIndentationLevel * 28)).dp + desktopExtraPadding
}

internal fun blockEndPadding(block: NoteBlock, desktopExtraPadding: Dp): Dp =
    (if (block is TableBlock || block is DatabaseBlock) 0.dp else 16.dp) + desktopExtraPadding

@Composable
internal fun RowScope.blockContentModifier(block: NoteBlock): Modifier {
    val quoteAccentColor = Color(0xFFFFD54F)
    return if (block is CodeBlock) {
        Modifier.weight(1f).padding(horizontal = 4.dp)
            .background(MaterialTheme.colorScheme.surface, DefaultBlockShape).padding(12.dp)
    } else if (block is QuoteBlock) {
        Modifier.weight(1f).padding(horizontal = 4.dp).drawBehind {
            drawLine(color = quoteAccentColor, start = Offset(0f, 0f), end = Offset(0f, size.height), strokeWidth = 2.dp.toPx())
        }.padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
    } else if (block is TableBlock) {
        Modifier.weight(1f)
    } else {
        Modifier.weight(1f).padding(horizontal = 4.dp)
    }
}
