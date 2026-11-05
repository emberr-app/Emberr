package com.emberr.presentation.shared.editor.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.components.EmberrShadowElevation
import com.emberr.presentation.shared.components.EmberrShadowSpotColorSoft
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.IconTooltipPosition
import com.emberr.presentation.shared.editor.formatting.removeHighlightMark
import com.emberr.ui.theme.HighlightColor
import com.emberr.ui.theme.LocalAppIsDark
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.format_bold
import emberr.shared.generated.resources.italic
import emberr.shared.generated.resources.text_highlight
import emberr.shared.generated.resources.text_x
import emberr.shared.generated.resources.underline
import org.jetbrains.compose.resources.painterResource

internal val DefaultCornerShape = RoundedCornerShape(12.dp)

@Composable
internal fun InlineFormatButtons(
    tint: Color,
    iconSize: Dp,
    onToggleFormat: (String) -> Unit,
    onHighlightClick: () -> Unit,
    enabled: Boolean = true
) {
    ToolbarButton(enabled = enabled, tooltip = "Bold", onClick = { onToggleFormat("bold") }) {
        Icon(painterResource(Res.drawable.format_bold), "Bold", tint = tint, modifier = Modifier.size(iconSize - 4.dp))
    }
    ToolbarButton(enabled = enabled, tooltip = "Italic", onClick = { onToggleFormat("italic") }) {
        Icon(painterResource(Res.drawable.italic), "Italic", tint = tint, modifier = Modifier.size(iconSize - 4.dp))
    }
    ToolbarButton(enabled = enabled, tooltip = "Strikethrough", onClick = { onToggleFormat("strike") }) {
        Icon(painterResource(Res.drawable.text_x), "Strikethrough", tint = tint, modifier = Modifier.size(iconSize - 2.dp))
    }
    ToolbarButton(enabled = enabled, tooltip = "Underline", onClick = { onToggleFormat("underline") }) {
        Icon(painterResource(Res.drawable.underline), "Underline", tint = tint, modifier = Modifier.size(iconSize - 2.dp))
    }
    ToolbarButton(enabled = enabled, tooltip = "Highlight", onClick = onHighlightClick) {
        Icon(painterResource(Res.drawable.text_highlight), "Highlight", tint = tint, modifier = Modifier.size(iconSize + 1.dp))
    }
}

@Composable
internal fun FloatingHighlightColors(onColorChosen: (String) -> Unit, onClose: () -> Unit) {
    val isDarkTheme = LocalAppIsDark.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FloatingSwatch(
            fillColor = MaterialTheme.colorScheme.background,
            onClick = onClose
        ) {
            Icon(
                Icons.Default.Close,
                "Close colours",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(FloatingSwatchSize / 2)
            )
        }

        FloatingSwatch(
            fillColor = Color.Transparent,
            showsRemoveMark = true,
            onClick = { onColorChosen("highlight:none") }
        )

        HighlightColor.entries.forEach { highlightColor ->
            FloatingSwatch(
                fillColor = highlightColor.backgroundFor(isDarkTheme),
                onClick = { onColorChosen("highlight:${highlightColor.storageName}") }
            )
        }
    }
}

@Composable
private fun FloatingSwatch(
    fillColor: Color,
    onClick: () -> Unit,
    showsRemoveMark: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .size(FloatingSwatchSize)
            .shadow(elevation = EmberrShadowElevation.Standard, shape = CircleShape, spotColor = EmberrShadowSpotColorSoft)
            .clip(CircleShape)
            .background(fillColor)
            .then(if (showsRemoveMark) Modifier.removeHighlightMark() else Modifier)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private val ToolbarButtonSize = 34.dp
private val FloatingSwatchSize = 30.dp

@Composable
internal fun ToolbarButton(
    enabled: Boolean = true,
    tooltip: String? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    IconTooltip(text = tooltip, position = IconTooltipPosition.Above) {
        Box(
            modifier = Modifier
                .size(ToolbarButtonSize)
                .clip(CircleShape)
                .clickable(enabled = enabled) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
internal fun ToolbarLabel(label: String, tint: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(ToolbarButtonSize)
            .defaultMinSize(minWidth = ToolbarButtonSize)
            .clip(CircleShape)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
internal fun ToolbarDivider(tint: Color) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .width(1.dp)
            .height(18.dp)
            .background(tint.copy(alpha = 0.2f))
    )
}
