package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.ContentScale
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
import coil3.compose.AsyncImage
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieCastMember
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.starsTextOf
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.EmberrShadowElevation
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.fullScreenDialogProperties
import com.emberr.presentation.shared.editor.DefaultBlockShape
import com.emberr.ui.theme.LocalAppIsDark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.star
import emberr.shared.generated.resources.star_outline
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource

private val DialogShape = RoundedCornerShape(24.dp)
private val FactShape = RoundedCornerShape(8.dp)
private val CastPhotoSize = 56.dp
private val CastColumnWidth = 80.dp
private val SectionGap = 32.dp
private val DialogScrim = Brush.verticalGradient(
    0f to Color.Black.copy(alpha = 0.5f),
    0.35f to Color.Black.copy(alpha = 0.7f),
    1f to Color.Black.copy(alpha = 0.85f)
)
private const val MAXIMUM_STAR_HALVES = 10

private data class DetailsColors(
    val text: Color,
    val secondaryText: Color,
    val panel: Color,
    val chipBackground: Color,
    val chipText: Color,
    val chipBorder: Color?,
    val starOutline: Color,
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
            starOutline = Color.White.copy(alpha = 0.5f),
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
            starOutline = MaterialTheme.colorScheme.outline,
            avatarBackground = chipContainerColor()
        )
    }

@Composable
internal fun MovieDetailsDialog(
    block: MovieBlock,
    details: MovieDetails,
    editor: MovieBlockEditor?,
    onDismiss: () -> Unit
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
        val posterUrl = details.posterUrl
        val colors = detailsColors(isOnPoster = posterUrl != null)
        val horizontalPadding = if (isDesktopPlatform) 32.dp else 24.dp
        val hazeState = remember { HazeState() }

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
                    .background(if (posterUrl != null) PosterBackdropBaseColor else dialogBackgroundColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Box(modifier = Modifier.matchParentSize().hazeSource(hazeState)) {
                    if (posterUrl != null) {
                        BlurredPosterBackdrop(imageUrl = posterUrl, blurRadius = 60.dp, scrim = DialogScrim)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(start = horizontalPadding, end = horizontalPadding, top = 72.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(SectionGap)
                    ) {
                        MovieDetailsHeader(block = block, details = details, editor = editor, colors = colors)
                        if (!isDesktopPlatform) {
                            MovieRatingPanel(block = block, details = details, editor = editor, colors = colors)
                        }
                        if (details.overview.isNotBlank()) {
                            MovieDetailsSection(title = "Overview", colors = colors) {
                                Text(
                                    text = details.overview,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.text
                                )
                            }
                        }
                        val facts = factRowsOf(details)
                        if (facts.isNotEmpty()) {
                            MovieDetailsSection(title = "Details", colors = colors) {
                                facts.forEach { (label, value) -> MovieFactRow(label = label, value = value, colors = colors) }
                            }
                        }
                        if (details.cast.isNotEmpty()) {
                            MovieDetailsSection(title = "Cast", colors = colors) {
                                MovieCastRow(cast = details.cast, colors = colors)
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
                        hazeStyle = if (posterUrl != null) EmberrBlur.OnImage else EmberrBlur.Regular,
                        shadowElevation = EmberrShadowElevation.None,
                        tooltip = "Close",
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieDetailsHeader(block: MovieBlock, details: MovieDetails, editor: MovieBlockEditor?, colors: DetailsColors) {
    val posterUrl = details.posterUrl
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
            Box(modifier = if (posterUrl != null) Modifier.shadow(16.dp, RoundedCornerShape(10.dp)) else Modifier) {
                MoviePoster(imageUrl = posterUrl, width = posterWidth, cornerRadius = 10.dp)
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
                        MovieRatingItems(block = block, details = details, editor = editor, colors = colors)
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
    colors: DetailsColors
) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .clip(DefaultBlockShape)
            .background(colors.panel)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        MovieRatingItems(block = block, details = details, editor = editor, colors = colors)
    }
}

@Composable
private fun MovieRatingItems(
    block: MovieBlock,
    details: MovieDetails,
    editor: MovieBlockEditor?,
    colors: DetailsColors
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
    MovieRatingItem(label = "Your rating", colors = colors) {
        StarRatingPicker(
            ratingHalfStars = block.ratingHalfStars,
            outlineColor = colors.starOutline,
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
private fun StarRatingPicker(ratingHalfStars: Int?, outlineColor: Color, onRate: ((Int?) -> Unit)?) {
    val filledHalves = ratingHalfStars ?: 0
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(MAXIMUM_STAR_HALVES / 2) { starIndex ->
            val halvesInThisStar = (filledHalves - starIndex * 2).coerceIn(0, 2)
            Box(modifier = Modifier.size(28.dp)) {
                Icon(
                    painter = painterResource(Res.drawable.star_outline),
                    contentDescription = null,
                    tint = outlineColor,
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

@Composable
private fun MovieFactRow(label: String, value: String, colors: DetailsColors) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.secondaryText,
            modifier = Modifier.width(if (isDesktopPlatform) 140.dp else 112.dp)
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
private fun MovieCastRow(cast: List<MovieCastMember>, colors: DetailsColors) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        cast.forEach { member ->
            Column(
                modifier = Modifier.width(CastColumnWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(CastPhotoSize)
                        .clip(CircleShape)
                        .background(colors.avatarBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialsOf(member.name),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = colors.text
                    )
                    if (member.photoUrl != null) {
                        AsyncImage(
                            model = member.photoUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.labelSmall,
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
        "Genres" to details.genres.joinToString(", "),
        "Language" to details.language,
        "Country" to details.countries.joinToString(", ")
    ).filter { (label, value) -> label.isNotBlank() && value.isNotBlank() }
}

private fun episodeCountText(episodes: Int): String = if (episodes == 1) "1 episode" else "$episodes episodes"

private fun releaseDateText(date: LocalDate): String {
    val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "${date.day} $month ${date.year}"
}

private fun initialsOf(name: String): String =
    name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
