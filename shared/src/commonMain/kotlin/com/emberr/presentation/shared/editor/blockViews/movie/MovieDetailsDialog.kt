package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieCastMember
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieNamedLogo
import com.emberr.domain.model.MovieTrailer
import com.emberr.domain.model.MovieTitleExtras
import com.emberr.domain.model.MovieWatchProviders
import com.emberr.domain.model.WatchStatus
import com.emberr.domain.model.allProviders
import com.emberr.domain.model.starsTextOf
import com.emberr.domain.model.titleKey
import com.emberr.domain.model.youtubeLink
import com.emberr.domain.movie.MovieTitleSource
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrTab
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.LocalDialogWindowTitleBar
import com.emberr.presentation.shared.components.customEmberrShadow
import com.emberr.presentation.shared.components.desktopDialogSize
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.fullScreenDialogProperties
import com.emberr.presentation.shared.editor.DefaultBlockShape
import com.emberr.ui.theme.LocalAppIsDark
import com.emberr.ui.theme.PaperTextureBox
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.check
import emberr.shared.generated.resources.link
import emberr.shared.generated.resources.logo_facebook
import emberr.shared.generated.resources.logo_imdb
import emberr.shared.generated.resources.logo_instagram
import emberr.shared.generated.resources.logo_x
import emberr.shared.generated.resources.logo_youtube
import emberr.shared.generated.resources.plus
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import emberr.shared.generated.resources.movie_add_to_watchlist
import emberr.shared.generated.resources.movie_adding
import emberr.shared.generated.resources.movie_data_from_justwatch
import emberr.shared.generated.resources.movie_dollars
import emberr.shared.generated.resources.movie_episode_count
import emberr.shared.generated.resources.movie_fact_box_office
import emberr.shared.generated.resources.movie_fact_budget
import emberr.shared.generated.resources.movie_fact_collection
import emberr.shared.generated.resources.movie_fact_companies
import emberr.shared.generated.resources.movie_fact_country
import emberr.shared.generated.resources.movie_fact_first_aired
import emberr.shared.generated.resources.movie_fact_genres
import emberr.shared.generated.resources.movie_fact_language
import emberr.shared.generated.resources.movie_fact_networks
import emberr.shared.generated.resources.movie_fact_released
import emberr.shared.generated.resources.movie_in_watchlist
import emberr.shared.generated.resources.movie_rate_stars
import emberr.shared.generated.resources.movie_read_more
import emberr.shared.generated.resources.movie_runtime
import emberr.shared.generated.resources.movie_seasons
import emberr.shared.generated.resources.movie_section_details
import emberr.shared.generated.resources.movie_section_keywords
import emberr.shared.generated.resources.movie_section_where_to_watch
import emberr.shared.generated.resources.movie_show_less
import emberr.shared.generated.resources.movie_status
import emberr.shared.generated.resources.movie_tab_cast
import emberr.shared.generated.resources.movie_tab_more_like_this
import emberr.shared.generated.resources.movie_tab_overview
import emberr.shared.generated.resources.movie_tab_reviews
import emberr.shared.generated.resources.movie_trailer
import emberr.shared.generated.resources.movie_watch_buy
import emberr.shared.generated.resources.movie_watch_rent
import emberr.shared.generated.resources.movie_watch_stream
import emberr.shared.generated.resources.movie_website
import emberr.shared.generated.resources.movie_your_rating
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.StringResource
import com.emberr.domain.util.format.shortNumberText
import com.emberr.domain.util.time.formatDate
import com.emberr.domain.util.time.DateTextStyle

private val DialogShape = RoundedCornerShape(24.dp)
private val FactShape = RoundedCornerShape(8.dp)
private val CastPortraitMinWidthDesktop = 140.dp
private val CastPortraitMinWidthPhone = 88.dp
private val CastColumnGap = 24.dp
private val CastRowGap = 28.dp
private val DesktopHorizontalPadding = 32.dp
private val DetailsSheetPadding = 20.dp
private val StatusBarHeight = 50.dp
private val StatusBarOuterPadding = 14.dp
private val StatusBarMaxWidth = 520.dp
private val SectionGap = 32.dp
private val TabContentGap = 20.dp
private val ScrollFadeWidth = 40.dp
private val FactLabelWidthDesktop = 140.dp
private val FactLabelWidthPhone = 112.dp
private val ProviderLabelWidthDesktop = 80.dp
private val ProviderTileSize = 40.dp
private val LogoTileShape = RoundedCornerShape(8.dp)
private val CompanyLogoShape = RoundedCornerShape(4.dp)
private val CompanyLogoWidth = 48.dp
private val CompanyLogoHeight = 24.dp
private val CompanyChipHeight = 32.dp
private const val MAXIMUM_STAR_HALVES = 10
private const val SKELETON_FACT_ROWS = 4

private enum class MovieDetailsTab(val labelResource: StringResource, val needsLiveExtras: Boolean = false) {
    OVERVIEW(Res.string.movie_tab_overview),
    CAST(Res.string.movie_tab_cast),
    SEASONS(Res.string.movie_seasons),
    MORE_LIKE_THIS(Res.string.movie_tab_more_like_this, needsLiveExtras = true),
    REVIEWS(Res.string.movie_tab_reviews, needsLiveExtras = true)
}

internal data class DetailsColors(
    val text: Color,
    val secondaryText: Color,
    val panel: Color,
    val chipBackground: Color
)

@Composable
internal fun detailsColors(): DetailsColors =
    DetailsColors(
        text = MaterialTheme.colorScheme.onSurface,
        secondaryText = MaterialTheme.colorScheme.outline,
        panel = if (LocalAppIsDark.current) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        chipBackground = chipContainerColor()
    )

@Composable
internal fun MovieDetailsDialog(
    block: MovieBlock,
    details: MovieDetails,
    editor: MovieBlockEditor?,
    onDismiss: () -> Unit,
    isPreview: Boolean = false,
    liveLoad: LiveTmdbLoadState<*>? = null
) {
    var openCastMember by remember { mutableStateOf<MovieCastMember?>(null) }
    var titleToPreview by remember { mutableStateOf<MovieTitleToPreview?>(null) }
    val showDetails: @Composable (onClose: () -> Unit, modifier: Modifier) -> Unit = { onClose, modifier ->
        MovieDetailsContent(
            block = block,
            details = details,
            editor = editor,
            isPreview = isPreview,
            liveLoad = liveLoad,
            onClose = onClose,
            onOpenPerson = { member -> openCastMember = member },
            onOpenTitle = { title -> titleToPreview = title },
            modifier = modifier
        )
    }

    val dialogWindowTitleBar = LocalDialogWindowTitleBar.current

    if (isDesktopPlatform) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = fullScreenDialogProperties()
        ) {
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
                dialogWindowTitleBar?.invoke()
                PaperTextureBox(
                    modifier = Modifier
                        .desktopDialogSize()
                        .safeDrawingPadding()
                        .clip(DialogShape)
                        .background(detailsBackgroundColor())
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    showDetails(onDismiss, Modifier.fillMaxSize())
                }
            }
            MovieDetailsPopups(
                openCastMember = openCastMember,
                titleToPreview = titleToPreview,
                onClosePerson = { openCastMember = null },
                onClosePreview = { titleToPreview = null }
            )
        }
    } else {
        EmberrBottomSheet(
            expanded = true,
            onDismiss = onDismiss,
            contentHorizontalPadding = 0.dp,
            scrollsContent = false
        ) { closeAnd ->
            showDetails({ closeAnd {} }, Modifier.fillMaxWidth())
        }
        MovieDetailsPopups(
            openCastMember = openCastMember,
            titleToPreview = titleToPreview,
            onClosePerson = { openCastMember = null },
            onClosePreview = { titleToPreview = null }
        )
    }
}

@Composable
private fun detailsBackgroundColor(): Color =
    if (LocalAppIsDark.current) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background

@Composable
private fun MovieDetailsContent(
    block: MovieBlock,
    details: MovieDetails,
    editor: MovieBlockEditor?,
    isPreview: Boolean,
    liveLoad: LiveTmdbLoadState<*>?,
    onClose: () -> Unit,
    onOpenPerson: (MovieCastMember) -> Unit,
    onOpenTitle: (MovieTitleToPreview) -> Unit,
    modifier: Modifier
) {
    val colors = detailsColors()
    val tabs = rememberMovieDetailsTabs(details, liveLoad)
    val watchlistAdder = if (isPreview) LocalWatchlistAdder.current else null
    val contentHazeState = remember { HazeState() }
    var statusBarHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val sidePadding = if (isDesktopPlatform) DesktopHorizontalPadding else DetailsSheetPadding

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .hazeSource(contentHazeState)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = sidePadding,
                    end = sidePadding,
                    top = sidePadding,
                    bottom = with(density) { statusBarHeightPx.toDp() } + sidePadding
                )
                .then(if (isPreview) Modifier.navigationBarsPadding() else Modifier),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            MovieDetailsTicket(
                details = details,
                facts = ticketFactsOf(details),
                background = colors.panel,
                onClose = onClose
            )
            if (!isPreview) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.movie_your_rating),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = colors.text,
                        modifier = Modifier.weight(1f)
                    )
                    StarRatingPicker(
                        ratingHalfStars = block.ratingHalfStars,
                        emptyColor = colors.chipBackground,
                        onRate = editor?.let { { halfStars: Int? -> it.setRating(block, halfStars) } }
                    )
                }
            }
            val trailer = details.trailer
            if (trailer != null || watchlistAdder != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    trailer?.let { MovieTrailerChip(trailer = it, colors = colors) }
                    watchlistAdder?.let { adder -> MovieAddToWatchlistButton(details = details, adder = adder, colors = colors) }
                }
            }
            MovieDetailsBody(
                details = details,
                colors = colors,
                liveLoad = liveLoad,
                tabs = tabs,
                onOpenPerson = onOpenPerson,
                onOpenTitle = onOpenTitle
            )
        }
        if (!isPreview) {
            WatchStatusBar(
                selectedStatus = block.watchStatus,
                colors = colors,
                hazeState = contentHazeState,
                onSelect = editor?.let { { status: WatchStatus -> it.setWatchStatus(block, status) } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { statusBarHeightPx = it.height }
                    .navigationBarsPadding()
                    .padding(StatusBarOuterPadding)
                    .widthIn(max = StatusBarMaxWidth)
            )
        }
    }
}

@Stable
private class MovieDetailsTabs(
    val shown: List<MovieDetailsTab>,
    val selected: MovieDetailsTab,
    val liveExtras: LiveTmdbLoadState<MovieTitleExtras>?,
    val onSelect: (MovieDetailsTab) -> Unit
)

@Composable
private fun rememberMovieDetailsTabs(details: MovieDetails, liveLoad: LiveTmdbLoadState<*>?): MovieDetailsTabs {
    var selectedTab by remember { mutableStateOf(MovieDetailsTab.OVERVIEW) }
    var hasOpenedLiveTab by remember { mutableStateOf(false) }
    val shownTabs = shownTabsOf(details)
    val shownTab = selectedTab.takeIf { it in shownTabs } ?: shownTabs.first()
    val titleSource = koinInject<MovieTitleSource>()
    val liveExtras = if (liveLoad == null && (hasOpenedLiveTab || shownTab.needsLiveExtras)) {
        rememberLiveTmdbLoad(details.titleKey()) { titleSource.loadTitleExtras(details.tmdbId, details.kind) }
    } else {
        null
    }
    return MovieDetailsTabs(
        shown = shownTabs,
        selected = shownTab,
        liveExtras = liveExtras,
        onSelect = { tab ->
            selectedTab = tab
            if (tab.needsLiveExtras) hasOpenedLiveTab = true
        }
    )
}

@Composable
private fun MovieDetailsBody(
    details: MovieDetails,
    colors: DetailsColors,
    liveLoad: LiveTmdbLoadState<*>?,
    tabs: MovieDetailsTabs,
    onOpenPerson: (MovieCastMember) -> Unit,
    onOpenTitle: (MovieTitleToPreview) -> Unit
) {
    val shownTab = tabs.selected
    if (liveLoad?.load == LiveTmdbLoad.Loading) {
        MovieOverviewSkeleton(colors = colors)
    } else if (liveLoad != null) {
        LiveTmdbLoadMessage(state = liveLoad, textColor = colors.text, chipBackground = colors.chipBackground)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(TabContentGap)) {
            MovieDetailsTabRow(
                tabs = tabs.shown,
                selectedTab = shownTab,
                contentColor = colors.text,
                selectedBackground = colors.chipBackground,
                onSelectTab = tabs.onSelect
            )
            when (shownTab) {
                MovieDetailsTab.OVERVIEW -> MovieOverviewTab(details = details, colors = colors)
                MovieDetailsTab.CAST -> MovieCastGrid(cast = details.cast, colors = colors, onOpenPerson = onOpenPerson)
                MovieDetailsTab.SEASONS -> MovieSeasonsTab(details = details, colors = colors)
                MovieDetailsTab.MORE_LIKE_THIS -> tabs.liveExtras?.let { extras ->
                    MovieMoreLikeThisTab(
                        liveExtras = extras,
                        colors = colors,
                        onOpenTitle = { result -> onOpenTitle(result.toTitleToPreview()) }
                    )
                }
                MovieDetailsTab.REVIEWS -> tabs.liveExtras?.let { extras ->
                    MovieReviewsTab(liveExtras = extras, colors = colors)
                }
            }
        }
    }
}

@Composable
private fun MovieDetailsPopups(
    openCastMember: MovieCastMember?,
    titleToPreview: MovieTitleToPreview?,
    onClosePerson: () -> Unit,
    onClosePreview: () -> Unit
) {
    openCastMember?.let { member ->
        member.personId?.let { personId ->
            MoviePersonSheet(member = member, personId = personId, onDismiss = onClosePerson)
        }
    }
    titleToPreview?.let { title -> MovieTitlePreviewDialog(title = title, onDismiss = onClosePreview) }
}

@Composable
private fun WatchStatusBar(
    selectedStatus: WatchStatus,
    colors: DetailsColors,
    hazeState: HazeState,
    onSelect: ((WatchStatus) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(StatusBarHeight)
            .emberrBorder(CircleShape)
            .customEmberrShadow(CircleShape)
            .clip(CircleShape)
            .emberrBlur(hazeState, EmberrBlur.Regular)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WatchStatus.entries.forEach { status ->
            val isSelected = status == selectedStatus
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(if (isSelected) colors.chipBackground else Color.Transparent)
                    .then(if (onSelect != null) Modifier.clickable { onSelect(status) } else Modifier)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).background(status.dotColor(), CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = status.displayName(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) colors.text else colors.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MovieTrailerChip(trailer: MovieTrailer, colors: DetailsColors) {
    val uriHandler = LocalUriHandler.current
    MovieChip(
        label = stringResource(Res.string.movie_trailer),
        onClick = { uriHandler.openUri(trailer.youtubeLink) },
        modifier = Modifier.height(movieChipHeight()),
        background = colors.chipBackground,
        contentColor = colors.text
    ) {
        Icon(
            painter = painterResource(Res.drawable.logo_youtube),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun MovieDetailsTabRow(
    tabs: List<MovieDetailsTab>,
    selectedTab: MovieDetailsTab,
    contentColor: Color,
    selectedBackground: Color,
    onSelectTab: (MovieDetailsTab) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fadesWhereItScrolls(scrollState)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tabs.forEach { tab ->
            EmberrTab(
                label = stringResource(tab.labelResource),
                isSelected = tab == selectedTab,
                contentColor = contentColor,
                selectedBackground = selectedBackground,
                onClick = { onSelectTab(tab) }
            )
        }
    }
}

internal fun Modifier.fadesWhereItScrolls(scrollState: ScrollState): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fadeFraction = (ScrollFadeWidth.toPx() / size.width).coerceAtMost(0.5f)
            val startColor = if (scrollState.canScrollBackward) Color.Transparent else Color.Black
            val endColor = if (scrollState.canScrollForward) Color.Transparent else Color.Black
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to startColor,
                    fadeFraction to Color.Black,
                    1f - fadeFraction to Color.Black,
                    1f to endColor
                ),
                blendMode = BlendMode.DstIn
            )
        }

@Composable
private fun MovieFactChip(text: String, colors: DetailsColors) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = colors.text,
        maxLines = 1,
        modifier = Modifier
            .clip(FactShape)
            .background(colors.chipBackground)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun MovieAddToWatchlistButton(
    details: MovieDetails,
    adder: WatchlistAdder,
    colors: DetailsColors,
    modifier: Modifier = Modifier
) {
    val isInWatchlist = details.titleKey() in adder.titleKeysInWatchlist
    val label = when {
        isInWatchlist -> stringResource(Res.string.movie_in_watchlist)
        adder.isAdding -> stringResource(Res.string.movie_adding)
        else -> stringResource(Res.string.movie_add_to_watchlist)
    }
    MovieChip(
        label = label,
        onClick = if (isInWatchlist || adder.isAdding) null else ({ adder.add(details.toSearchResult()) }),
        modifier = modifier.height(movieChipHeight()),
        background = colors.chipBackground,
        contentColor = colors.text
    ) {
        Icon(
            painter = painterResource(if (isInWatchlist) Res.drawable.check else Res.drawable.plus),
            contentDescription = null,
            tint = colors.text,
            modifier = Modifier.size(12.dp)
        )
    }
}

@Composable
internal fun StarRatingPicker(ratingHalfStars: Int?, emptyColor: Color, onRate: ((Int?) -> Unit)?) {
    val filledHalves = ratingHalfStars ?: 0
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(MAXIMUM_STAR_HALVES / 2) { starIndex ->
            val halvesInThisStar = (filledHalves - starIndex * 2).coerceIn(0, 2)
            Box(modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = emptyColor,
                    modifier = Modifier.fillMaxSize()
                )
                if (halvesInThisStar > 0) {
                    Icon(
                        imageVector = Icons.Filled.Star,
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
                            val rateDescription = stringResource(Res.string.movie_rate_stars, starsTextOf(tappedHalfStars))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .semantics { contentDescription = rateDescription }
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
        val links = linksOf(details)
        if (links.isNotEmpty()) {
            MovieLinkChips(links = links, background = colors.chipBackground, contentColor = colors.text)
        }
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
            MovieDetailsSection(title = stringResource(Res.string.movie_section_details), colors = colors) {
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
            MovieDetailsSection(title = stringResource(Res.string.movie_section_keywords), colors = colors) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    details.keywords.forEach { keyword -> MovieFactChip(text = keyword, colors = colors) }
                }
            }
        }
    }
}

@Composable
private fun MovieOverviewSkeleton(colors: DetailsColors) {
    val progress = rememberShimmerProgress()
    val shimmerColors = shimmerColorsOf(colors.chipBackground, colors.text)
    val chipHeight = movieChipHeight()
    Column(verticalArrangement = Arrangement.spacedBy(SectionGap)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(76.dp, 52.dp, 64.dp, 92.dp).forEachIndexed { index, tabWidth ->
                Box(
                    modifier = Modifier
                        .size(width = tabWidth, height = chipHeight)
                        .clip(FactShape)
                        .shimmer(progress, shimmerColors, itemIndex = index)
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(1f, 1f, 1f, 0.95f, 0.6f).forEachIndexed { index, widthFraction ->
                MovieSkeletonLine(widthFraction = widthFraction, height = 12.dp, progress = progress, colors = shimmerColors, itemIndex = index)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MovieSkeletonLine(widthFraction = 0.25f, height = 14.dp, progress = progress, colors = shimmerColors)
            repeat(SKELETON_FACT_ROWS) { index ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.width(factLabelWidth()).padding(end = 24.dp)) {
                        MovieSkeletonLine(widthFraction = 1f, height = 12.dp, progress = progress, colors = shimmerColors, itemIndex = index)
                    }
                    MovieSkeletonLine(widthFraction = 0.6f, height = 12.dp, progress = progress, colors = shimmerColors, itemIndex = index)
                }
            }
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
                text = if (isExpanded) stringResource(Res.string.movie_show_less) else stringResource(Res.string.movie_read_more),
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

@Composable
internal fun MovieLinkChips(
    links: List<MovieLink>,
    background: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .fadesWhereItScrolls(scrollState)
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        links.forEach { link ->
            MovieChip(
                label = link.label,
                onClick = { uriHandler.openUri(link.address) },
                modifier = Modifier.height(movieChipHeight()),
                background = background,
                contentColor = contentColor
            ) {
                Icon(
                    painter = painterResource(link.icon),
                    contentDescription = null,
                    tint = if (link.keepsIconColors) Color.Unspecified else contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

internal class MovieLink(
    val label: String,
    val address: String,
    val icon: DrawableResource,
    val keepsIconColors: Boolean = true
)

internal fun imdbLinkOf(imdbPath: String): MovieLink =
    MovieLink(label = "IMDb", address = "https://www.imdb.com/$imdbPath", icon = Res.drawable.logo_imdb)

internal fun socialLinksOf(instagramHandle: String, facebookHandle: String, xHandle: String): List<MovieLink> = listOfNotNull(
    instagramHandle.takeIf { it.isNotBlank() }?.let {
        MovieLink(label = "Instagram", address = "https://www.instagram.com/$it", icon = Res.drawable.logo_instagram)
    },
    facebookHandle.takeIf { it.isNotBlank() }?.let {
        MovieLink(label = "Facebook", address = "https://www.facebook.com/$it", icon = Res.drawable.logo_facebook)
    },
    xHandle.takeIf { it.isNotBlank() }?.let {
        MovieLink(label = "X", address = "https://x.com/$it", icon = Res.drawable.logo_x, keepsIconColors = false)
    }
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieWhereToWatch(providers: MovieWatchProviders, colors: DetailsColors, onOpenWatchPage: (() -> Unit)?) {
    val groups = listOf(
        stringResource(Res.string.movie_watch_stream) to providers.stream,
        stringResource(Res.string.movie_watch_rent) to providers.rent,
        stringResource(Res.string.movie_watch_buy) to providers.buy
    )
        .filter { (_, logos) -> logos.isNotEmpty() }
    MovieDetailsSection(title = stringResource(Res.string.movie_section_where_to_watch), colors = colors) {
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
        Text(text = stringResource(Res.string.movie_data_from_justwatch), style = MaterialTheme.typography.labelSmall, color = colors.secondaryText)
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
            color = colors.text,
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
            modifier = Modifier.width(factLabelWidth())
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.text,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun factLabelWidth(): Dp = if (isDesktopPlatform) FactLabelWidthDesktop else FactLabelWidthPhone

@Composable
private fun MovieCastGrid(cast: List<MovieCastMember>, colors: DetailsColors, onOpenPerson: (MovieCastMember) -> Unit) {
    MovieAdaptiveGrid(
        items = cast,
        minimumCellWidth = if (isDesktopPlatform) CastPortraitMinWidthDesktop else CastPortraitMinWidthPhone,
        columnGap = CastColumnGap,
        rowGap = CastRowGap
    ) { member, portraitWidth ->
        MovieCastPortrait(
            member = member,
            colors = colors,
            onClick = member.personId?.let { { onOpenPerson(member) } },
            modifier = Modifier.width(portraitWidth)
        )
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
        MoviePersonPhoto(member = member, colors = colors, modifier = Modifier.fillMaxWidth())
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

@Composable
internal fun MoviePersonPhoto(member: MovieCastMember, colors: DetailsColors, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(PosterAspectRatio)
            .clip(DefaultBlockShape)
            .background(colors.chipBackground),
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
}

@Composable
private fun ticketFactsOf(details: MovieDetails): List<String> = listOfNotNull(
    details.ageRating,
    details.lengthText(),
    details.episodeCount?.takeIf { details.kind == MovieKind.SHOW }?.let { episodes -> episodeCountText(episodes) },
    details.language
).filter { it.isNotBlank() }

@Composable
private fun factRowsOf(details: MovieDetails): List<Pair<String, String>> {
    val isShow = details.kind == MovieKind.SHOW
    val lengthValue = if (isShow) {
        listOfNotNull(details.lengthText(), details.episodeCount?.let { episodeCountText(it) }).joinToString(", ")
    } else {
        details.lengthText().orEmpty()
    }
    return listOf(
        details.creditLabel to details.creditNames.joinToString(", "),
        (if (isShow) stringResource(Res.string.movie_fact_first_aired) else stringResource(Res.string.movie_fact_released)) to
            details.releaseDate?.let(::releaseDateText).orEmpty(),
        (if (isShow) stringResource(Res.string.movie_seasons) else stringResource(Res.string.movie_runtime)) to lengthValue,
        stringResource(Res.string.movie_status) to details.productionStatus,
        stringResource(Res.string.movie_fact_genres) to details.genres.joinToString(", "),
        stringResource(Res.string.movie_fact_language) to details.language,
        stringResource(Res.string.movie_fact_country) to details.countries.joinToString(", "),
        stringResource(Res.string.movie_fact_budget) to details.budgetDollars?.let { dollarsText(it) }.orEmpty(),
        stringResource(Res.string.movie_fact_box_office) to details.revenueDollars?.let { dollarsText(it) }.orEmpty(),
        stringResource(Res.string.movie_fact_collection) to details.collectionName
    ).filter { (label, value) -> label.isNotBlank() && value.isNotBlank() }
}

@Composable
private fun logoRowsOf(details: MovieDetails): List<Pair<String, List<MovieNamedLogo>>> =
    listOf(
        stringResource(Res.string.movie_fact_networks) to details.networks,
        stringResource(Res.string.movie_fact_companies) to details.productionCompanies
    )
        .filter { (_, companies) -> companies.isNotEmpty() }

@Composable
private fun linksOf(details: MovieDetails): List<MovieLink> = listOfNotNull(
    details.imdbId.takeIf { it.isNotBlank() }?.let { imdbLinkOf("title/$it") },
    details.homepage.takeIf { it.startsWith("https://") || it.startsWith("http://") }?.let {
        MovieLink(label = stringResource(Res.string.movie_website), address = it, icon = Res.drawable.link, keepsIconColors = false)
    }
) + socialLinksOf(details.instagramHandle, details.facebookHandle, details.xHandle)

@Composable
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
        MovieDetailsTab.SEASONS.takeIf { hasSeasonsToShow(details) },
        MovieDetailsTab.MORE_LIKE_THIS,
        MovieDetailsTab.REVIEWS
    )
}

@Composable
private fun dollarsText(dollars: Long): String = stringResource(Res.string.movie_dollars, shortNumberText(dollars))

@Composable
internal fun episodeCountText(episodes: Int): String = pluralStringResource(Res.plurals.movie_episode_count, episodes, episodes)

internal fun releaseDateText(date: LocalDate): String = formatDate(date, DateTextStyle.SHORT_MONTH_DAY_YEAR)

internal fun initialsOf(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
