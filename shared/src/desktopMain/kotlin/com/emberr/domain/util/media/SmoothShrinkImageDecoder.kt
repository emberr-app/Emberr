package com.emberr.domain.util.media

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.util.component1
import coil3.util.component2
import okio.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.impl.use

private val BlendedShrinking = FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR)

class SmoothShrinkImageDecoder(
    private val source: ImageSource,
    private val options: Options
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        val image = Image.makeFromEncoded(bytes)
        try {
            val bitmap = shrunkBitmapOf(image)
            bitmap.setImmutable()
            return DecodeResult(
                image = bitmap.asImage(),
                isSampled = bitmap.width < image.width || bitmap.height < image.height
            )
        } finally {
            image.close()
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun shrunkBitmapOf(image: Image): Bitmap {
        val (targetWidth, targetHeight) = DecodeUtils.computeDstSize(
            srcWidth = image.width,
            srcHeight = image.height,
            targetSize = options.size,
            scale = options.scale,
            maxSize = options.maxBitmapSize
        )
        val sizeMultiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = image.width,
            srcHeight = image.height,
            dstWidth = targetWidth,
            dstHeight = targetHeight,
            scale = options.scale,
            maxSize = options.maxBitmapSize
        ).let { if (options.precision == Precision.INEXACT) it.coerceAtMost(1.0) else it }
        val outputWidth = (sizeMultiplier * image.width).toInt()
        val outputHeight = (sizeMultiplier * image.height).toInt()

        val bitmap = Bitmap()
        bitmap.allocN32Pixels(outputWidth, outputHeight)
        Canvas(bitmap).use { canvas ->
            canvas.drawImageRect(
                image = image,
                srcLeft = 0f,
                srcTop = 0f,
                srcRight = image.width.toFloat(),
                srcBottom = image.height.toFloat(),
                dstLeft = 0f,
                dstTop = 0f,
                dstRight = outputWidth.toFloat(),
                dstBottom = outputHeight.toFloat(),
                samplingMode = BlendedShrinking,
                paint = null,
                strict = false
            )
        }
        return bitmap
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder =
            SmoothShrinkImageDecoder(result.source, options)
    }
}
