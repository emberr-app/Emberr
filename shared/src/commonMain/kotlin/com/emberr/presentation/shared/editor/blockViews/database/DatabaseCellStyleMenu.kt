@file:OptIn(ExperimentalLayoutApi::class)

package com.emberr.presentation.shared.editor.blockViews.database

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.emberr.domain.database.DatabaseStyleTarget
import com.emberr.domain.database.styleOf
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseCellStyle
import com.emberr.domain.model.DatabaseColumnTarget
import com.emberr.domain.model.TextAlignment
import com.emberr.presentation.shared.editor.formatting.removeHighlightMark
import com.emberr.ui.theme.HighlightColor
import com.emberr.ui.theme.LocalAppIsDark
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.check
import emberr.shared.generated.resources.palette
import emberr.shared.generated.resources.textalign_center2
import emberr.shared.generated.resources.textalign_left2
import emberr.shared.generated.resources.textalign_right2
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import emberr.shared.generated.resources.database_align_center
import emberr.shared.generated.resources.database_align_left
import emberr.shared.generated.resources.database_align_right
import emberr.shared.generated.resources.database_alignment
import emberr.shared.generated.resources.database_default_color
import emberr.shared.generated.resources.database_style_cell
import emberr.shared.generated.resources.database_style_column
import emberr.shared.generated.resources.database_style_row
import emberr.shared.generated.resources.editor_table_background_color
import emberr.shared.generated.resources.editor_table_text_color
import org.jetbrains.compose.resources.stringResource

private val StyleSwatchSize = 28.dp

@Composable
internal fun databaseTextColorNamed(colorName: String?): Color? {
    val isDarkTheme = LocalAppIsDark.current
    return HighlightColor.entries.firstOrNull { it.storageName == colorName }?.backgroundFor(!isDarkTheme)
}

@Composable
internal fun databaseBackgroundColorNamed(colorName: String?): Color? {
    val isDarkTheme = LocalAppIsDark.current
    return HighlightColor.entries.firstOrNull { it.storageName == colorName }?.backgroundFor(isDarkTheme)
}

@Composable
internal fun DatabaseStyleOptions(
    block: DatabaseBlock,
    rowNoteId: String,
    column: DatabaseColumnTarget,
    editor: DatabaseBlockEditor
) {
    DatabaseStyleOption(label = stringResource(Res.string.database_style_cell), target = DatabaseStyleTarget.CELL, block = block, rowNoteId = rowNoteId, column = column, editor = editor)
    DatabaseStyleOption(label = stringResource(Res.string.database_style_row), target = DatabaseStyleTarget.ROW, block = block, rowNoteId = rowNoteId, column = column, editor = editor)
    DatabaseStyleOption(label = stringResource(Res.string.database_style_column), target = DatabaseStyleTarget.COLUMN, block = block, rowNoteId = rowNoteId, column = column, editor = editor)
}

@Composable
private fun DatabaseStyleOption(
    label: String,
    target: DatabaseStyleTarget,
    block: DatabaseBlock,
    rowNoteId: String,
    column: DatabaseColumnTarget,
    editor: DatabaseBlockEditor
) {
    DatabaseMenuLayer(
        title = label,
        anchor = { openLayer ->
            DatabaseMenuOption(label = label, icon = { DatabaseOptionIcon(Res.drawable.palette) }, onClick = openLayer)
        }
    ) { _ ->
        val style = block.styleOf(target, rowNoteId, column)
        fun choose(newStyle: DatabaseCellStyle) = editor.setStyle(block.id, target, rowNoteId, column, newStyle)

        DatabaseMenuSectionLabel(text = stringResource(Res.string.editor_table_text_color))
        DatabaseStyleColorChoices(
            selectedColorName = style.textColorName,
            colorOf = { databaseTextColorNamed(it.storageName) },
            onColorChosen = { choose(style.copy(textColorName = it)) }
        )

        DatabaseMenuSectionDivider()
        DatabaseMenuSectionLabel(text = stringResource(Res.string.editor_table_background_color))
        DatabaseStyleColorChoices(
            selectedColorName = style.backgroundColorName,
            colorOf = { databaseBackgroundColorNamed(it.storageName) },
            onColorChosen = { choose(style.copy(backgroundColorName = it)) }
        )

        DatabaseMenuSectionDivider()
        DatabaseMenuSectionLabel(text = stringResource(Res.string.database_alignment))
        DatabaseAlignmentOptions(selectedAlignment = style.alignment) { choose(style.copy(alignment = it)) }
    }
}

@Composable
internal fun DatabaseAlignmentOptions(selectedAlignment: TextAlignment?, onAlignmentChosen: (TextAlignment?) -> Unit) {
    DatabaseAlignmentOption(stringResource(Res.string.database_align_left), Res.drawable.textalign_left2, TextAlignment.LEFT, selectedAlignment, onAlignmentChosen)
    DatabaseAlignmentOption(stringResource(Res.string.database_align_center), Res.drawable.textalign_center2, TextAlignment.CENTER, selectedAlignment, onAlignmentChosen)
    DatabaseAlignmentOption(stringResource(Res.string.database_align_right), Res.drawable.textalign_right2, TextAlignment.RIGHT, selectedAlignment, onAlignmentChosen)
}

@Composable
private fun DatabaseAlignmentOption(
    label: String,
    icon: DrawableResource,
    alignment: TextAlignment,
    selectedAlignment: TextAlignment?,
    onAlignmentChosen: (TextAlignment?) -> Unit
) {
    val isSelected = selectedAlignment == alignment
    DatabaseMenuOption(
        label = label,
        isSelected = isSelected,
        icon = { DatabaseOptionIcon(icon) },
        onClick = { onAlignmentChosen(if (isSelected) null else alignment) }
    )
}

@Composable
internal fun DatabaseStyleColorChoices(
    selectedColorName: String?,
    colorOf: @Composable (HighlightColor) -> Color?,
    onColorChosen: (String?) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = DatabaseMenuTextInset, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DatabaseStyleSwatch(
            color = Color.Transparent,
            label = stringResource(Res.string.database_default_color),
            isSelected = selectedColorName == null,
            showsNoColorMark = true,
            onClick = { onColorChosen(null) }
        )
        HighlightColor.entries.forEach { highlightColor ->
            DatabaseStyleSwatch(
                color = colorOf(highlightColor) ?: Color.Transparent,
                label = highlightColor.displayName,
                isSelected = selectedColorName == highlightColor.storageName,
                showsNoColorMark = false,
                onClick = { onColorChosen(highlightColor.storageName) }
            )
        }
    }
}

@Composable
private fun DatabaseStyleSwatch(
    color: Color,
    label: String,
    isSelected: Boolean,
    showsNoColorMark: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(StyleSwatchSize)
            .clip(CircleShape)
            .background(color)
            .then(if (showsNoColorMark && !isSelected) Modifier.removeHighlightMark() else Modifier)
            .border(
                width = if (isSelected) 2.dp else 0.6.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                painter = painterResource(Res.drawable.check),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
