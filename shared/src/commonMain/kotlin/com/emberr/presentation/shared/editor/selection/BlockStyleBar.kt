package com.emberr.presentation.shared.editor.selection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.TextAlignment
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.customEmberrShadow
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.emberrBorder
import dev.chrisbanes.haze.HazeState
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.arrow_right2
import emberr.shared.generated.resources.card_edit2
import emberr.shared.generated.resources.check_square
import emberr.shared.generated.resources.code
import emberr.shared.generated.resources.indent_left
import emberr.shared.generated.resources.indent_right
import emberr.shared.generated.resources.ordered_list
import emberr.shared.generated.resources.quote_down2
import emberr.shared.generated.resources.textalign_center2
import emberr.shared.generated.resources.textalign_justifycenter2
import emberr.shared.generated.resources.textalign_left2
import emberr.shared.generated.resources.textalign_right2
import emberr.shared.generated.resources.unordered_list
import org.jetbrains.compose.resources.painterResource
import com.emberr.presentation.shared.editor.toolbar.DefaultCornerShape
import com.emberr.presentation.shared.editor.toolbar.FloatingHighlightColors
import com.emberr.presentation.shared.editor.toolbar.InlineFormatButtons
import com.emberr.presentation.shared.editor.toolbar.ToolbarButton
import com.emberr.presentation.shared.editor.toolbar.ToolbarDivider
import com.emberr.presentation.shared.editor.toolbar.ToolbarLabel

@Composable
fun BlockStyleBar(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    onChangeBlockType: (String) -> Unit,
    onToggleFormat: (String) -> Unit,
    onAdjustIndentation: (Boolean) -> Unit,
    onSetAlignment: (TextAlignment) -> Unit,
    hazeState: HazeState
) {
    val tint = MaterialTheme.colorScheme.primary
    val iconSize = 19.dp
    val customIconSize = 18.dp
    var isChoosingHighlightColor by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.padding(horizontal = 24.dp)
    ) {
        Column {
            if (isChoosingHighlightColor) {
                FloatingHighlightColors(
                    onColorChosen = onToggleFormat,
                    onClose = { isChoosingHighlightColor = false }
                )
            }

            Surface(
                shape = DefaultCornerShape,
                color = Color.Transparent,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .emberrBorder(DefaultCornerShape)
                    .customEmberrShadow(DefaultCornerShape)
                    .clip(DefaultCornerShape)
                    .emberrBlur(hazeState, EmberrBlur.Regular)
            ) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 36.dp) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        ToolbarButton(tooltip = "Text", onClick = { onChangeBlockType("text") }) {
                            Icon(Icons.AutoMirrored.Filled.Subject, "Text", tint = tint, modifier = Modifier.size(iconSize))
                        }
                        ToolbarLabel("H1", tint) { onChangeBlockType("h1") }
                        ToolbarLabel("H2", tint) { onChangeBlockType("h2") }
                        ToolbarButton(tooltip = "Checkbox", onClick = { onChangeBlockType("checkbox") }) {
                            Icon(painterResource(Res.drawable.check_square), "Checkbox", tint = tint, modifier = Modifier.size(customIconSize - 2.dp))
                        }
                        ToolbarButton(tooltip = "Bulleted list", onClick = { onChangeBlockType("bullet") }) {
                            Icon(painterResource(Res.drawable.unordered_list), "Bulleted list", tint = tint, modifier = Modifier.size(customIconSize - 2.dp))
                        }
                        ToolbarButton(tooltip = "Numbered list", onClick = { onChangeBlockType("number") }) {
                            Icon(painterResource(Res.drawable.ordered_list), "Numbered list", tint = tint, modifier = Modifier.size(customIconSize - 3.dp))
                        }
                        ToolbarButton(tooltip = "Toggle list", onClick = { onChangeBlockType("toggle") }) {
                            Icon(painterResource(Res.drawable.arrow_right2), "Toggle list", tint = tint, modifier = Modifier.size(customIconSize))
                        }
                        ToolbarButton(tooltip = "Quote", onClick = { onChangeBlockType("quote") }) {
                            Icon(painterResource(Res.drawable.quote_down2), "Quote", tint = tint, modifier = Modifier.size(customIconSize - 3.dp))
                        }
                        ToolbarButton(tooltip = "Callout", onClick = { onChangeBlockType("callout") }) {
                            Icon(painterResource(Res.drawable.card_edit2), "Callout", tint = tint, modifier = Modifier.size(customIconSize - 2.dp))
                        }
                        ToolbarButton(tooltip = "Code", onClick = { onChangeBlockType("code") }) {
                            Icon(painterResource(Res.drawable.code), "Code", tint = tint, modifier = Modifier.size(customIconSize))
                        }

                        ToolbarDivider(tint)

                        InlineFormatButtons(
                            tint = tint,
                            iconSize = customIconSize,
                            onToggleFormat = onToggleFormat,
                            onHighlightClick = { isChoosingHighlightColor = !isChoosingHighlightColor }
                        )

                        ToolbarDivider(tint)

                        ToolbarButton(tooltip = "Align left", onClick = { onSetAlignment(TextAlignment.LEFT) }) {
                            Icon(painterResource(Res.drawable.textalign_left2), "Align left", tint = tint, modifier = Modifier.size(customIconSize))
                        }
                        ToolbarButton(tooltip = "Align right", onClick = { onSetAlignment(TextAlignment.RIGHT) }) {
                            Icon(painterResource(Res.drawable.textalign_right2), "Align right", tint = tint, modifier = Modifier.size(customIconSize))
                        }
                        ToolbarButton(tooltip = "Align center", onClick = { onSetAlignment(TextAlignment.CENTER) }) {
                            Icon(painterResource(Res.drawable.textalign_center2), "Align center", tint = tint, modifier = Modifier.size(customIconSize))
                        }
                        ToolbarButton(tooltip = "Justify", onClick = { onSetAlignment(TextAlignment.JUSTIFY) }) {
                            Icon(painterResource(Res.drawable.textalign_justifycenter2), "Justify", tint = tint, modifier = Modifier.size(customIconSize))
                        }

                        ToolbarDivider(tint)

                        ToolbarButton(tooltip = "Decrease indent", onClick = { onAdjustIndentation(false) }) {
                            Icon(painterResource(Res.drawable.indent_right), "Decrease indent", tint = tint, modifier = Modifier.size(customIconSize))
                        }
                        ToolbarButton(tooltip = "Increase indent", onClick = { onAdjustIndentation(true) }) {
                            Icon(painterResource(Res.drawable.indent_left), "Increase indent", tint = tint, modifier = Modifier.size(customIconSize))
                        }
                    }
                }
            }
        }
    }
}
