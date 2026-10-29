package com.emberr.domain.movie

import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieNamedLogo
import com.emberr.domain.model.allProviders
import com.emberr.domain.util.media.WebImageFetcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private const val MOST_DOWNLOADS_AT_ONCE = 6

suspend fun MovieDetails.withImagesSavedBy(webImageFetcher: WebImageFetcher): MovieDetails {
    val logos = productionCompanies + networks + watchProviders?.allProviders.orEmpty()
    val imageUrls = listOfNotNull(posterUrl) +
        cast.mapNotNull { it.photoUrl } +
        seasons.mapNotNull { it.posterUrl } +
        logos.mapNotNull { it.logoUrl }
    val savedFileNameByUrl = saveImagesIntoMedia(imageUrls.distinct(), webImageFetcher)
    fun savedFileNameOf(url: String?): String? = url?.let { savedFileNameByUrl[it] }
    fun List<MovieNamedLogo>.withSavedLogos() = map { it.copy(logoFilePath = savedFileNameOf(it.logoUrl)) }

    return copy(
        posterFilePath = savedFileNameOf(posterUrl),
        cast = cast.map { it.copy(photoFilePath = savedFileNameOf(it.photoUrl)) },
        seasons = seasons.map { it.copy(posterFilePath = savedFileNameOf(it.posterUrl)) },
        productionCompanies = productionCompanies.withSavedLogos(),
        networks = networks.withSavedLogos(),
        watchProviders = watchProviders?.let { providers ->
            providers.copy(
                stream = providers.stream.withSavedLogos(),
                rent = providers.rent.withSavedLogos(),
                buy = providers.buy.withSavedLogos()
            )
        }
    )
}

private suspend fun saveImagesIntoMedia(urls: List<String>, webImageFetcher: WebImageFetcher): Map<String, String> =
    coroutineScope {
        val downloadSlots = Semaphore(MOST_DOWNLOADS_AT_ONCE)
        urls.map { url ->
            async {
                downloadSlots.withPermit { webImageFetcher.fetchIntoMedia(url) }
                    ?.let { savedImage -> url to savedImage.localFileName }
            }
        }.awaitAll().filterNotNull().toMap()
    }
