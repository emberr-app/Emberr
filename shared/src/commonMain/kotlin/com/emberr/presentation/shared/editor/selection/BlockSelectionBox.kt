package com.emberr.presentation.shared.editor.selection

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

private val SelectionBoxBorderColor = Color(0xFF4F5B8A)

internal class BlockSelectionBoxDrag(
    private val selection: CrossBlockSelectionState,
    private val selectBlocks: (Set<String>) -> Unit
) {
    private var anchor: BlockSelectionBoxAnchor? = null
    private var anchorX = 0f
    private var pointerInList = Offset.Zero
    private var blockIdsSelectedBefore = emptySet<String>()

    fun startAt(pressInList: Offset, blockIdsSelectedBefore: Set<String>) {
        anchor = selection.windowPointOf(pressInList)?.let { selection.selectionBoxAnchorAt(it) }
        anchorX = pressInList.x
        pointerInList = pressInList
        this.blockIdsSelectedBefore = blockIdsSelectedBefore
    }

    fun moveTo(pointerInList: Offset) {
        this.pointerInList = pointerInList
        update()
    }

    fun update() {
        val anchor = anchor ?: return
        val anchorY = selection.selectionBoxAnchorYInList(anchor) ?: return
        selection.blockSelectionBoxInList = Rect(
            left = min(anchorX, pointerInList.x),
            top = min(anchorY, pointerInList.y),
            right = max(anchorX, pointerInList.x),
            bottom = max(anchorY, pointerInList.y)
        )
        val pointerBlockId = selection.windowPointOf(pointerInList)?.let { selection.blockIdUnderSelectionBoxCorner(it) }
        selectBlocks(blockIdsSelectedBefore + selection.blockIdsTouchedBySelectionBox(anchor.blockId, pointerBlockId))
    }

    fun end() {
        selection.blockSelectionBoxInList = null
    }
}

fun Modifier.drawBlockSelectionBox(selection: CrossBlockSelectionState, color: Color): Modifier = drawWithContent {
    drawContent()
    val box = selection.blockSelectionBoxInList?.intersect(Rect(Offset.Zero, size)) ?: return@drawWithContent
    if (box.width <= 0f || box.height <= 0f) return@drawWithContent
    drawRect(color.copy(alpha = 0.12f), topLeft = box.topLeft, size = box.size)
    drawRect(SelectionBoxBorderColor, topLeft = box.topLeft, size = box.size, style = Stroke(width = 1.dp.toPx()))
}
