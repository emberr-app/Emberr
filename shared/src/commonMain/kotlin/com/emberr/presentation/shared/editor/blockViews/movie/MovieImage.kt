package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.emberr.domain.util.media.MediaStorageHelper
import okio.Path.Companion.toPath
import org.koin.compose.koinInject

@Composable
internal fun MovieImage(
    filePath: String?,
    url: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Crop,
    onImageLoaded: (AsyncImagePainter.State.Success) -> Unit = {}
) {
    val mediaStorageHelper = koinInject<MediaStorageHelper>()
    val localPath = remember(filePath) { filePath?.let { mediaStorageHelper.getAbsoluteMediaPath(it).toPath() } }
    var localFileFailed by remember(filePath) { mutableStateOf(false) }
    val usesLocalFile = localPath != null && !localFileFailed
    AsyncImage(
        model = if (usesLocalFile) localPath else url,
        contentDescription = null,
        contentScale = contentScale,
        alignment = alignment,
        modifier = modifier,
        onSuccess = onImageLoaded,
        onError = { if (usesLocalFile) localFileFailed = true }
    )
}
