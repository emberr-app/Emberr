package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import coil3.compose.AsyncImagePainter

internal actual fun samplePosterPixels(loadedImage: AsyncImagePainter.State.Success, width: Int, height: Int): IntArray? {
    val sample = ImageBitmap(width, height)
    CanvasDrawScope().draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = Canvas(sample),
        size = Size(width.toFloat(), height.toFloat())
    ) {
        with(loadedImage.painter) { draw(size) }
    }
    return IntArray(width * height).also { sample.readPixels(it) }
}
