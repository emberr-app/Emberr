package com.emberr.domain.selfhost.media

import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieCastMember
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieNamedLogo
import com.emberr.domain.model.MovieSeason
import com.emberr.domain.model.MovieWatchProviders
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.VoiceBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaReferenceScannerTest {

    @Test
    fun aNoteWithNoMediaReferencesNothing() {
        val blocks = listOf(TextBlock(id = "text-1", text = "just words"))

        assertTrue(MediaReferenceScanner.extractMediaFileNames(blocks).isEmpty())
    }

    @Test
    fun imagesDocumentsAndVoiceNotesAreAllCollected() {
        val blocks = listOf(
            ImageBlock(id = "image-1", localFilePath = "photo.png"),
            DocumentBlock(id = "document-1", localFilePath = "invoice.pdf"),
            VoiceBlock(id = "voice-1", localFilePath = "memo.m4a")
        )

        assertEquals(
            setOf("photo.png", "invoice.pdf", "memo.m4a"),
            MediaReferenceScanner.extractMediaFileNames(blocks)
        )
    }

    @Test
    fun onlyTheFileNameIsKeptAndNotTheFolderItSatIn() {
        val blocks = listOf(
            ImageBlock(id = "image-1", localFilePath = "/data/user/0/com.emberr/files/photo.png"),
            DocumentBlock(id = "document-1", localFilePath = "media/nested/invoice.pdf")
        )

        assertEquals(
            setOf("photo.png", "invoice.pdf"),
            MediaReferenceScanner.extractMediaFileNames(blocks)
        )
    }

    @Test
    fun aMovieBlocksPosterAndCastPhotosAreCollected() {
        val blocks = listOf(
            MovieBlock(
                id = "movie-1",
                details = MovieDetails(
                    tmdbId = 155,
                    kind = MovieKind.MOVIE,
                    title = "The Dark Knight",
                    posterUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
                    posterFilePath = "poster.jpg",
                    cast = listOf(
                        MovieCastMember(name = "Christian Bale", photoFilePath = "bale.jpg"),
                        MovieCastMember(name = "Aaron Eckhart", photoUrl = "https://image.tmdb.org/t/p/w185/eckhart.jpg")
                    )
                )
            )
        )

        assertEquals(setOf("poster.jpg", "bale.jpg"), MediaReferenceScanner.extractMediaFileNames(blocks))
    }

    @Test
    fun aShowBlocksLogosAndSeasonPostersAreCollected() {
        val blocks = listOf(
            MovieBlock(
                id = "show-1",
                details = MovieDetails(
                    tmdbId = 1396,
                    kind = MovieKind.SHOW,
                    title = "Breaking Bad",
                    productionCompanies = listOf(MovieNamedLogo("Sony Pictures Television", logoFilePath = "tmdb_sony.png")),
                    networks = listOf(MovieNamedLogo("AMC", logoFilePath = "tmdb_amc.png")),
                    seasons = listOf(MovieSeason(number = 1, posterFilePath = "tmdb_season1.jpg"), MovieSeason(number = 2)),
                    watchProviders = MovieWatchProviders(
                        stream = listOf(MovieNamedLogo("Netflix", logoFilePath = "tmdb_netflix.jpg")),
                        rent = listOf(MovieNamedLogo("Apple TV", logoFilePath = "tmdb_apple.jpg")),
                        buy = listOf(MovieNamedLogo("Google Play", logoFilePath = "tmdb_google.jpg"))
                    )
                )
            )
        )

        assertEquals(
            setOf(
                "tmdb_sony.png",
                "tmdb_amc.png",
                "tmdb_season1.jpg",
                "tmdb_netflix.jpg",
                "tmdb_apple.jpg",
                "tmdb_google.jpg"
            ),
            MediaReferenceScanner.extractMediaFileNames(blocks)
        )
    }

    @Test
    fun mediaBlocksWithNoFileYetAreIgnored() {
        val blocks = listOf(
            ImageBlock(id = "image-1", localFilePath = null),
            DocumentBlock(id = "document-1", localFilePath = null),
            VoiceBlock(id = "voice-1", localFilePath = null)
        )

        assertTrue(MediaReferenceScanner.extractMediaFileNames(blocks).isEmpty())
    }

    @Test
    fun deletedBlocksNoLongerCountAsUsingTheirMedia() {
        val blocks = listOf(
            ImageBlock(id = "image-1", localFilePath = "kept.png"),
            ImageBlock(id = "image-2", localFilePath = "removed.png", isDeleted = true)
        )

        assertEquals(setOf("kept.png"), MediaReferenceScanner.extractMediaFileNames(blocks))
    }

    @Test
    fun theSameFileUsedTwiceIsOnlyListedOnce() {
        val blocks = listOf(
            ImageBlock(id = "image-1", localFilePath = "shared.png"),
            ImageBlock(id = "image-2", localFilePath = "folder/shared.png")
        )

        assertEquals(setOf("shared.png"), MediaReferenceScanner.extractMediaFileNames(blocks))
    }
}
