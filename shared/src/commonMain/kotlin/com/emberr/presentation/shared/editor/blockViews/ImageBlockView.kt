package com.emberr.presentation.shared.editor.blockViews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.toSize
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.sync.MediaRetryCoordinator
import com.emberr.domain.util.media.ImageClipboard
import com.emberr.domain.util.media.ImageDownloader
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.domain.util.media.webImageAddressOrNull
import com.emberr.presentation.shared.editor.ImageLinkFetch
import com.emberr.presentation.shared.editor.rememberWebLinkActions
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.showFeedback
import com.emberr.presentation.LocalImageOverlay
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.editor.DefaultBlockShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.camera
import emberr.shared.generated.resources.circle_x
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.upload2
import emberr.shared.generated.resources.image
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.DrawableResource
import org.koin.compose.koinInject
import java.io.File

private val MaximumImageBlockHeight = 320.dp
private val LoadingImageBlockHeight = 180.dp
private val EmptyImageButtonSidePadding = 8.dp
private val EmptyImageButtonsEndSpace = 6.dp
private val SourceLinkPillMaxWidth = 200.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageBlockView(
    block: ImageBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onRequestPicker: () -> Unit,
    onDelete: () -> Unit = {},
    onRequestCamera: () -> Unit,
    imageLinkFetch: ImageLinkFetch?,
    onImageLinkSubmitted: (address: String) -> Unit,
    fillsAvailableSpace: Boolean = false
) {
    val mediaStorageHelper = koinInject<MediaStorageHelper>()
    val mediaRetryCoordinator = koinInject<MediaRetryCoordinator>()
    val imageDownloader = koinInject<ImageDownloader>()
    val coroutineScope = rememberCoroutineScope()
    val webLinkActions = rememberWebLinkActions()
    var showFullScreen by remember { mutableStateOf(false) }
    val setFullScreenOverlay = LocalImageOverlay.current

    if (block.localFilePath == null) {
        EmptyImageBlock(
            inSelectionMode = inSelectionMode,
            onToggleSelection = onToggleSelection,
            onRequestPicker = onRequestPicker,
            onRequestCamera = onRequestCamera,
            imageLinkFetch = imageLinkFetch,
            onImageLinkSubmitted = onImageLinkSubmitted
        )
    } else {
        val absolutePath = remember(block.localFilePath) {
            mediaStorageHelper.getAbsoluteMediaPath(block.localFilePath)
        }
        val fileName = remember(block.localFilePath) { block.localFilePath.substringAfterLast("/") }
        val availability = rememberMediaAvailability(absolutePath, fileName)

        if (availability != MediaAvailability.Available) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .heightIn(min = 100.dp, max = 160.dp)
                    .clip(DefaultBlockShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (inSelectionMode) onToggleSelection()
                            else if (availability == MediaAvailability.Failed) mediaRetryCoordinator.retryMediaDownload(fileName)
                        },
                        onLongClick = if (isDesktopPlatform) null else onToggleSelection
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (availability == MediaAvailability.Failed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(Res.drawable.circle_x),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Failed to download image - tap to retry",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Downloading image...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            val imageFile = remember(absolutePath) { File(absolutePath) }
            val context = LocalPlatformContext.current
            val request = remember(absolutePath, context) {
                ImageRequest.Builder(context)
                    .data(imageFile)
                    .memoryCacheKey("$absolutePath-${imageFile.lastModified()}")
                    .diskCacheKey("$absolutePath-${imageFile.lastModified()}")
                    .build()
            }

            val imageHazeState = remember { HazeState() }
            var widthToHeightRatio by remember(absolutePath) { mutableStateOf<Float?>(null) }
            var thumbnailBoundsInRoot by remember { mutableStateOf<Rect?>(null) }

            Box(
                modifier = if (fillsAvailableSpace) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                },
                contentAlignment = Alignment.CenterStart
            ) {
                val sizeModifier = if (fillsAvailableSpace) {
                    Modifier.fillMaxSize()
                } else {
                    widthToHeightRatio?.let { ratio ->
                        if (isDesktopPlatform) {
                            Modifier.heightIn(max = MaximumImageBlockHeight).aspectRatio(ratio)
                        } else {
                            Modifier.fillMaxWidth().aspectRatio(ratio)
                        }
                    } ?: Modifier.fillMaxWidth().height(LoadingImageBlockHeight)
                }

                Box(
                    modifier = sizeModifier
                        .onGloballyPositioned { coordinates ->
                            thumbnailBoundsInRoot =
                                Rect(coordinates.positionInRoot(), coordinates.size.toSize())
                        }
                        .graphicsLayer { alpha = if (showFullScreen) 0f else 1f }
                        .clip(DefaultBlockShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (inSelectionMode) onToggleSelection()
                                else showFullScreen = true
                            },
                            onLongClick = if (isDesktopPlatform) null else onToggleSelection
                        )
                ) {
                    AsyncImage(
                        model = request,
                        contentDescription = "Note Image",
                        modifier = Modifier.fillMaxSize().hazeSource(state = imageHazeState),
                        contentScale = ContentScale.Crop,
                        onSuccess = { successState ->
                            if (widthToHeightRatio == null) {
                                val loadedImage = successState.result.image
                                if (loadedImage.width > 0 && loadedImage.height > 0) {
                                    widthToHeightRatio = loadedImage.width.toFloat() / loadedImage.height.toFloat()
                                }
                            }
                        }
                    )

                    Icon(
                        painterResource(Res.drawable.copy),
                        contentDescription = "Copy Image",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .emberrBlur(imageHazeState, EmberrBlur.OnImage)
                            .padding(8.dp)
                            .size(16.dp)
                            .clickable {
                                coroutineScope.launch {
                                    val success = ImageClipboard.copyImageToClipboard(absolutePath)
                                    showFeedback(if (success) "Image copied to clipboard" else "Failed to copy image")
                                }
                            }
                    )

                    block.sourceUrl?.let { sourceUrl ->
                        Text(
                            text = sourceUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp)
                                .widthIn(max = SourceLinkPillMaxWidth)
                                .clip(CircleShape)
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .emberrBlur(imageHazeState, EmberrBlur.OnImage)
                                .clickable {
                                    if (inSelectionMode) onToggleSelection() else webLinkActions.openLink(sourceUrl)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            LaunchedEffect(showFullScreen, request) {
                if (showFullScreen) {
                    setFullScreenOverlay?.invoke {
                        com.emberr.presentation.shared.editor.components.FullScreenImageScreen(
                            request = request,
                            hasLocalFile = true,
                            thumbnailBoundsInRoot = thumbnailBoundsInRoot,
                            imageWidthToHeightRatio = widthToHeightRatio,
                            onBack = { showFullScreen = false },
                            onDownload = {
                                coroutineScope.launch {
                                    val success = imageDownloader.downloadImage(absolutePath, fileName)
                                    showFeedback(
                                        if (success) {
                                            if (isDesktopPlatform) "Image saved to Downloads" else "Image saved to Photos"
                                        } else {
                                            "Failed to save image"
                                        }
                                    )
                                }
                            },
                            onCopy = {
                                coroutineScope.launch {
                                    val success = ImageClipboard.copyImageToClipboard(absolutePath)
                                    showFeedback(if (success) "Image copied to clipboard" else "Failed to copy image")
                                }
                            },
                            onDelete = {
                                setFullScreenOverlay?.invoke(null)
                                showFullScreen = false
                                onDelete()
                            },
                            onOpenSourceLink = block.sourceUrl?.let { sourceUrl -> { webLinkActions.openLink(sourceUrl) } }
                        )
                    }
                } else {
                    setFullScreenOverlay?.invoke(null)
                }
            }
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmptyImageBlock(
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onRequestPicker: () -> Unit,
    onRequestCamera: () -> Unit,
    imageLinkFetch: ImageLinkFetch?,
    onImageLinkSubmitted: (address: String) -> Unit
) {
    var linkText by remember { mutableStateOf(imageLinkFetch?.address.orEmpty()) }
    var hasEditedLinkSinceLastTry by remember { mutableStateOf(false) }
    val isFetching = imageLinkFetch?.hasFailed == false
    val hasFetchFailed = imageLinkFetch?.hasFailed == true && !hasEditedLinkSinceLastTry
    val hintColor = MaterialTheme.colorScheme.outline

    fun fetchImage(address: String) {
        hasEditedLinkSinceLastTry = false
        onImageLinkSubmitted(address)
    }

    fun fetchTypedLink() {
        webImageAddressOrNull(linkText)?.let { address -> fetchImage(address) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .defaultMinSize(minHeight = 52.dp)
            .clip(DefaultBlockShape)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (inSelectionMode) onToggleSelection() },
                        onLongClick = if (isDesktopPlatform) null else onToggleSelection
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Icon(painterResource(Res.drawable.image), contentDescription = null, tint = hintColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (linkText.isEmpty()) {
                        Text(
                            text = "Paste an image link, or upload one",
                            style = MaterialTheme.typography.bodyLarge,
                            color = hintColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = linkText,
                        onValueChange = { newText ->
                            val wasPasted = newText.length - linkText.length > 1
                            linkText = newText
                            hasEditedLinkSinceLastTry = true
                            val address = webImageAddressOrNull(newText)
                            if (wasPasted && address != null) fetchImage(address)
                        },
                        enabled = !inSelectionMode && !isFetching,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { fetchTypedLink() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPreviewKeyEvent { event ->
                                val isEnterPress = event.type == KeyEventType.KeyDown &&
                                    (event.key == Key.Enter || event.key == Key.NumPadEnter)
                                if (isEnterPress) fetchTypedLink()
                                isEnterPress
                            }
                    )
                }
                if (isFetching) {
                    Spacer(Modifier.width(8.dp))
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onSurface,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            )
            Spacer(Modifier.width(EmptyImageButtonSidePadding))
            EmptyImageBlockButton(
                icon = Res.drawable.upload2,
                contentDescription = "Upload Image",
                onClick = { if (inSelectionMode) onToggleSelection() else onRequestPicker() }
            )
            if (!isDesktopPlatform) {
                EmptyImageBlockButton(
                    icon = Res.drawable.camera,
                    contentDescription = "Take Photo",
                    onClick = { if (inSelectionMode) onToggleSelection() else onRequestCamera() }
                )
            }
            Spacer(Modifier.width(EmptyImageButtonsEndSpace))
        }

        if (hasFetchFailed) {
            Text(
                text = "Couldn't load an image from this link",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 44.dp, end = 14.dp, bottom = 10.dp)
            )
        }
    }
}

@Composable
private fun EmptyImageBlockButton(icon: DrawableResource, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = EmptyImageButtonSidePadding, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painterResource(icon),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
    }
}
