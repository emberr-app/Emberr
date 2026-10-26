package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.editor.DefaultBlockShape
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.util.system.isDesktopPlatform

private val GalleryCellMinWidthDesktop = 150.dp
private val GalleryCellMinWidthPhone = 110.dp
private val GalleryColumnGap = 16.dp
private val GalleryRowGap = 20.dp

@Composable
internal fun WatchlistGallery(
    titles: List<MovieBlock>,
    movieEditor: MovieBlockEditor,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    titleSelection: WatchlistSelection
) {
    val titleIdsInShownOrder = titles.map { it.id }
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val minimumCellWidth = if (isDesktopPlatform) GalleryCellMinWidthDesktop else GalleryCellMinWidthPhone
        val columnCount = maxOf(1, ((maxWidth + GalleryColumnGap) / (minimumCellWidth + GalleryColumnGap)).toInt())
        val cellWidth = (maxWidth - GalleryColumnGap * (columnCount - 1)) / columnCount

        Column(verticalArrangement = Arrangement.spacedBy(GalleryRowGap)) {
            titles.chunked(columnCount).forEach { rowOfTitles ->
                Row(horizontalArrangement = Arrangement.spacedBy(GalleryColumnGap)) {
                    rowOfTitles.forEach { title ->
                        val details = title.details ?: return@forEach
                        key(title.id) {
                            WatchlistGalleryCell(
                                title = title,
                                details = details,
                                width = cellWidth,
                                movieEditor = movieEditor,
                                inSelectionMode = inSelectionMode,
                                onToggleSelection = onToggleSelection,
                                selection = if (inSelectionMode) null else titleSelection.forTitle(title.id, titleIdsInShownOrder)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun WatchlistGalleryCell(
    title: MovieBlock,
    details: MovieDetails,
    width: Dp,
    movieEditor: MovieBlockEditor,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    selection: TitleCardSelection?
) {
    var isDetailsOpen by remember(title.id) { mutableStateOf(false) }

    if (isDetailsOpen) {
        MovieDetailsDialog(
            block = title,
            details = details,
            editor = movieEditor,
            onDismiss = { isDetailsOpen = false }
        )
    }

    Column(
        modifier = Modifier
            .width(width)
            .clip(DefaultBlockShape)
            .titleSelectionMouseClicks(selection)
            .combinedClickable(
                onClick = {
                    when {
                        inSelectionMode -> onToggleSelection()
                        selection?.isSelecting == true -> selection.onToggle()
                        else -> isDetailsOpen = true
                    }
                },
                onLongClick = selection?.let { { if (it.isSelecting) it.onToggle() else it.onStartSelecting() } }
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = if (selection?.isSelected == true) {
                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, DefaultBlockShape)
            } else {
                Modifier
            }
        ) {
            MoviePoster(filePath = details.posterFilePath, url = details.posterUrl, width = width, cornerRadius = PosterCornerRadius)
            if (selection?.isSelecting == true) {
                TitleSelectionCheckbox(
                    isSelected = selection.isSelected,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                )
            }
        }
        Column(
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = details.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (title.isWatched) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (title.isWatched) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (details.yearText.isNotBlank()) {
                    Text(
                        text = details.yearText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1
                    )
                }
                details.tmdbRating?.let { rating -> TmdbRating(rating = rating) }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GalleryStatus(title = title)
            }
        }
    }
}

@Composable
private fun GalleryStatus(title: MovieBlock) {
    Box(modifier = Modifier.size(7.dp).background(title.watchStatus.dotColor(), CircleShape))
    Text(
        text = title.watchStatus.label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
