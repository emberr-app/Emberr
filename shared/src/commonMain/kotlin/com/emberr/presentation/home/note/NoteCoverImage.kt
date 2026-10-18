package com.emberr.presentation.home.note

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.emberr.domain.cover.DeviceCoverPositionStore
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.customEmberrShadow
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.cover.coverAlignmentFor
import com.emberr.presentation.shared.cover.rememberSavedCoverPosition
import com.emberr.presentation.shared.editor.blockViews.media.AnimatedGifLayer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import org.koin.compose.koinInject
import java.io.File

@Composable
internal fun NoteCoverImage(
    noteId: String,
    coverImagePath: String,
    isEditorFocused: Boolean,
    actionsEndPadding: Dp,
    modifier: Modifier = Modifier
) {
    val mediaStorageHelper = koinInject<MediaStorageHelper>()
    val coverPositionStore = koinInject<DeviceCoverPositionStore>()
    val absolutePath = remember(coverImagePath) { mediaStorageHelper.getAbsoluteMediaPath(coverImagePath) }
    val context = LocalPlatformContext.current
    val request = remember(absolutePath) {
        ImageRequest.Builder(context)
            .data(File(absolutePath))
            .memoryCacheKey(absolutePath)
            .diskCacheKey(absolutePath)
            .build()
    }

    val savedPosition by rememberSavedCoverPosition(noteId, coverImagePath)
    var draftPosition by remember(noteId, coverImagePath) { mutableStateOf<Float?>(null) }
    var imageSize by remember(absolutePath) { mutableStateOf(IntSize.Zero) }
    var coverSize by remember { mutableStateOf(IntSize.Zero) }
    var wasTapped by remember { mutableStateOf(false) }
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    val coverHazeState = remember { HazeState() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(isEditorFocused) {
        if (isEditorFocused) wasTapped = false
    }

    val isRepositioning = draftPosition != null
    val hiddenHeight = hiddenCoverHeight(imageSize, coverSize)
    val canReposition = hiddenHeight > 1f
    val showsActions = isRepositioning || (canReposition && (if (isDesktopPlatform) isHovered else wasTapped))
    val alignment = coverAlignmentFor(draftPosition ?: savedPosition)

    fun closeActions() {
        draftPosition = null
        wasTapped = false
    }

    Box(
        modifier = modifier
            .onSizeChanged { coverSize = it }
            .hoverable(hoverSource)
            .then(if (isRepositioning) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
            .pointerInput(isRepositioning, hiddenHeight) {
                if (isRepositioning) {
                    detectVerticalDragGestures { change, dragAmount ->
                        change.consume()
                        val currentPosition = draftPosition ?: savedPosition
                        draftPosition = (currentPosition - dragAmount / hiddenHeight).coerceIn(0f, 1f)
                    }
                } else if (!isDesktopPlatform) {
                    detectTapGestures {
                        wasTapped = !wasTapped
                        if (wasTapped) focusManager.clearFocus()
                    }
                }
            }
    ) {
        Box(modifier = Modifier.fillMaxSize().hazeSource(state = coverHazeState)) {
            AsyncImage(
                model = request,
                contentDescription = "Cover Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alignment = alignment,
                onSuccess = { successState ->
                    val loadedImage = successState.result.image
                    imageSize = IntSize(loadedImage.width, loadedImage.height)
                }
            )
            AnimatedGifLayer(absolutePath, ContentScale.Crop, Modifier.fillMaxSize(), alignment)
        }

        if (isRepositioning) {
            CoverPill(
                text = "Drag image to reposition",
                hazeState = coverHazeState,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (showsActions) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = actionsEndPadding, bottom = 12.dp)
            ) {
                if (isRepositioning) {
                    CoverPill(text = "Save position", hazeState = coverHazeState, onClick = {
                        draftPosition?.let { position -> coverPositionStore.savePosition(noteId, coverImagePath, position) }
                        closeActions()
                    })
                    CoverPill(text = "Cancel", hazeState = coverHazeState, onClick = ::closeActions)
                } else {
                    CoverPill(text = "Reposition", hazeState = coverHazeState, onClick = {
                        draftPosition = savedPosition
                        wasTapped = false
                    })
                }
            }
        }
    }
}

@Composable
private fun CoverPill(
    text: String,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
            .emberrBorder(CircleShape)
            .customEmberrShadow(CircleShape)
            .clip(CircleShape)
            .emberrBlur(hazeState, EmberrBlur.Regular)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

private fun hiddenCoverHeight(imageSize: IntSize, coverSize: IntSize): Float {
    if (imageSize.width <= 0 || imageSize.height <= 0 || coverSize.width <= 0 || coverSize.height <= 0) return 0f
    val cropScale = maxOf(
        coverSize.width.toFloat() / imageSize.width,
        coverSize.height.toFloat() / imageSize.height
    )
    return imageSize.height * cropScale - coverSize.height
}
