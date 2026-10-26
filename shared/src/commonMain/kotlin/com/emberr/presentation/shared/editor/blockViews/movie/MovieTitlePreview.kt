package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MoviePersonRole
import com.emberr.domain.model.titleKey
import com.emberr.domain.movie.MovieTitleSource
import org.koin.compose.koinInject

internal data class MovieTitleToPreview(
    val tmdbId: Long,
    val kind: MovieKind,
    val title: String,
    val yearText: String = "",
    val posterUrl: String? = null
)

internal fun MoviePersonRole.toTitleToPreview(): MovieTitleToPreview =
    MovieTitleToPreview(tmdbId = tmdbId, kind = kind, title = title, yearText = yearText, posterUrl = posterUrl)

@Composable
internal fun MovieTitlePreviewDialog(title: MovieTitleToPreview, onDismiss: () -> Unit) {
    val titleSource = koinInject<MovieTitleSource>()
    val liveDetails = rememberLiveTmdbLoad(title) { titleSource.loadDetails(title.tmdbId, title.kind, mayReuseRecentCopy = true) }
    val loadedDetails = liveDetails.loadedValue
    val firstDetails = remember(title) {
        MovieDetails(
            tmdbId = title.tmdbId,
            kind = title.kind,
            title = title.title,
            yearText = title.yearText,
            posterUrl = title.posterUrl
        )
    }
    val details = loadedDetails ?: firstDetails
    val previewBlock = remember(details) { MovieBlock(id = "preview-${details.titleKey()}", details = details) }
    MovieDetailsDialog(
        block = previewBlock,
        details = details,
        editor = null,
        onDismiss = onDismiss,
        isPreview = true,
        liveLoad = liveDetails.takeIf { loadedDetails == null }
    )
}
