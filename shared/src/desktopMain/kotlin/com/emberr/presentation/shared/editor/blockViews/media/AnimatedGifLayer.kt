package com.emberr.presentation.shared.editor.blockViews.media

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

private const val SHORTEST_FRAME_MILLIS = 20
private const val NO_PRIOR_FRAME = -1

@Composable
actual fun AnimatedGifLayer(absolutePath: String, contentScale: ContentScale, modifier: Modifier, alignment: Alignment) {
    if (!absolutePath.endsWith(".gif", ignoreCase = true)) return

    val gifCodec by produceState<Codec?>(initialValue = null, absolutePath) {
        value = withContext(Dispatchers.IO) { openAnimatedGifOrNull(absolutePath) }
    }
    val codec = gifCodec ?: return
    val frameBitmap = remember(codec) { Bitmap().apply { allocPixels(codec.imageInfo) } }
    var shownFrame by remember(codec) { mutableStateOf<ImageBitmap?>(null) }

    DisposableEffect(codec) {
        onDispose { codec.close() }
    }

    LaunchedEffect(codec) {
        val frameDurations = codec.framesInfo.map { it.duration.coerceAtLeast(SHORTEST_FRAME_MILLIS) }
        while (true) {
            for (frame in 0 until codec.frameCount) {
                val priorFrame = if (frame == 0) NO_PRIOR_FRAME else frame - 1
                val wasDecoded = runCatching { codec.readPixels(frameBitmap, frame, priorFrame) }.isSuccess ||
                    runCatching { codec.readPixels(frameBitmap, frame) }.isSuccess
                if (!wasDecoded) return@LaunchedEffect
                shownFrame = frameBitmap.asComposeImageBitmap()
                delay(frameDurations[frame].milliseconds)
            }
        }
    }

    shownFrame?.let { frame ->
        Image(bitmap = frame, contentDescription = null, contentScale = contentScale, alignment = alignment, modifier = modifier)
    }
}

private fun openAnimatedGifOrNull(absolutePath: String): Codec? =
    runCatching { Codec.makeFromData(Data.makeFromBytes(File(absolutePath).readBytes())) }
        .getOrNull()
        ?.let { codec ->
            if (codec.frameCount > 1) codec else {
                codec.close()
                null
            }
        }
