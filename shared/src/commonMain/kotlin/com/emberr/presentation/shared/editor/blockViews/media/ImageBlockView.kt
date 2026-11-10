package com.emberr.presentation.shared.editor.blockViews.media

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.sync.MediaRetryCoordinator
import com.emberr.domain.util.media.ImageClipboard
import com.emberr.domain.util.media.ImageDownloader
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.domain.util.media.webImageAddressOrNull
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.showFeedback
import com.emberr.presentation.LocalImageOverlay
import com.emberr.presentation.shared.editor.DefaultBlockShape
import com.emberr.presentation.shared.editor.ImageLinkFetch
import com.emberr.presentation.shared.editor.links.rememberWebLinkActions
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.camera
import emberr.shared.generated.resources.circle_x
import emberr.shared.generated.resources.image
import emberr.shared.generated.resources.upload2
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import java.io.File
import kotlin.math.abs
import emberr.shared.generated.resources.editor_failed_to_copy_image
import emberr.shared.generated.resources.editor_failed_to_save_image
import emberr.shared.generated.resources.editor_image_copied
import emberr.shared.generated.resources.editor_image_saved_to_downloads
import emberr.shared.generated.resources.editor_image_saved_to_photos
import org.jetbrains.compose.resources.getString
import emberr.shared.generated.resources.editor_could_not_load_image_from_link
import emberr.shared.generated.resources.editor_downloading_image
import emberr.shared.generated.resources.editor_failed_to_download_image
import emberr.shared.generated.resources.editor_note_image
import emberr.shared.generated.resources.editor_paste_image_link_or_upload
import emberr.shared.generated.resources.editor_take_photo
import emberr.shared.generated.resources.editor_take_photo_title_case
import emberr.shared.generated.resources.editor_upload_image
import emberr.shared.generated.resources.editor_upload_image_title_case
import org.jetbrains.compose.resources.stringResource

private val MaximumImageBlockHeight = 320.dp
private val LoadingImageBlockHeight = 180.dp
private val EmptyImageButtonSidePadding = 8.dp
private val EmptyImageButtonsEndSpace = 6.dp
private val knownImageRatiosByPath = mutableMapOf<String, Float>()
private const val NOTICEABLE_RATIO_DIFFERENCE = 0.01f

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
        val transferText = rememberMediaTransferText(fileName, availability)

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
                            stringResource(Res.string.editor_failed_to_download_image),
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
                            transferText ?: stringResource(Res.string.editor_downloading_image),
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

            var widthToHeightRatio by remember(absolutePath) {
                val knownRatio = knownImageRatiosByPath[absolutePath]
                    ?: readImageWidthToHeightRatio(absolutePath)?.also { knownImageRatiosByPath[absolutePath] = it }
                mutableStateOf(knownRatio)
            }
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
                        contentDescription = stringResource(Res.string.editor_note_image),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { successState ->
                            val loadedImage = successState.result.image
                            if (loadedImage.width > 0 && loadedImage.height > 0) {
                                val loadedRatio = loadedImage.width.toFloat() / loadedImage.height.toFloat()
                                val shownRatio = widthToHeightRatio
                                if (shownRatio == null || abs(shownRatio - loadedRatio) > NOTICEABLE_RATIO_DIFFERENCE) {
                                    knownImageRatiosByPath[absolutePath] = loadedRatio
                                    widthToHeightRatio = loadedRatio
                                }
                            }
                        }
                    )
                    AnimatedGifLayer(absolutePath, ContentScale.Crop, Modifier.fillMaxSize())
                    transferText?.let { text ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            LaunchedEffect(showFullScreen, request) {
                if (showFullScreen) {
                    setFullScreenOverlay?.invoke {
                        com.emberr.presentation.shared.editor.components.FullScreenImageScreen(
                            request = request,
                            localImagePath = absolutePath,
                            hasLocalFile = true,
                            thumbnailBoundsInRoot = thumbnailBoundsInRoot,
                            imageWidthToHeightRatio = widthToHeightRatio,
                            onBack = { showFullScreen = false },
                            onDownload = {
                                coroutineScope.launch {
                                    val success = imageDownloader.downloadImage(absolutePath, fileName)
                                    showFeedback(
                                        if (success) {
                                            if (isDesktopPlatform) getString(Res.string.editor_image_saved_to_downloads) else getString(Res.string.editor_image_saved_to_photos)
                                        } else {
                                            getString(Res.string.editor_failed_to_save_image)
                                        }
                                    )
                                }
                            },
                            onCopy = {
                                coroutineScope.launch {
                                    val success = ImageClipboard.copyImageToClipboard(absolutePath)
                                    showFeedback(if (success) getString(Res.string.editor_image_copied) else getString(Res.string.editor_failed_to_copy_image))
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
                            text = stringResource(Res.string.editor_paste_image_link_or_upload),
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
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
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
            IconTooltip(text = stringResource(Res.string.editor_upload_image)) {
                EmptyImageBlockButton(
                    icon = Res.drawable.upload2,
                    contentDescription = stringResource(Res.string.editor_upload_image_title_case),
                    onClick = { if (inSelectionMode) onToggleSelection() else onRequestPicker() }
                )
            }
            if (!isDesktopPlatform) {
                IconTooltip(text = stringResource(Res.string.editor_take_photo)) {
                    EmptyImageBlockButton(
                        icon = Res.drawable.camera,
                        contentDescription = stringResource(Res.string.editor_take_photo_title_case),
                        onClick = { if (inSelectionMode) onToggleSelection() else onRequestCamera() }
                    )
                }
            }
            Spacer(Modifier.width(EmptyImageButtonsEndSpace))
        }

        if (hasFetchFailed) {
            Text(
                text = stringResource(Res.string.editor_could_not_load_image_from_link),
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
