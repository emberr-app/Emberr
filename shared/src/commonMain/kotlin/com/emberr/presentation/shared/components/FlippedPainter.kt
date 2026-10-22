package com.emberr.presentation.shared.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.painter.Painter

fun Painter.flippedHorizontally(): Painter = object : Painter() {
    override val intrinsicSize: Size = this@flippedHorizontally.intrinsicSize
    override fun DrawScope.onDraw() {
        scale(scaleX = -1f, scaleY = 1f) {
            with(this@flippedHorizontally) { draw(size) }
        }
    }
}
