package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.emberrBorderColor
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.ellipsis
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private val TicketStubWidth = 150.dp
private val TicketNotchRadius = 10.dp
private val TicketCornerRadius = 12.dp
private val TicketPosterWidth = 88.dp
private val TicketPosterCornerRadius = 6.dp
private val TicketPosterGap = 20.dp
private val TicketPadding = 16.dp
private val PerforationDashLength = 4.dp
private val BoardStubWidth = 64.dp
private val BoardNotchRadius = 6.dp
private val BoardPadding = 10.dp
private val BoardPosterGap = 10.dp
private val BoardMinPosterHeight = 66.dp
private val GalleryNotchRadius = 8.dp
private val GalleryPadding = 10.dp
private val GalleryStubVerticalPadding = 8.dp
private val StatusChipVerticalPadding = 4.dp
private val PhonePosterWidth = 64.dp
private val PhonePosterGap = 16.dp
private val PhoneNotchRadius = 9.dp
private val PhoneStubVerticalPadding = 10.dp
private val SpacedLabelLetterSpacing = 1.5.sp
private const val RATING_SIZE_SCALE = 2f
private const val RATING_LINE_HEIGHT_SCALE = 1.15f

internal val MovieTicketShape: Shape = TicketShape(stubSize = TicketStubWidth, notchRadius = TicketNotchRadius)
private val BoardTicketShape: Shape = TicketShape(stubSize = BoardStubWidth, notchRadius = BoardNotchRadius)

private class TicketShape(
    private val stubSize: Dp,
    private val notchRadius: Dp,
    private val stubIsAtBottom: Boolean = false
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val stubSizePx = with(density) { stubSize.toPx() }
        val notchRadiusPx = with(density) { notchRadius.toPx() }
        val cornerRadius = with(density) { TicketCornerRadius.toPx() }
        val notchCenters = if (stubIsAtBottom) {
            listOf(Offset(0f, size.height - stubSizePx), Offset(size.width, size.height - stubSizePx))
        } else {
            listOf(Offset(size.width - stubSizePx, 0f), Offset(size.width - stubSizePx, size.height))
        }
        val ticket = Path().apply { addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(cornerRadius))) }
        val notches = Path().apply {
            notchCenters.forEach { center -> addOval(Rect(center = center, radius = notchRadiusPx)) }
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, ticket, notches))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MovieTicketCard(
    block: MovieBlock,
    details: MovieDetails,
    editor: MovieBlockEditor?,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isInWatchlist: Boolean = false,
    isNewlyAdded: Boolean = false,
    selection: TitleCardSelection? = null,
    runAfterKeyboardCloses: (() -> Unit) -> Unit = { it() }
) {
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    var isMenuOpen by remember { mutableStateOf(false) }
    var isDetailsOpen by remember { mutableStateOf(false) }
    val openDetails = if (inSelectionMode) onToggleSelection else ({ runAfterKeyboardCloses { isDetailsOpen = true } })
    val onStatusClick = when {
        editor == null -> null
        inSelectionMode -> onToggleSelection
        else -> ({ editor.showNextWatchStatus(block) })
    }
    val phoneStubHeight = phoneStubHeight()
    val shape = if (isDesktopPlatform) MovieTicketShape else rememberBottomStubShape(phoneStubHeight, PhoneNotchRadius)

    if (isDetailsOpen) {
        MovieDetailsDialog(
            block = block,
            details = details,
            editor = editor,
            onDismiss = { isDetailsOpen = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when {
                    selection?.isSelected == true -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                    isNewlyAdded -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), shape)
                    else -> Modifier
                }
            )
            .clip(shape)
            .hoverable(hoverSource)
            .titleSelectionMouseClicks(selection)
            .then(
                if (selection != null) {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (selection.isSelecting) selection.onToggle() },
                        onLongClick = { if (selection.isSelecting) selection.onToggle() else selection.onStartSelecting() }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        if (isDesktopPlatform) {
            TicketMainPart(
                block = block,
                details = details,
                onOpenDetails = openDetails,
                modifier = Modifier.fillMaxWidth().padding(end = TicketStubWidth)
            )
            Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                TicketStub(
                    block = block,
                    details = details,
                    onStatusClick = onStatusClick,
                    modifier = Modifier.width(TicketStubWidth).fillMaxHeight()
                )
            }
        } else {
            PhoneTicketContent(
                block = block,
                details = details,
                stubHeight = phoneStubHeight,
                onOpenDetails = openDetails,
                onLongPress = when {
                    selection == null -> onToggleSelection
                    selection.isSelecting -> selection.onToggle
                    else -> selection.onStartSelecting
                },
                onStatusClick = onStatusClick
            )
        }

        val cornerModifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 8.dp, end = if (isDesktopPlatform) TicketStubWidth + 8.dp else 8.dp)
        if (selection?.isSelecting == true) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = selection.onToggle
                    )
            )
            TitleSelectionCheckbox(isSelected = selection.isSelected, modifier = cornerModifier)
        } else if (editor != null) {
            Box(modifier = cornerModifier) {
                if (!isDesktopPlatform || isHovered || isMenuOpen) {
                    IconTooltip(text = "Options") {
                        Icon(
                            painter = painterResource(Res.drawable.ellipsis),
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { runAfterKeyboardCloses { isMenuOpen = true } }
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                }
                MovieOptionsMenu(
                    expanded = isMenuOpen,
                    title = details.title,
                    onDismiss = { isMenuOpen = false },
                    onChangeTitle = if (isInWatchlist) null else ({ editor.changeTitle(block.id) }),
                    onRefreshDetails = { editor.refreshDetails(block.id, details) },
                    deleteLabel = if (isInWatchlist) "Remove from watchlist" else "Delete block",
                    onDelete = onDelete
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhoneTicketContent(
    block: MovieBlock,
    details: MovieDetails,
    stubHeight: Dp,
    onOpenDetails: () -> Unit,
    onLongPress: () -> Unit,
    onStatusClick: (() -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PosterBesideText(
            gap = PhonePosterGap,
            minPosterHeight = PhonePosterWidth * 1.5f,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(onClick = onOpenDetails, onLongClick = onLongPress)
                .padding(TicketPadding),
            poster = {
                Box(propagateMinConstraints = true) {
                    MoviePoster(
                        filePath = details.posterFilePath,
                        url = details.posterUrl,
                        width = PhonePosterWidth,
                        cornerRadius = TicketPosterCornerRadius,
                        onImageLoaded = { loadedImage -> savePosterColor(details.posterColorKey, loadedImage) }
                    )
                }
            }
        ) {
            Column(
                modifier = Modifier.padding(end = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
            ) {
                SpacedLabel(
                    text = listOf(details.kind.label, details.yearText).filter { it.isNotBlank() }.joinToString(" · ")
                )
                Text(
                    text = details.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (block.isWatched) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (block.isWatched) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                details.creditLine()?.let { credit ->
                    Text(
                        text = credit,
                        style = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        RatingBesideStatus(
            modifier = Modifier
                .fillMaxWidth()
                .height(stubHeight)
                .ticketStub(details = details, notchRadius = PhoneNotchRadius, stubIsAtBottom = true)
                .padding(horizontal = TicketPadding),
            rating = {
                Row {
                    Text(
                        text = details.tmdbRating?.let(::tmdbRatingText) ?: "–",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.alignByBaseline()
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    SpacedLabel(text = "TMDB rating", modifier = Modifier.alignByBaseline())
                }
            },
            fullStatus = { TicketStatusChip(block = block, onClick = onStatusClick) },
            shortStatus = { TicketStatusChip(block = block, onClick = onStatusClick, showsLabel = false) }
        )
    }
}

@Composable
private fun phoneStubHeight(): Dp =
    with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() } +
        StatusChipVerticalPadding * 2 + PhoneStubVerticalPadding * 2

@Composable
private fun rememberBottomStubShape(stubHeight: Dp, notchRadius: Dp): Shape =
    remember(stubHeight, notchRadius) { TicketShape(stubSize = stubHeight, notchRadius = notchRadius, stubIsAtBottom = true) }

@Composable
private fun TicketMainPart(
    block: MovieBlock,
    details: MovieDetails,
    onOpenDetails: () -> Unit,
    modifier: Modifier
) {
    PosterBesideText(
        gap = TicketPosterGap,
        minPosterHeight = TicketPosterWidth * 1.5f,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(TicketPadding),
        poster = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(TicketPosterCornerRadius))
                    .clickable(onClick = onOpenDetails),
                propagateMinConstraints = true
            ) {
                MoviePoster(
                    filePath = details.posterFilePath,
                    url = details.posterUrl,
                    width = TicketPosterWidth,
                    cornerRadius = TicketPosterCornerRadius,
                    onImageLoaded = { loadedImage -> savePosterColor(details.posterColorKey, loadedImage) }
                )
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Column(
                modifier = Modifier.padding(end = 28.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SpacedLabel(
                    text = listOf(details.kind.label, details.yearText).filter { it.isNotBlank() }.joinToString(" · ")
                )
                Text(
                    text = details.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (block.isWatched) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (block.isWatched) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                details.creditLine()?.let { credit ->
                    Text(
                        text = credit,
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column {
                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
                    thickness = 0.5.dp,
                    color = emberrBorderColor
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        details.lengthFact()?.let { (label, value) -> TicketFact(label = label, value = value) }
                        if (details.genres.isNotEmpty()) {
                            TicketFact(label = "Genre", value = details.genres.joinToString(" / "))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Show details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textDecoration = TextDecoration.Underline,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onOpenDetails)
                            .padding(horizontal = 2.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PosterBesideText(
    gap: Dp,
    minPosterHeight: Dp,
    modifier: Modifier,
    poster: @Composable () -> Unit,
    text: @Composable () -> Unit
) {
    Layout(contents = listOf(poster, text), modifier = modifier) { (posterParts, textParts), constraints ->
        val textPart = textParts.first()
        val gapPx = gap.roundToPx()
        val height = maxOf(textPart.minIntrinsicHeight(constraints.maxWidth), minPosterHeight.roundToPx())
        val posterWidth = (height * 2f / 3f).roundToInt()
        val posterPlaceable = posterParts.first().measure(Constraints.fixed(posterWidth, height))
        val textWidth = (constraints.maxWidth - posterWidth - gapPx).coerceAtLeast(0)
        val textPlaceable = textPart.measure(Constraints.fixed(textWidth, height))
        layout(constraints.maxWidth, height) {
            posterPlaceable.place(0, 0)
            textPlaceable.place(posterWidth + gapPx, 0)
        }
    }
}

@Composable
private fun TicketStub(
    block: MovieBlock,
    details: MovieDetails,
    onStatusClick: (() -> Unit)?,
    modifier: Modifier
) {
    val ratingStyle = MaterialTheme.typography.titleLarge.let { style ->
        style.copy(
            fontSize = style.fontSize * RATING_SIZE_SCALE,
            lineHeight = style.fontSize * RATING_SIZE_SCALE * RATING_LINE_HEIGHT_SCALE,
            fontWeight = FontWeight.Normal
        )
    }

    Column(
        modifier = modifier
            .ticketStub(details = details, notchRadius = TicketNotchRadius)
            .padding(horizontal = 12.dp, vertical = TicketPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = details.tmdbRating?.let(::tmdbRatingText) ?: "–",
            style = ratingStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
        SpacedLabel(text = "TMDB rating")
        Spacer(modifier = Modifier.height(14.dp))
        TicketStatusChip(block = block, onClick = onStatusClick)
    }
}

@Composable
private fun Modifier.ticketStub(details: MovieDetails, notchRadius: Dp, stubIsAtBottom: Boolean = false): Modifier {
    val posterColor = savedPosterColor(details.posterColorKey)
    val stubColor by animateColorAsState(posterColor?.stubColor() ?: chipContainerColor())
    val perforationColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    return background(stubColor).drawBehind {
        val dashLength = PerforationDashLength.toPx()
        val edgeGap = notchRadius.toPx() + dashLength
        drawLine(
            color = perforationColor,
            start = if (stubIsAtBottom) Offset(edgeGap, 0f) else Offset(0f, edgeGap),
            end = if (stubIsAtBottom) Offset(size.width - edgeGap, 0f) else Offset(0f, size.height - edgeGap),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashLength, dashLength))
        )
    }
}

@Composable
private fun TicketStatusChip(block: MovieBlock, onClick: (() -> Unit)?, showsLabel: Boolean = true) {
    val labelHeight = with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f), CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = StatusChipVerticalPadding)
            .height(labelHeight),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(block.watchStatus.dotColor(), CircleShape)
        )
        if (showsLabel) {
            Text(
                text = block.watchStatus.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun TicketFact(label: String, value: String) {
    Column {
        SpacedLabel(text = label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SpacedLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        letterSpacing = SpacedLabelLetterSpacing,
        color = MaterialTheme.colorScheme.outline,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
internal fun boardTicketShape(): Shape =
    if (isDesktopPlatform) BoardTicketShape else rememberBottomStubShape(phoneStubHeight(), BoardNotchRadius)

@Composable
internal fun BoardTicketCard(
    title: MovieBlock,
    details: MovieDetails,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(boardTicketShape())
    ) {
        if (isDesktopPlatform) {
            BoardTicketMainPart(
                title = title,
                details = details,
                modifier = Modifier.fillMaxWidth().padding(end = BoardStubWidth)
            )
            Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                Column(
                    modifier = Modifier
                        .width(BoardStubWidth)
                        .fillMaxHeight()
                        .ticketStub(details = details, notchRadius = BoardNotchRadius)
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = details.tmdbRating?.let(::tmdbRatingText) ?: "–",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    SpacedLabel(text = "TMDB")
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                BoardTicketMainPart(title = title, details = details, modifier = Modifier.fillMaxWidth())
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(phoneStubHeight())
                        .ticketStub(details = details, notchRadius = BoardNotchRadius, stubIsAtBottom = true)
                        .padding(horizontal = BoardPadding),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row {
                        Text(
                            text = details.tmdbRating?.let(::tmdbRatingText) ?: "–",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.alignByBaseline()
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SpacedLabel(text = "TMDB rating", modifier = Modifier.alignByBaseline())
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardTicketMainPart(title: MovieBlock, details: MovieDetails, modifier: Modifier) {
    PosterBesideText(
        gap = BoardPosterGap,
        minPosterHeight = BoardMinPosterHeight,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(BoardPadding),
        poster = {
            Box(propagateMinConstraints = true) {
                MoviePoster(
                    filePath = details.posterFilePath,
                    url = details.posterUrl,
                    width = BoardMinPosterHeight * 2f / 3f,
                    cornerRadius = 4.dp,
                    onImageLoaded = { loadedImage -> savePosterColor(details.posterColorKey, loadedImage) }
                )
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)) {
            SpacedLabel(
                text = listOf(details.kind.label, details.yearText).filter { it.isNotBlank() }.joinToString(" · ")
            )
            Text(
                text = details.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (title.isWatched) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (title.isWatched) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            details.creditLine()?.let { credit ->
                Text(
                    text = credit,
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun GalleryTicketCard(
    title: MovieBlock,
    details: MovieDetails,
    width: Dp,
    selection: TitleCardSelection?,
    onStatusClick: () -> Unit,
    clickModifier: Modifier
) {
    val stubHeight = if (isDesktopPlatform) {
        with(LocalDensity.current) {
            MaterialTheme.typography.titleLarge.lineHeight.toDp() + MaterialTheme.typography.labelSmall.lineHeight.toDp()
        } + GalleryStubVerticalPadding * 2
    } else {
        phoneStubHeight()
    }
    val shape = rememberBottomStubShape(stubHeight, GalleryNotchRadius)

    Column(
        modifier = Modifier
            .width(width)
            .then(
                if (selection?.isSelected == true) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .then(clickModifier)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Box {
            MoviePoster(
                filePath = details.posterFilePath,
                url = details.posterUrl,
                width = width,
                cornerRadius = 0.dp,
                onImageLoaded = { loadedImage -> savePosterColor(details.posterColorKey, loadedImage) }
            )
            if (selection?.isSelecting == true) {
                TitleSelectionCheckbox(
                    isSelected = selection.isSelected,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                )
            }
        }
        Column(
            modifier = Modifier.padding(GalleryPadding),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            SpacedLabel(
                text = listOf(details.kind.label, details.yearText).filter { it.isNotBlank() }.joinToString(" · ")
            )
            Text(
                text = details.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (title.isWatched) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (title.isWatched) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = details.creditNames.joinToString(", "),
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        RatingBesideStatus(
            modifier = Modifier
                .fillMaxWidth()
                .height(stubHeight)
                .ticketStub(details = details, notchRadius = GalleryNotchRadius, stubIsAtBottom = true)
                .padding(horizontal = GalleryPadding),
            rating = {
                if (isDesktopPlatform) {
                    Column {
                        Text(
                            text = details.tmdbRating?.let(::tmdbRatingText) ?: "–",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        SpacedLabel(text = "TMDB")
                    }
                } else {
                    Row {
                        Text(
                            text = details.tmdbRating?.let(::tmdbRatingText) ?: "–",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            modifier = Modifier.alignByBaseline()
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        SpacedLabel(text = "TMDB", modifier = Modifier.alignByBaseline())
                    }
                }
            },
            fullStatus = { TicketStatusChip(block = title, onClick = onStatusClick) },
            shortStatus = {
                IconTooltip(text = title.watchStatus.label) {
                    TicketStatusChip(block = title, onClick = onStatusClick, showsLabel = false)
                }
            }
        )
    }
}

@Composable
private fun RatingBesideStatus(
    modifier: Modifier,
    rating: @Composable () -> Unit,
    fullStatus: @Composable () -> Unit,
    shortStatus: @Composable () -> Unit
) {
    Layout(contents = listOf(rating, fullStatus, shortStatus), modifier = modifier) { (ratingParts, fullParts, shortParts), constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val ratingPlaceable = ratingParts.first().measure(looseConstraints)
        val gap = GalleryPadding.roundToPx()
        val fullStatusPlaceable = fullParts.first().measure(looseConstraints)
        val statusPlaceable = if (ratingPlaceable.width + gap + fullStatusPlaceable.width <= constraints.maxWidth) {
            fullStatusPlaceable
        } else {
            shortParts.first().measure(looseConstraints)
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            ratingPlaceable.place(0, (constraints.maxHeight - ratingPlaceable.height) / 2)
            statusPlaceable.place(
                constraints.maxWidth - statusPlaceable.width,
                (constraints.maxHeight - statusPlaceable.height) / 2
            )
        }
    }
}

@Composable
internal fun MovieTicketLoading() {
    val progress = rememberShimmerProgress()
    val shimmerColors = ShimmerColors(
        base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    )
    if (!isDesktopPlatform) {
        PhoneTicketLoading(progress = progress, shimmerColors = shimmerColors)
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(MovieTicketShape)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
                .padding(TicketPadding),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(TicketPosterWidth)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(TicketPosterCornerRadius))
                    .shimmer(progress, shimmerColors)
            )
            Column(
                modifier = Modifier.weight(1f).padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                listOf(0.25f to 10.dp, 0.5f to 20.dp, 0.4f to 12.dp).forEach { (widthFraction, height) ->
                    MovieSkeletonLine(widthFraction = widthFraction, height = height, progress = progress, colors = shimmerColors)
                }
            }
        }
        Box(
            modifier = Modifier
                .width(TicketStubWidth)
                .fillMaxHeight()
                .background(chipContainerColor())
        )
    }
}

@Composable
private fun PhoneTicketLoading(progress: State<Float>, shimmerColors: ShimmerColors) {
    val stubHeight = phoneStubHeight()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rememberBottomStubShape(stubHeight, PhoneNotchRadius))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(TicketPadding),
            horizontalArrangement = Arrangement.spacedBy(PhonePosterGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(PhonePosterWidth)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(TicketPosterCornerRadius))
                    .shimmer(progress, shimmerColors)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                listOf(0.35f to 10.dp, 0.7f to 20.dp, 0.5f to 12.dp).forEach { (widthFraction, height) ->
                    MovieSkeletonLine(widthFraction = widthFraction, height = height, progress = progress, colors = shimmerColors)
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stubHeight)
                .background(chipContainerColor())
        )
    }
}

private fun MovieDetails.creditLine(): String? {
    if (creditNames.isEmpty()) return null
    val prefix = if (kind == MovieKind.MOVIE) "Directed by" else "Created by"
    return "$prefix ${creditNames.joinToString(", ")}"
}

private fun MovieDetails.lengthFact(): Pair<String, String>? = when (kind) {
    MovieKind.MOVIE -> runtimeMinutes?.let { "Runtime" to minutesText(it) }
    MovieKind.SHOW -> seasonCount?.let { "Seasons" to it.toString() }
}
