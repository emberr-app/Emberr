package com.emberr.presentation.shared.editor

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emberr.domain.model.BulletedListBlock
import com.emberr.domain.model.CalloutBlock
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.CodeBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NumberedListBlock
import com.emberr.domain.model.ToggleBlock
import com.emberr.domain.util.system.triggerHapticFeedback
import com.emberr.presentation.shared.components.EmberrCheckbox
import com.emberr.presentation.shared.editor.blockViews.callout.CalloutOptionsMenu
import com.emberr.presentation.shared.editor.blockViews.callout.accentColor
import com.emberr.presentation.shared.editor.blockViews.callout.icon
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun BlockLeadingMarker(
    block: NoteBlock,
    textStyle: TextStyle,
    actions: EditorActions,
    afterKeyboardCloses: (action: () -> Unit) -> Unit
) {
    var showCalloutOptions by remember { mutableStateOf(false) }
    val iconOffset = when (block) {
        is HeadingBlock -> if (block.level == 1) 4.dp else 2.dp
        is CodeBlock -> 12.dp
        else -> (-2).dp
    }

    if (block is CheckboxBlock || block is BulletedListBlock || block is NumberedListBlock || block is ToggleBlock || block is CalloutBlock) {
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .offset(y = iconOffset)
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (block) {
                is CheckboxBlock -> EmberrCheckbox(
                    checked = block.isChecked,
                    onCheckedChange = {
                        triggerHapticFeedback()
                        actions.onToggleCheckbox(block.id, it)
                    }
                )
                is BulletedListBlock -> Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(textStyle.color))
                is NumberedListBlock -> Text("${block.number}.", style = textStyle.copy(fontSize = 17.sp))
                is ToggleBlock -> {
                    val rotation by animateFloatAsState(if (block.isExpanded) 90f else 0f, label = "toggleRotation")
                    Icon(
                        Icons.Default.ChevronRight, null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(32.dp).rotate(rotation).clickable { actions.onToggleExpand(block.id) }
                    )
                }
                is CalloutBlock -> {
                    Icon(
                        painterResource(block.calloutType.icon()), "Callout options",
                        tint = block.calloutType.accentColor(),
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { afterKeyboardCloses { showCalloutOptions = true } }
                            .padding(2.dp)
                    )
                    CalloutOptionsMenu(
                        expanded = showCalloutOptions,
                        block = block,
                        onDismiss = { showCalloutOptions = false },
                        onUpdateStyle = { calloutType, isFoldable -> actions.onUpdateCalloutStyle(block.id, calloutType, isFoldable) }
                    )
                }
            }
        }
    }
}
