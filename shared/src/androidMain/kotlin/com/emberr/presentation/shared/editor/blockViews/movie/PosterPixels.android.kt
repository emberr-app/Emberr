package com.emberr.presentation.shared.editor.blockViews.movie

import android.graphics.Bitmap
import coil3.BitmapImage
import coil3.compose.AsyncImagePainter

internal actual fun samplePosterPixels(loadedImage: AsyncImagePainter.State.Success, width: Int, height: Int): IntArray? {
    val bitmap = (loadedImage.result.image as? BitmapImage)?.bitmap ?: return null
    val readableBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return null
    } else {
        bitmap
    }
    val sample = Bitmap.createScaledBitmap(readableBitmap, width, height, true)
    return IntArray(width * height).also { sample.getPixels(it, 0, width, 0, 0, width, height) }
}
