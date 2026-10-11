package com.emberr.domain.util.media

import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.compose.ui.unit.IntSize
import java.io.IOException

private val sidewaysExifOrientations = setOf(
    ExifInterface.ORIENTATION_ROTATE_90,
    ExifInterface.ORIENTATION_ROTATE_270,
    ExifInterface.ORIENTATION_TRANSPOSE,
    ExifInterface.ORIENTATION_TRANSVERSE
)

actual fun readImagePixelSize(absolutePath: String): IntSize? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(absolutePath, options)
    if (options.outWidth <= 0 || options.outHeight <= 0) return null
    return if (isStoredSideways(absolutePath)) IntSize(options.outHeight, options.outWidth) else IntSize(options.outWidth, options.outHeight)
}

private fun isStoredSideways(absolutePath: String): Boolean =
    try {
        ExifInterface(absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) in sidewaysExifOrientations
    } catch (_: IOException) {
        false
    }
