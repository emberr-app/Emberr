package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
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
import emberr.shared.generated.resources.x
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
private val DetailsPosterWidth = 96.dp
private val DetailsNotchRadius = 10.dp
private val DetailsStubVerticalPadding = 12.dp
private val DetailsCloseButtonSize = 36.dp
private val SpacedLabelLetterSpacing = 1.5.sp
private val DesktopTextLineGap = 6.dp
private val PhoneTextLineGap = 2.dp
private val CardHighlightBorderWidth = 2.dp
private val SkeletonLineGap = 9.dp
private val RatingStatusGap = 10.dp
private val RatingLabelGap = 8.dp
private const val RATING_SIZE_SCALE = 2f
private const val RATING_LINE_HEIGHT_SCALE = 1.15f
private const val TICKET_LINE_ALPHA = 0.35f

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
    inBlockSelectionMode: Boolean,
    onToggleBlockSelection: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isInWatchlist: Boolean = false,
    isNewlyAdded: Boolean = false,
    watchlistSelection: TitleCardSelection? = null,
    runAfterKeyboardCloses: (() -> Unit) -> Unit = { it() }
) {
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()
    var isMenuOpen by remember { mutableStateOf(false) }
    var isDetailsOpen by remember { mutableStateOf(false) }
    val openDetails = if (inBlockSelectionMode) onToggleBlockSelection else ({ runAfterKeyboardCloses { isDetailsOpen = true } })
    val onStatusClick = when {
        editor == null -> null
        inBlockSelectionMode -> onToggleBlockSelection
        else -> ({ editor.moveToNextWatchStatus(block) })
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
                    watchlistSelection?.isSelected == true -> Modifier.border(CardHighlightBorderWidth, MaterialTheme.colorScheme.primary, shape)
                    isNewlyAdded -> Modifier.border(CardHighlightBorderWidth, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), shape)
                    else -> Modifier
                }
            )
            .clip(shape)
            .hoverable(hoverSource)
            .titleSelectionMouseClicks(watchlistSelection)
            .then(
                if (watchlistSelection != null) {
                    Modifier.combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { if (watchlistSelection.isSelecting) watchlistSelection.onToggle() },
                        onLongClick = {
                            if (watchlistSelection.isSelecting) watchlistSelection.onToggle() else watchlistSelection.onStartSelecting()
                        }
                    )
                } else {
                    Modifier
                }
            )
    ) {
        if (isDesktopPlatform) {
            SideStubTicketLayout(
                details = details,
                stubWidth = TicketStubWidth,
                notchRadius = TicketNotchRadius,
                mainPart = { mainPartModifier ->
                    TicketMainPart(block = block, details = details, onOpenDetails = openDetails, modifier = mainPartModifier)
                }
            ) {
                TicketStub(block = block, details = details, onStatusClick = onStatusClick)
            }
        } else {
            PhoneTicketContent(
                block = block,
                details = details,
                stubHeight = phoneStubHeight,
                onOpenDetails = openDetails,
                onLongPress = when {
                    watchlistSelection == null -> onToggleBlockSelection
                    watchlistSelection.isSelecting -> watchlistSelection.onToggle
                    else -> watchlistSelection.onStartSelecting
                },
                onStatusClick = onStatusClick
            )
        }

        val cornerModifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 8.dp, end = if (isDesktopPlatform) TicketStubWidth + 8.dp else 8.dp)
        if (watchlistSelection?.isSelecting == true) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = watchlistSelection.onToggle
                    )
            )
            TitleSelectionCheckbox(isSelected = watchlistSelection.isSelected, modifier = cornerModifier)
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

@Composable
private fun BoxScope.SideStubTicketLayout(
    details: MovieDetails,
    stubWidth: Dp,
    notchRadius: Dp,
    mainPart: @Composable (Modifier) -> Unit,
    stub: @Composable BoxScope.() -> Unit
) {
    mainPart(Modifier.fillMaxWidth().padding(end = stubWidth))
    Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
        Box(
            modifier = Modifier
                .width(stubWidth)
                .fillMaxHeight()
                .ticketStub(stubColor = animatedStubColor(details), perforationColor = ticketLineColor(), notchRadius = notchRadius),
            content = stub
        )
    }
}

@Composable
private fun BottomStubTicketLayout(
    details: MovieDetails,
    stubHeight: Dp,
    notchRadius: Dp,
    stubHorizontalPadding: Dp,
    modifier: Modifier,
    mainPart: @Composable () -> Unit,
    stub: @Composable BoxScope.() -> Unit
) {
    Column(modifier = modifier) {
        mainPart()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stubHeight)
                .ticketStub(
                    stubColor = animatedStubColor(details),
                    perforationColor = ticketLineColor(),
                    notchRadius = notchRadius,
                    stubIsAtBottom = true
                )
                .padding(horizontal = stubHorizontalPadding),
            contentAlignment = Alignment.CenterStart,
            content = stub
        )
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
    BottomStubTicketLayout(
        details = details,
        stubHeight = stubHeight,
        notchRadius = PhoneNotchRadius,
        stubHorizontalPadding = TicketPadding,
        modifier = Modifier.fillMaxWidth(),
        mainPart = {
            PosterBesideText(
                gap = PhonePosterGap,
                minPosterHeight = PhonePosterWidth / PosterAspectRatio,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .combinedClickable(onClick = onOpenDetails, onLongClick = onLongPress)
                    .padding(TicketPadding),
                poster = { TicketPoster(details = details, width = PhonePosterWidth, cornerRadius = TicketPosterCornerRadius) }
            ) {
                TicketTitleLines(
                    details = details,
                    credit = details.creditLine(),
                    showsAsWatched = block.isWatched,
                    verticalArrangement = Arrangement.spacedBy(PhoneTextLineGap, Alignment.CenterVertically),
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
        }
    ) {
        RatingBesideStatus(
            modifier = Modifier.fillMaxSize(),
            rating = { RatingBesideLabel(details = details, label = "TMDB rating") },
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
        minPosterHeight = TicketPosterWidth / PosterAspectRatio,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(TicketPadding),
        poster = {
            TicketPoster(
                details = details,
                width = TicketPosterWidth,
                cornerRadius = TicketPosterCornerRadius,
                modifier = Modifier
                    .clip(RoundedCornerShape(TicketPosterCornerRadius))
                    .clickable(onClick = onOpenDetails)
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            TicketTitleLines(
                details = details,
                credit = details.creditLine(),
                showsAsWatched = block.isWatched,
                verticalArrangement = Arrangement.spacedBy(DesktopTextLineGap),
                modifier = Modifier.padding(end = 28.dp),
                creditStyle = MaterialTheme.typography.bodyLarge
            )
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
private fun TicketPoster(details: MovieDetails, width: Dp, cornerRadius: Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier, propagateMinConstraints = true) {
        MoviePoster(
            filePath = details.posterFilePath,
            url = details.posterUrl,
            width = width,
            cornerRadius = cornerRadius,
            savesPosterColor = true
        )
    }
}

@Composable
private fun TicketTitleLines(
    details: MovieDetails,
    credit: String?,
    showsAsWatched: Boolean,
    verticalArrangement: Arrangement.Vertical,
    modifier: Modifier = Modifier,
    creditStyle: TextStyle = MaterialTheme.typography.labelSmall,
    scrollsLongText: Boolean = false
) {
    val textModifier = if (scrollsLongText) Modifier.loopingMarquee() else Modifier
    val textOverflow = if (scrollsLongText) TextOverflow.Clip else TextOverflow.Ellipsis
    Column(modifier = modifier, verticalArrangement = verticalArrangement) {
        SpacedLabel(
            text = listOf(details.kind.label, details.yearText).filter { it.isNotBlank() }.joinToString(" · "),
            modifier = textModifier
        )
        Text(
            text = details.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (showsAsWatched) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (showsAsWatched) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = textOverflow,
            modifier = textModifier
        )
        credit?.let {
            Text(
                text = it,
                style = creditStyle,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = textOverflow,
                modifier = textModifier
            )
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
        val posterWidth = (height * PosterAspectRatio).roundToInt()
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
    onStatusClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = TicketPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RatingNumber(details = details, style = bigRatingStyle())
        SpacedLabel(text = "TMDB rating")
        Spacer(modifier = Modifier.height(14.dp))
        TicketStatusChip(block = block, onClick = onStatusClick)
    }
}

@Composable
private fun RatingNumber(details: MovieDetails, style: TextStyle, modifier: Modifier = Modifier) {
    Text(
        text = details.tmdbRating?.let(::tmdbRatingText) ?: "–",
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = modifier
    )
}

@Composable
private fun RatingBesideLabel(
    details: MovieDetails,
    label: String,
    modifier: Modifier = Modifier,
    labelGap: Dp = RatingLabelGap,
    numberStyle: TextStyle = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
) {
    Row(modifier = modifier) {
        RatingNumber(details = details, style = numberStyle, modifier = Modifier.alignByBaseline())
        Spacer(modifier = Modifier.width(labelGap))
        SpacedLabel(text = label, modifier = Modifier.alignByBaseline())
    }
}

private fun cardTextLineGap(): Dp = if (isDesktopPlatform) DesktopTextLineGap else PhoneTextLineGap

@Composable
private fun bigRatingStyle(): TextStyle = MaterialTheme.typography.titleLarge.let { style ->
    style.copy(
        fontSize = style.fontSize * RATING_SIZE_SCALE,
        lineHeight = style.fontSize * RATING_SIZE_SCALE * RATING_LINE_HEIGHT_SCALE,
        fontWeight = FontWeight.Normal
    )
}

@Composable
private fun titleRatingStyle(): TextStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Normal)

@Composable
private fun ticketLineColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = TICKET_LINE_ALPHA)

@Composable
private fun animatedStubColor(details: MovieDetails): Color =
    animateColorAsState(savedPosterColor(details.posterColorKey)?.stubColor() ?: chipContainerColor()).value

private fun Modifier.ticketStub(
    stubColor: Color,
    perforationColor: Color,
    notchRadius: Dp,
    stubIsAtBottom: Boolean = false
): Modifier = background(stubColor).drawBehind {
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

@Composable
private fun TicketStatusChip(block: MovieBlock, onClick: (() -> Unit)?, showsLabel: Boolean = true) {
    val labelHeight = with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .border(1.dp, ticketLineColor(), CircleShape)
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
    block: MovieBlock,
    details: MovieDetails,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(boardTicketShape())
    ) {
        if (isDesktopPlatform) {
            SideStubTicketLayout(
                details = details,
                stubWidth = BoardStubWidth,
                notchRadius = BoardNotchRadius,
                mainPart = { mainPartModifier -> BoardTicketMainPart(block = block, details = details, modifier = mainPartModifier) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    RatingNumber(details = details, style = titleRatingStyle())
                    SpacedLabel(text = "TMDB")
                }
            }
        } else {
            BottomStubTicketLayout(
                details = details,
                stubHeight = phoneStubHeight(),
                notchRadius = BoardNotchRadius,
                stubHorizontalPadding = BoardPadding,
                modifier = Modifier.fillMaxWidth(),
                mainPart = { BoardTicketMainPart(block = block, details = details, modifier = Modifier.fillMaxWidth()) }
            ) {
                RatingBesideLabel(details = details, label = "TMDB rating")
            }
        }
    }
}

@Composable
private fun BoardTicketMainPart(block: MovieBlock, details: MovieDetails, modifier: Modifier) {
    PosterBesideText(
        gap = BoardPosterGap,
        minPosterHeight = BoardMinPosterHeight,
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(BoardPadding),
        poster = { TicketPoster(details = details, width = BoardMinPosterHeight * PosterAspectRatio, cornerRadius = 4.dp) }
    ) {
        TicketTitleLines(
            details = details,
            credit = details.creditLine(),
            showsAsWatched = block.isWatched,
            verticalArrangement = Arrangement.spacedBy(cardTextLineGap(), Alignment.CenterVertically)
        )
    }
}

@Composable
internal fun GalleryTicketCard(
    block: MovieBlock,
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

    BottomStubTicketLayout(
        details = details,
        stubHeight = stubHeight,
        notchRadius = GalleryNotchRadius,
        stubHorizontalPadding = GalleryPadding,
        modifier = Modifier
            .width(width)
            .then(
                if (selection?.isSelected == true) {
                    Modifier.border(CardHighlightBorderWidth, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .then(clickModifier)
            .background(MaterialTheme.colorScheme.surface),
        mainPart = {
            Box {
                MoviePoster(
                    filePath = details.posterFilePath,
                    url = details.posterUrl,
                    width = width,
                    cornerRadius = 0.dp,
                    savesPosterColor = true
                )
                if (selection?.isSelecting == true) {
                    TitleSelectionCheckbox(
                        isSelected = selection.isSelected,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    )
                }
            }
            TicketTitleLines(
                details = details,
                credit = details.creditNames.joinToString(", "),
                showsAsWatched = block.isWatched,
                verticalArrangement = Arrangement.spacedBy(cardTextLineGap()),
                modifier = Modifier.padding(GalleryPadding)
            )
        }
    ) {
        RatingBesideStatus(
            modifier = Modifier.fillMaxSize(),
            rating = {
                if (isDesktopPlatform) {
                    Column {
                        RatingNumber(details = details, style = titleRatingStyle())
                        SpacedLabel(text = "TMDB")
                    }
                } else {
                    RatingBesideLabel(details = details, label = "TMDB", labelGap = 6.dp)
                }
            },
            fullStatus = { TicketStatusChip(block = block, onClick = onStatusClick) },
            shortStatus = {
                IconTooltip(text = block.watchStatus.label) {
                    TicketStatusChip(block = block, onClick = onStatusClick, showsLabel = false)
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
        val gap = RatingStatusGap.roundToPx()
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
internal fun MovieDetailsTicket(
    details: MovieDetails,
    facts: List<String>,
    background: Color,
    onClose: () -> Unit
) {
    if (isDesktopPlatform) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MovieTicketShape)
        ) {
            SideStubTicketLayout(
                details = details,
                stubWidth = TicketStubWidth,
                notchRadius = TicketNotchRadius,
                mainPart = { mainPartModifier ->
                    DetailsTicketMainPart(details = details, facts = facts, background = background, modifier = mainPartModifier)
                }
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RatingNumber(details = details, style = bigRatingStyle())
                    SpacedLabel(text = "TMDB rating")
                }
                DetailsCloseButton(onClose = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(10.dp))
            }
        }
        return
    }

    val stubHeight = maxOf(
        with(LocalDensity.current) { MaterialTheme.typography.titleLarge.lineHeight.toDp() },
        DetailsCloseButtonSize
    ) + DetailsStubVerticalPadding * 2
    BottomStubTicketLayout(
        details = details,
        stubHeight = stubHeight,
        notchRadius = DetailsNotchRadius,
        stubHorizontalPadding = TicketPadding,
        modifier = Modifier
            .fillMaxWidth()
            .clip(rememberBottomStubShape(stubHeight, DetailsNotchRadius)),
        mainPart = { DetailsTicketMainPart(details = details, facts = facts, background = background, modifier = Modifier.fillMaxWidth()) }
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            RatingBesideLabel(
                details = details,
                label = "TMDB rating",
                modifier = Modifier.weight(1f),
                numberStyle = titleRatingStyle()
            )
            DetailsCloseButton(onClose = onClose)
        }
    }
}

private fun Modifier.loopingMarquee(): Modifier = basicMarquee(iterations = Int.MAX_VALUE)

@Composable
private fun DetailsTicketMainPart(
    details: MovieDetails,
    facts: List<String>,
    background: Color,
    modifier: Modifier
) {
    PosterBesideText(
        gap = PhonePosterGap,
        minPosterHeight = DetailsPosterWidth / PosterAspectRatio,
        modifier = modifier
            .background(background)
            .padding(TicketPadding),
        poster = { TicketPoster(details = details, width = DetailsPosterWidth, cornerRadius = TicketPosterCornerRadius) }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)) {
            TicketTitleLines(
                details = details,
                credit = details.creditLine(),
                showsAsWatched = false,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                scrollsLongText = true
            )
            if (facts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .loopingMarquee(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    facts.forEach { fact -> SpacedLabel(text = fact) }
                }
            }
        }
    }
}

@Composable
private fun DetailsCloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(DetailsCloseButtonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.x),
            contentDescription = "Close details",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
internal fun MovieTicketLoading() {
    if (isDesktopPlatform) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(MovieTicketShape)
        ) {
            TicketSkeletonMainPart(
                posterWidth = TicketPosterWidth,
                posterGap = TicketPosterGap,
                lineSizes = listOf(0.25f to 10.dp, 0.5f to 20.dp, 0.4f to 12.dp),
                linesTopPadding = 4.dp,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(TicketStubWidth)
                    .fillMaxHeight()
                    .background(chipContainerColor())
            )
        }
    } else {
        val stubHeight = phoneStubHeight()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(rememberBottomStubShape(stubHeight, PhoneNotchRadius))
        ) {
            TicketSkeletonMainPart(
                posterWidth = PhonePosterWidth,
                posterGap = PhonePosterGap,
                lineSizes = listOf(0.35f to 10.dp, 0.7f to 20.dp, 0.5f to 12.dp),
                linesTopPadding = 0.dp,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(stubHeight)
                    .background(chipContainerColor())
            )
        }
    }
}

@Composable
private fun TicketSkeletonMainPart(
    posterWidth: Dp,
    posterGap: Dp,
    lineSizes: List<Pair<Float, Dp>>,
    linesTopPadding: Dp,
    verticalAlignment: Alignment.Vertical,
    modifier: Modifier
) {
    val progress = rememberShimmerProgress()
    val shimmerColors = ShimmerColors(
        base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    )
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(TicketPadding),
        horizontalArrangement = Arrangement.spacedBy(posterGap),
        verticalAlignment = verticalAlignment
    ) {
        Box(
            modifier = Modifier
                .width(posterWidth)
                .aspectRatio(PosterAspectRatio)
                .clip(RoundedCornerShape(TicketPosterCornerRadius))
                .shimmer(progress, shimmerColors)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = linesTopPadding),
            verticalArrangement = Arrangement.spacedBy(SkeletonLineGap)
        ) {
            lineSizes.forEach { (widthFraction, height) ->
                MovieSkeletonLine(widthFraction = widthFraction, height = height, progress = progress, colors = shimmerColors)
            }
        }
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
