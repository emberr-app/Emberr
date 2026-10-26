package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieCastMember
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieNamedLogo
import com.emberr.domain.model.MovieTrailer
import com.emberr.domain.model.MovieWatchProviders
import com.emberr.domain.model.allProviders
import com.emberr.domain.model.hasBackdrop
import com.emberr.domain.model.hasPoster
import com.emberr.domain.model.starsTextOf
import com.emberr.domain.model.youtubeLink
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.EmberrShadowElevation
import com.emberr.presentation.shared.components.EmberrTab
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.fullScreenDialogProperties
import com.emberr.presentation.shared.editor.DefaultBlockShape
import com.emberr.ui.theme.LocalAppIsDark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.link
import emberr.shared.generated.resources.play
import emberr.shared.generated.resources.star
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private val DialogShape = RoundedCornerShape(24.dp)
private val FactShape = RoundedCornerShape(8.dp)
private val CastPortraitMinWidthDesktop = 100.dp
private val CastPortraitMinWidthPhone = 88.dp
private val CastColumnGap = 24.dp
private val CastRowGap = 28.dp
private val BackdropHeightDesktop = 320.dp
private val BackdropHeightPhone = 230.dp
private val BackdropContentOverlap = 96.dp
private val BackdropCropAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.6f)
private val SectionGap = 32.dp
private val TabContentGap = 20.dp
private val FactLabelWidthDesktop = 140.dp
private val FactLabelWidthPhone = 112.dp
private val ProviderLabelWidthDesktop = 80.dp
private val ProviderTileSize = 40.dp
private val LogoTileShape = RoundedCornerShape(8.dp)
private val CompanyLogoShape = RoundedCornerShape(4.dp)
private val CompanyLogoWidth = 48.dp
private val CompanyLogoHeight = 24.dp
private val CompanyChipHeight = 32.dp
private val DialogScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.5f),
    0.35f to Color.Black.copy(alpha = 0.7f),
    1f to Color.Black.copy(alpha = 0.85f)
)
private const val MAXIMUM_STAR_HALVES = 10
private val BackdropFade = Brush.verticalGradient(0.5f to Color.Black, 1f to Color.Transparent)

private enum class MovieDetailsTab(val label: String) {
    OVERVIEW("Overview"),
    CAST("Cast"),
    SEASONS("Seasons")
}

internal data class DetailsColors(
    val text: Color,
    val secondaryText: Color,
    val panel: Color,
    val chipBackground: Color,
    val chipText: Color,
    val chipBorder: Color?,
    val emptyStar: Color,
    val avatarBackground: Color
)

@Composable
private fun detailsColors(isOnPoster: Boolean): DetailsColors =
    if (isOnPoster) {
        DetailsColors(
            text = Color.White,
            secondaryText = Color.White.copy(alpha = 0.7f),
            panel = Color.White.copy(alpha = 0.08f),
            chipBackground = Color.White.copy(alpha = 0.12f),
            chipText = Color.White,
            chipBorder = Color.White.copy(alpha = 0.24f),
            emptyStar = Color.White.copy(alpha = 0.22f),
            avatarBackground = Color.White.copy(alpha = 0.14f)
        )
    } else {
        DetailsColors(
            text = MaterialTheme.colorScheme.onSurface,
            secondaryText = MaterialTheme.colorScheme.outline,
            panel = if (LocalAppIsDark.current) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
            chipBackground = chipContainerColor(),
            chipText = MaterialTheme.colorScheme.onSurface,
            chipBorder = null,
            emptyStar = chipContainerColor(),
            avatarBackground = chipContainerColor()
        )
    }

@Composable
internal fun MovieDetailsDialog(
    block: MovieBlock,
    details: MovieDetails,
    editor: MovieBlockEditor?,
    onDismiss: () -> Unit,
    isPreview: Boolean = false,
    liveLoad: LiveTmdbLoadState<*>? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = fullScreenDialogProperties()
    ) {
        val dialogBackgroundColor = if (LocalAppIsDark.current) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.background
        }
        val hasPoster = details.hasPoster
        val colors = detailsColors(isOnPoster = hasPoster)
        val horizontalPadding = if (isDesktopPlatform) 32.dp else 24.dp
        val hazeState = remember { HazeState() }
        var selectedTab by remember { mutableStateOf(MovieDetailsTab.OVERVIEW) }
        var openCastMember by remember { mutableStateOf<MovieCastMember?>(null) }
        val shownTabs = shownTabsOf(details)
        val shownTab = selectedTab.takeIf { it in shownTabs } ?: shownTabs.firstOrNull()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isDesktopPlatform) 0.6f else 0.94f)
                    .widthIn(max = 760.dp)
                    .fillMaxHeight(if (isDesktopPlatform) 0.8f else 0.92f)
                    .heightIn(max = if (isDesktopPlatform) 720.dp else Dp.Unspecified)
                    .safeDrawingPadding()
                    .clip(DialogShape)
                    .background(if (hasPoster) PosterBackdropBaseColor else dialogBackgroundColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Box(modifier = Modifier.matchParentSize().hazeSource(hazeState)) {
                    if (hasPoster) {
                        BlurredPosterBackdrop(details = details, blurRadius = 60.dp, scrim = DialogScrim)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        val backdropHeight = if (isDesktopPlatform) BackdropHeightDesktop else BackdropHeightPhone
                        if (details.hasBackdrop) {
                            MovieBackdropBanner(details = details, height = backdropHeight)
                        }
                        Column(
                            modifier = Modifier.padding(
                                start = horizontalPadding,
                                end = horizontalPadding,
                                top = if (details.hasBackdrop) backdropHeight - BackdropContentOverlap else 72.dp,
                                bottom = 32.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(SectionGap)
                        ) {
                            MovieDetailsHeader(
                                block = block,
                                details = details,
                                editor = editor,
                                colors = colors,
                                showsWatchState = !isPreview
                            )
                            val hasRatingPanelContent = !isPreview || details.tmdbRating != null || details.trailer != null
                            if (!isDesktopPlatform && hasRatingPanelContent) {
                                MovieRatingPanel(
                                    block = block,
                                    details = details,
                                    editor = editor,
                                    colors = colors,
                                    showsWatchState = !isPreview
                                )
                            }
                            if (liveLoad != null) {
                                LiveTmdbLoadMessage(state = liveLoad, textColor = colors.text, chipBackground = colors.chipBackground)
                            } else if (shownTab != null) {
                                Column(verticalArrangement = Arrangement.spacedBy(TabContentGap)) {
                                    if (shownTabs.size > 1) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            shownTabs.forEach { tab ->
                                                EmberrTab(
                                                    label = tab.label,
                                                    isSelected = tab == shownTab,
                                                    contentColor = colors.text,
                                                    onClick = { selectedTab = tab }
                                                )
                                            }
                                        }
                                    }
                                    when (shownTab) {
                                        MovieDetailsTab.OVERVIEW -> MovieOverviewTab(details = details, colors = colors)
                                        MovieDetailsTab.CAST -> MovieCastGrid(
                                            cast = details.cast,
                                            colors = colors,
                                            onOpenPerson = { member -> openCastMember = member }
                                        )
                                        MovieDetailsTab.SEASONS -> MovieSeasonsTab(details = details, colors = colors)
                                    }
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .emberrBorder(CircleShape)
                        .clip(CircleShape)
                ) {
                    TopBarIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = "Close details",
                        bgColor = Color.Transparent,
                        tint = colors.text,
                        hazeState = hazeState,
                        hazeStyle = if (hasPoster) EmberrBlur.OnImage else EmberrBlur.Regular,
                        shadowElevation = EmberrShadowElevation.None,
                        tooltip = "Close",
                        onClick = onDismiss
                    )
                }
            }
        }

        openCastMember?.let { member ->
            member.personId?.let { personId ->
                MoviePersonSheet(member = member, personId = personId, onDismiss = { openCastMember = null })
            }
        }
    }
}

@Composable
private fun MovieBackdropBanner(details: MovieDetails, height: Dp) {
    MovieImage(
        filePath = details.backdropFilePath,
        url = details.backdropUrl,
        alignment = BackdropCropAlignment,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(BackdropFade, blendMode = BlendMode.DstIn)
            }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieDetailsHeader(
    block: MovieBlock,
    details: MovieDetails,
    editor: MovieBlockEditor?,
    colors: DetailsColors,
    showsWatchState: Boolean
) {
    val posterWidth = if (isDesktopPlatform) 150.dp else 104.dp

    val titleAndTagline: @Composable ColumnScope.() -> Unit = {
        Text(text = details.title, style = MaterialTheme.typography.titleLarge, color = colors.text)
        if (details.tagline.isNotBlank()) {
            Text(
                text = details.tagline,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = colors.secondaryText
            )
        }
    }
    val factChips: @Composable () -> Unit = {
        val facts = headerFactsOf(details)
        if (facts.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                facts.forEach { fact -> MovieFactChip(text = fact, colors = colors) }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(if (isDesktopPlatform) 24.dp else 18.dp)) {
            Box(modifier = if (details.hasPoster) Modifier.shadow(16.dp, DefaultBlockShape) else Modifier) {
                MoviePoster(filePath = details.posterFilePath, url = details.posterUrl, width = posterWidth, cornerRadius = PosterCornerRadius)
            }
            Column(
                modifier = Modifier.weight(1f).padding(end = 36.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                titleAndTagline()
                if (isDesktopPlatform) {
                    Spacer(modifier = Modifier.height(6.dp))
                    factChips()
                    FlowRow(
                        modifier = Modifier.padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MovieRatingItems(
                            block = block,
                            details = details,
                            editor = editor,
                            colors = colors,
                            showsWatchState = showsWatchState
                        )
                    }
                    details.trailer?.let { trailer ->
                        MovieTrailerButton(trailer = trailer, colors = colors, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
        if (!isDesktopPlatform) factChips()
    }
}

@Composable
private fun MovieFactChip(text: String, colors: DetailsColors) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = colors.chipText,
        maxLines = 1,
        modifier = Modifier
            .clip(FactShape)
            .background(colors.chipBackground)
            .then(if (colors.chipBorder != null) Modifier.border(1.dp, colors.chipBorder, FactShape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieRatingPanel(
    block: MovieBlock,
    details: MovieDetails,
    editor: MovieBlockEditor?,
    colors: DetailsColors,
    showsWatchState: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DefaultBlockShape)
            .background(colors.panel)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            MovieRatingItems(
                block = block,
                details = details,
                editor = editor,
                colors = colors,
                showsWatchState = showsWatchState
            )
        }
        details.trailer?.let { trailer -> MovieTrailerButton(trailer = trailer, colors = colors) }
    }
}

@Composable
private fun MovieTrailerButton(trailer: MovieTrailer, colors: DetailsColors, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    MovieChip(
        label = "Trailer",
        onClick = { uriHandler.openUri(trailer.youtubeLink) },
        modifier = modifier.height(movieChipHeight()),
        background = colors.chipBackground,
        contentColor = colors.chipText
    ) {
        Icon(
            painter = painterResource(Res.drawable.play),
            contentDescription = null,
            tint = colors.chipText,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
private fun MovieRatingItems(
    block: MovieBlock,
    details: MovieDetails,
    editor: MovieBlockEditor?,
    colors: DetailsColors,
    showsWatchState: Boolean
) {
    details.tmdbRating?.let { rating ->
        MovieRatingItem(label = "TMDB", colors = colors) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.star),
                    contentDescription = null,
                    tint = RatingStarColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = tmdbRatingText(rating), style = MaterialTheme.typography.titleLarge, color = colors.text)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "/ 10", style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
            }
        }
    }
    if (!showsWatchState) return
    MovieRatingItem(label = "Your rating", colors = colors) {
        StarRatingPicker(
            ratingHalfStars = block.ratingHalfStars,
            emptyColor = colors.emptyStar,
            onRate = editor?.let { { halfStars: Int? -> it.setRating(block, halfStars) } }
        )
    }
    MovieRatingItem(label = "Status", colors = colors) {
        MovieChip(
            label = block.watchStatus.label,
            onClick = editor?.let { { it.showNextWatchStatus(block) } },
            modifier = Modifier.height(movieChipHeight()),
            background = colors.chipBackground,
            contentColor = colors.chipText
        ) {
            Box(modifier = Modifier.size(8.dp).background(block.watchStatus.dotColor(), CircleShape))
        }
    }
}

@Composable
private fun MovieRatingItem(
    label: String,
    colors: DetailsColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
        content()
    }
}

@Composable
private fun StarRatingPicker(ratingHalfStars: Int?, emptyColor: Color, onRate: ((Int?) -> Unit)?) {
    val filledHalves = ratingHalfStars ?: 0
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(MAXIMUM_STAR_HALVES / 2) { starIndex ->
            val halvesInThisStar = (filledHalves - starIndex * 2).coerceIn(0, 2)
            Box(modifier = Modifier.size(28.dp)) {
                Icon(
                    painter = painterResource(Res.drawable.star),
                    contentDescription = null,
                    tint = emptyColor,
                    modifier = Modifier.fillMaxSize()
                )
                if (halvesInThisStar > 0) {
                    Icon(
                        painter = painterResource(Res.drawable.star),
                        contentDescription = null,
                        tint = RatingStarColor,
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                clipRect(right = size.width * halvesInThisStar / 2f) {
                                    this@drawWithContent.drawContent()
                                }
                            }
                    )
                }
                if (onRate != null) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        listOf(1, 2).forEach { half ->
                            val tappedHalfStars = starIndex * 2 + half
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .semantics { contentDescription = "Rate ${starsTextOf(tappedHalfStars)} stars" }
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { onRate(if (tappedHalfStars == ratingHalfStars) null else tappedHalfStars) }
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieDetailsSection(title: String, colors: DetailsColors, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = colors.text
        )
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieOverviewTab(details: MovieDetails, colors: DetailsColors) {
    val uriHandler = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        if (details.overview.isNotBlank()) {
            Text(text = details.overview, style = MaterialTheme.typography.bodyLarge, color = colors.text)
        }
        details.watchProviders?.takeIf { it.allProviders.isNotEmpty() }?.let { providers ->
            MovieWhereToWatch(
                providers = providers,
                colors = colors,
                onOpenWatchPage = providers.link.takeIf { it.isNotBlank() }?.let { link -> { uriHandler.openUri(link) } }
            )
        }
        val facts = factRowsOf(details)
        val logoRows = logoRowsOf(details)
        if (facts.isNotEmpty() || logoRows.isNotEmpty()) {
            MovieDetailsSection(title = "Details", colors = colors) {
                facts.forEach { (label, value) -> MovieFactRow(label = label, value = value, colors = colors) }
                logoRows.forEach { (label, companies) ->
                    MovieLabeledRow(
                        label = label,
                        labelWidth = FactLabelWidthDesktop,
                        labelHeight = CompanyChipHeight,
                        colors = colors
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            companies.forEach { company -> MovieCompanyChip(company = company, colors = colors) }
                        }
                    }
                }
            }
        }
        if (details.keywords.isNotEmpty()) {
            MovieDetailsSection(title = "Keywords", colors = colors) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    details.keywords.forEach { keyword -> MovieFactChip(text = keyword, colors = colors) }
                }
            }
        }
        val links = linksOf(details)
        if (links.isNotEmpty()) {
            MovieLinkChips(links = links, background = colors.chipBackground, contentColor = colors.chipText)
        }
    }
}

@Composable
internal fun MovieReadMoreText(text: String, color: Color, shortLines: Int) {
    var isExpanded by remember { mutableStateOf(false) }
    var isCutShort by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = color,
            maxLines = if (isExpanded) Int.MAX_VALUE else shortLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { layout -> if (!isExpanded) isCutShort = layout.hasVisualOverflow }
        )
        if (isCutShort) {
            Text(
                text = if (isExpanded) "Show less" else "Read more",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = color,
                modifier = Modifier
                    .clip(DefaultBlockShape)
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MovieLinkChips(
    links: List<Pair<String, String>>,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        links.forEach { (label, address) ->
            MovieChip(
                label = label,
                onClick = { uriHandler.openUri(address) },
                modifier = Modifier.height(movieChipHeight()),
                background = background,
                contentColor = contentColor
            ) {
                Icon(
                    painter = painterResource(Res.drawable.link),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieWhereToWatch(providers: MovieWatchProviders, colors: DetailsColors, onOpenWatchPage: (() -> Unit)?) {
    val groups = listOf("Stream" to providers.stream, "Rent" to providers.rent, "Buy" to providers.buy)
        .filter { (_, logos) -> logos.isNotEmpty() }
    MovieDetailsSection(title = "Where to watch", colors = colors) {
        groups.forEach { (label, logos) ->
            MovieLabeledRow(
                label = label,
                labelWidth = ProviderLabelWidthDesktop,
                labelHeight = ProviderTileSize,
                colors = colors
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    logos.forEach { provider -> MovieProviderTile(provider = provider, colors = colors, onClick = onOpenWatchPage) }
                }
            }
        }
        Text(text = "Data from JustWatch", style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
    }
}

@Composable
private fun MovieLabeledRow(
    label: String,
    labelWidth: Dp,
    labelHeight: Dp,
    colors: DetailsColors,
    content: @Composable () -> Unit
) {
    if (isDesktopPlatform) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(labelWidth).height(labelHeight), contentAlignment = Alignment.CenterStart) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge, color = colors.secondaryText)
            }
            Box(modifier = Modifier.weight(1f)) { content() }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = colors.secondaryText)
            content()
        }
    }
}

@Composable
private fun MovieProviderTile(provider: MovieNamedLogo, colors: DetailsColors, onClick: (() -> Unit)?) {
    IconTooltip(text = provider.name) {
        Box(
            modifier = Modifier
                .size(ProviderTileSize)
                .clip(LogoTileShape)
                .background(colors.chipBackground)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .semantics { contentDescription = provider.name },
            contentAlignment = Alignment.Center
        ) {
            Text(text = initialsOf(provider.name), style = MaterialTheme.typography.labelSmall, color = colors.text)
            if (provider.logoFilePath != null || provider.logoUrl != null) {
                MovieImage(filePath = provider.logoFilePath, url = provider.logoUrl, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun MovieCompanyChip(company: MovieNamedLogo, colors: DetailsColors) {
    val hasLogo = company.logoFilePath != null || company.logoUrl != null
    Row(
        modifier = Modifier
            .height(CompanyChipHeight)
            .clip(FactShape)
            .background(colors.chipBackground)
            .then(if (colors.chipBorder != null) Modifier.border(1.dp, colors.chipBorder, FactShape) else Modifier)
            .padding(start = if (hasLogo) 4.dp else 10.dp, end = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasLogo) {
            Box(
                modifier = Modifier
                    .size(width = CompanyLogoWidth, height = CompanyLogoHeight)
                    .clip(CompanyLogoShape)
                    .background(Color.White)
                    .padding(3.dp)
            ) {
                MovieImage(
                    filePath = company.logoFilePath,
                    url = company.logoUrl,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Text(
            text = company.name,
            style = MaterialTheme.typography.labelSmall,
            color = colors.chipText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MovieFactRow(label: String, value: String, colors: DetailsColors) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.secondaryText,
            modifier = Modifier.width(if (isDesktopPlatform) FactLabelWidthDesktop else FactLabelWidthPhone)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.text,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MovieCastGrid(cast: List<MovieCastMember>, colors: DetailsColors, onOpenPerson: (MovieCastMember) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val portraitMinWidth = if (isDesktopPlatform) CastPortraitMinWidthDesktop else CastPortraitMinWidthPhone
        val columnCount = ((maxWidth + CastColumnGap) / (portraitMinWidth + CastColumnGap)).toInt().coerceAtLeast(1)
        Column(verticalArrangement = Arrangement.spacedBy(CastRowGap)) {
            cast.chunked(columnCount).forEach { rowMembers ->
                Row(horizontalArrangement = Arrangement.spacedBy(CastColumnGap)) {
                    rowMembers.forEach { member ->
                        MovieCastPortrait(
                            member = member,
                            colors = colors,
                            onClick = member.personId?.let { { onOpenPerson(member) } },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columnCount - rowMembers.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieCastPortrait(
    member: MovieCastMember,
    colors: DetailsColors,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(DefaultBlockShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(DefaultBlockShape)
                .background(colors.avatarBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialsOf(member.name),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.text
            )
            if (member.photoFilePath != null || member.photoUrl != null) {
                MovieImage(filePath = member.photoFilePath, url = member.photoUrl, modifier = Modifier.fillMaxSize())
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colors.text,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (member.character.isNotBlank()) {
            Text(
                text = member.character,
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun headerFactsOf(details: MovieDetails): List<String> = listOfNotNull(
    details.kind.label,
    details.yearText,
    details.ageRating,
    details.lengthText(),
    details.episodeCount?.takeIf { details.kind == MovieKind.SHOW }?.let { episodes -> episodeCountText(episodes) },
    details.language
).filter { it.isNotBlank() }

private fun factRowsOf(details: MovieDetails): List<Pair<String, String>> {
    val isShow = details.kind == MovieKind.SHOW
    val lengthValue = if (isShow) {
        listOfNotNull(details.lengthText(), details.episodeCount?.let(::episodeCountText)).joinToString(", ")
    } else {
        details.lengthText().orEmpty()
    }
    return listOf(
        details.creditLabel to details.creditNames.joinToString(", "),
        (if (isShow) "First aired" else "Released") to details.releaseDate?.let(::releaseDateText).orEmpty(),
        (if (isShow) "Seasons" else "Runtime") to lengthValue,
        "Status" to details.productionStatus,
        "Genres" to details.genres.joinToString(", "),
        "Language" to details.language,
        "Country" to details.countries.joinToString(", "),
        "Budget" to details.budgetDollars?.let(::dollarsText).orEmpty(),
        "Box office" to details.revenueDollars?.let(::dollarsText).orEmpty(),
        "Collection" to details.collectionName
    ).filter { (label, value) -> label.isNotBlank() && value.isNotBlank() }
}

private fun logoRowsOf(details: MovieDetails): List<Pair<String, List<MovieNamedLogo>>> =
    listOf("Networks" to details.networks, "Companies" to details.productionCompanies)
        .filter { (_, companies) -> companies.isNotEmpty() }

private fun linksOf(details: MovieDetails): List<Pair<String, String>> = listOfNotNull(
    details.imdbId.takeIf { it.isNotBlank() }?.let { "IMDb" to "https://www.imdb.com/title/$it" },
    details.homepage.takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let { "Website" to it },
    details.instagramHandle.takeIf { it.isNotBlank() }?.let { "Instagram" to "https://www.instagram.com/$it" },
    details.facebookHandle.takeIf { it.isNotBlank() }?.let { "Facebook" to "https://www.facebook.com/$it" },
    details.xHandle.takeIf { it.isNotBlank() }?.let { "X" to "https://x.com/$it" }
)

private fun shownTabsOf(details: MovieDetails): List<MovieDetailsTab> {
    val hasOverview = details.overview.isNotBlank() ||
        details.watchProviders?.allProviders.orEmpty().isNotEmpty() ||
        factRowsOf(details).isNotEmpty() ||
        logoRowsOf(details).isNotEmpty() ||
        details.keywords.isNotEmpty() ||
        linksOf(details).isNotEmpty()
    return listOfNotNull(
        MovieDetailsTab.OVERVIEW.takeIf { hasOverview },
        MovieDetailsTab.CAST.takeIf { details.cast.isNotEmpty() },
        MovieDetailsTab.SEASONS.takeIf { hasSeasonsToShow(details) }
    )
}

private fun dollarsText(dollars: Long): String = when {
    dollars >= 1_000_000_000L -> "$${oneDecimalText(dollars / 1_000_000_000.0)}B"
    dollars >= 10_000_000L -> "$${(dollars / 1_000_000.0).roundToInt()}M"
    dollars >= 1_000_000L -> "$${oneDecimalText(dollars / 1_000_000.0)}M"
    dollars >= 1_000L -> "$${(dollars / 1_000.0).roundToInt()}K"
    else -> "$$dollars"
}

private fun oneDecimalText(value: Double): String = ((value * 10).roundToInt() / 10.0).toString()

internal fun episodeCountText(episodes: Int): String = if (episodes == 1) "1 episode" else "$episodes episodes"

internal fun releaseDateText(date: LocalDate): String {
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "${date.day} $month ${date.year}"
}

internal fun initialsOf(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
