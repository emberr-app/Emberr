package com.emberr.domain.movie

import com.emberr.domain.model.MovieDetails
import com.emberr.domain.util.media.WebImageFetcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

suspend fun MovieDetails.withImagesSavedBy(webImageFetcher: WebImageFetcher): MovieDetails = coroutineScope {
    val posterFile = async { posterUrl?.let { webImageFetcher.fetchIntoMedia(it)?.localFileName } }
    val castPhotoFiles = cast.map { member ->
        async { member.photoUrl?.let { webImageFetcher.fetchIntoMedia(it)?.localFileName } }
    }
    copy(
        posterFilePath = posterFile.await(),
        cast = cast.zip(castPhotoFiles.awaitAll()) { member, photoFile -> member.copy(photoFilePath = photoFile) }
    )
}
