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
import emberr.shared.generated.resources.editor_align_justify
import emberr.shared.generated.resources.editor_block_callout
import emberr.shared.generated.resources.editor_block_checkbox
import emberr.shared.generated.resources.editor_block_code
import emberr.shared.generated.resources.editor_block_quote
import emberr.shared.generated.resources.editor_block_text
import emberr.shared.generated.resources.editor_style_bar_align_center
import emberr.shared.generated.resources.editor_style_bar_align_left
import emberr.shared.generated.resources.editor_style_bar_align_right
import emberr.shared.generated.resources.editor_style_bar_bulleted_list
import emberr.shared.generated.resources.editor_style_bar_decrease_indent
import emberr.shared.generated.resources.editor_style_bar_increase_indent
import emberr.shared.generated.resources.editor_style_bar_numbered_list
import emberr.shared.generated.resources.editor_style_bar_toggle_list
import org.jetbrains.compose.resources.stringResource

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
                        ToolbarButton(tooltip = stringResource(Res.string.editor_block_text), onClick = { onChangeBlockType("text") }) {
                            Icon(Icons.AutoMirrored.Filled.Subject, stringResource(Res.string.editor_block_text), tint = tint, modifier = Modifier.size(iconSize))
                        }
                        ToolbarLabel("H1", tint) { onChangeBlockType("h1") }
                        ToolbarLabel("H2", tint) { onChangeBlockType("h2") }
                        ToolbarButton(tooltip = stringResource(Res.string.editor_block_checkbox), onClick = { onChangeBlockType("checkbox") }) {
                            Icon(painterResource(Res.drawable.check_square), stringResource(Res.string.editor_block_checkbox), tint = tint, modifier = Modifier.size(customIconSize - 2.dp))
                        }
                        ToolbarButton(tooltip = stringResource(Res.string.editor_style_bar_bulleted_list), onClick = { onChangeBlockType("bullet") }) {
                            Icon(painterResource(Res.drawable.unordered_list), stringResource(Res.string.editor_style_bar_bulleted_list), tint = tint, modifier = Modifier.size(customIconSize - 2.dp))
                        }
                        ToolbarButton(tooltip = stringResource(Res.string.editor_style_bar_numbered_list), onClick = { onChangeBlockType("number") }) {
                            Icon(painterResource(Res.drawable.ordered_list), stringResource(Res.string.editor_style_bar_numbered_list), tint = tint, modifier = Modifier.size(customIconSize - 3.dp))
                        }
                        ToolbarButton(tooltip = stringResource(Res.string.editor_style_bar_toggle_list), onClick = { onChangeBlockType("toggle") }) {
                            Icon(painterResource(Res.drawable.arrow_right2), stringResource(Res.string.editor_style_bar_toggle_list), tint = tint, modifier = Modifier.size(customIconSize))
                        }
                        ToolbarButton(tooltip = stringResource(Res.string.editor_block_quote), onClick = { onChangeBlockType("quote") }) {
                            Icon(painterResource(Res.drawable.quote_down2), stringResource(Res.string.editor_block_quote), tint = tint, modifier = Modifier.size(customIconSize - 3.dp))
                        }
                        ToolbarButton(tooltip = stringResource(Res.string.editor_block_callout), onClick = { onChangeBlockType("callout") }) {
                            Icon(painterResource(Res.drawable.card_edit2), stringResource(Res.string.editor_block_callout), tint = tint, modifier = Modifier.size(customIconSize - 2.dp))
                        }
                        ToolbarButton(tooltip = stringResource(Res.string.editor_block_code), onClick = { onChangeBlockType("code") }) {
                            Icon(painterResource(Res.drawable.code), stringResource(Res.string.editor_block_code), tint = tint, modifier = Modifier.size(customIconSize))
                        }

                        ToolbarDivider(tint)

                        InlineFormatButtons(
                            tint = tint,
                            iconSize = customIconSize,
                            onToggleFormat = onToggleFormat,
                            onHighlightClick = { isChoosingHighlightColor = !isChoosingHighlightColor }
                        )

                        ToolbarDivider(tint)

                        ToolbarButton(tooltip = stringResource(Res.string.editor_style_bar_align_left), onClick = { onSetAlignment(TextAlignment.LEFT) }) {
                            Icon(painterResource(Res.drawable.textalign_left2), stringResource(Res.string.editor_style_bar_align_left), tint = tint, modifier = Modifier.size(customIconSize))
                        }
                        ToolbarButton(tooltip = stringResource(Res.string.editor_style_bar_align_right), onClick = { onSetAlignment(TextAlignment.RIGHT) }) {
                            Icon(painterResource(Res.drawable.textalign_right2), stringResource(Res.string.editor_style_bar_align_right), tint = tint, modifier = Modifier.size(customIconSize))
                        }
                        ToolbarButton(tooltip = stringResource(Res.string.editor_style_bar_align_center), onClick = { onSetAlignment(TextAlignment.CENTER) }) {
                            Icon(painterResource(Res.drawable.textalign_center2), stringResource(Res.string.editor_style_bar_align_center), tint = tint, modifier = Modifier.size(customIconSize))
                        }
                        ToolbarButton(tooltip = stringResource(Res.string.editor_align_justify), onClick = { onSetAlignment(TextAlignment.JUSTIFY) }) {
                            Icon(painterResource(Res.drawable.textalign_justifycenter2), stringResource(Res.string.editor_align_justify), tint = tint, modifier = Modifier.size(customIconSize))
                        }

                        ToolbarDivider(tint)

                        ToolbarButton(tooltip = stringResource(Res.string.editor_style_bar_decrease_indent), onClick = { onAdjustIndentation(false) }) {
                            Icon(painterResource(Res.drawable.indent_right), stringResource(Res.string.editor_style_bar_decrease_indent), tint = tint, modifier = Modifier.size(customIconSize))
                        }
                        ToolbarButton(tooltip = stringResource(Res.string.editor_style_bar_increase_indent), onClick = { onAdjustIndentation(true) }) {
                            Icon(painterResource(Res.drawable.indent_left), stringResource(Res.string.editor_style_bar_increase_indent), tint = tint, modifier = Modifier.size(customIconSize))
                        }
                    }
                }
            }
        }
    }
}
