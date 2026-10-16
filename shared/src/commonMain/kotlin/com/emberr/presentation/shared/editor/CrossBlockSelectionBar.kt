package com.emberr.presentation.shared.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.components.customEmberrShadow
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.format_bold
import emberr.shared.generated.resources.highlight
import emberr.shared.generated.resources.italic
import emberr.shared.generated.resources.scissor2
import emberr.shared.generated.resources.text_x
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.underline
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private val SelectionBarShape = RoundedCornerShape(12.dp)
private val SelectionBarGapFromText = 8.dp
private val SelectionBarSideGutter = 16.dp

@Composable
fun CrossBlockSelectionBar(
    selection: CrossBlockSelectionState,
    listState: LazyListState,
    topInsetPx: Float,
    editorTopLeftInWindow: Offset,
    actions: TextSelectionMenuActions
) {
    val isShown by remember(selection) { derivedStateOf { selection.isActive && !selection.isBeingTouched } }
    var isChoosingHighlightColor by remember { mutableStateOf(false) }
    LaunchedEffect(isShown) { if (!isShown) isChoosingHighlightColor = false }

    Layout(
        content = {
            if (isShown) {
                SelectionBarContent(
                    actions = actions,
                    isChoosingHighlightColor = isChoosingHighlightColor,
                    onChoosingHighlightColorChange = { isChoosingHighlightColor = it }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { measurables, constraints ->
        val sideGutter = SelectionBarSideGutter.roundToPx()
        val bar = measurables.firstOrNull()?.measure(
            constraints.copy(minWidth = 0, minHeight = 0, maxWidth = (constraints.maxWidth - 2 * sideGutter).coerceAtLeast(0))
        )
        layout(constraints.maxWidth, constraints.maxHeight) {
            if (bar == null) return@layout
            val gap = SelectionBarGapFromText.toPx()
            val visibleTop = topInsetPx + gap
            val lowestTop = constraints.maxHeight - gap - bar.height
            val startRect = selection.cursorRectOf(SelectionEdge.Start)?.translate(-editorTopLeftInWindow)
            val endRect = selection.cursorRectOf(SelectionEdge.End)?.translate(-editorTopLeftInWindow)
            val aboveStart = startRect?.let { it.top - gap - bar.height }?.takeIf { it in visibleTop..lowestTop }
            val belowEndHandle = endRect?.let { it.bottom + 2 * SelectionHandleRadius.toPx() + gap }?.takeIf { it in visibleTop..lowestTop }
            val idsInSelection = selection.blockIdsInSelection().toSet()
            val isSelectionOnScreen = listState.layoutInfo.visibleItemsInfo.any { it.key in idsInSelection }
            val top = aboveStart ?: belowEndHandle ?: visibleTop.takeIf { isSelectionOnScreen } ?: return@layout
            bar.place(IntOffset((constraints.maxWidth - bar.width) / 2, top.roundToInt()))
        }
    }
}

@Composable
private fun SelectionBarContent(
    actions: TextSelectionMenuActions,
    isChoosingHighlightColor: Boolean,
    onChoosingHighlightColorChange: (Boolean) -> Unit
) {
    val tint = MaterialTheme.colorScheme.primary
    val toggleFormat = actions.toggleFormat
    Box(
        modifier = Modifier
            .customEmberrShadow(SelectionBarShape)
            .clip(SelectionBarShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), SelectionBarShape)
            .pointerInput(Unit) { detectTapGestures { } }
    ) {
        if (isChoosingHighlightColor && toggleFormat != null) {
            Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                HighlightColorCircles(
                    onColorChosen = { formatWithColor ->
                        onChoosingHighlightColorChange(false)
                        toggleFormat(formatWithColor)
                    },
                    onClose = { onChoosingHighlightColorChange(false) }
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions.copy?.let { SelectionBarButton(Res.drawable.copy, "Copy", 18.dp, it) }
                actions.cut?.let { SelectionBarButton(Res.drawable.scissor2, "Cut", 18.dp, it) }
                actions.paste?.let { paste ->
                    SelectionBarButtonFrame(onClick = paste) {
                        Icon(Icons.Default.ContentPaste, "Paste", modifier = Modifier.size(19.dp), tint = tint)
                    }
                }
                actions.delete?.let { SelectionBarButton(Res.drawable.trash, "Delete", 18.dp, it) }
                actions.selectAll?.let { selectAll ->
                    SelectionBarButtonFrame(onClick = selectAll) {
                        Icon(Icons.Default.SelectAll, "Select All", modifier = Modifier.size(20.dp), tint = tint)
                    }
                }
                if (toggleFormat != null) {
                    Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f)))
                    SelectionBarButton(Res.drawable.format_bold, "Bold", 14.dp) { toggleFormat("bold") }
                    SelectionBarButton(Res.drawable.italic, "Italic", 14.dp) { toggleFormat("italic") }
                    SelectionBarButton(Res.drawable.text_x, "Strikethrough", 16.dp) { toggleFormat("strike") }
                    SelectionBarButton(Res.drawable.underline, "Underline", 16.dp) { toggleFormat("underline") }
                    SelectionBarButton(Res.drawable.highlight, "Highlight", 16.dp) { onChoosingHighlightColorChange(true) }
                }
            }
        }
    }
}

@Composable
private fun SelectionBarButton(icon: DrawableResource, label: String, iconSize: Dp, onClick: () -> Unit) {
    SelectionBarButtonFrame(onClick = onClick) {
        Icon(painterResource(icon), label, modifier = Modifier.size(iconSize), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SelectionBarButtonFrame(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
