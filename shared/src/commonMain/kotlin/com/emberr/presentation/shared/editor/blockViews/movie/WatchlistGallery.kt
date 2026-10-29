package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalFoundationApi::class)
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

    val clickModifier = Modifier
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
        )

    GalleryTicketCard(
        title = title,
        details = details,
        width = width,
        selection = selection,
        onStatusClick = {
            when {
                inSelectionMode -> onToggleSelection()
                selection?.isSelecting == true -> selection.onToggle()
                else -> movieEditor.showNextWatchStatus(title)
            }
        },
        clickModifier = clickModifier
    )
}
