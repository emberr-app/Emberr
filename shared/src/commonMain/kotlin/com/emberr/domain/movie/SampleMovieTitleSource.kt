package com.emberr.domain.movie

import com.emberr.domain.model.MovieCastMember
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.MovieSearchResult
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate

private const val SAMPLE_SEARCH_DELAY_MILLIS = 150L
private const val SAMPLE_DETAILS_DELAY_MILLIS = 600L
private const val SUGGESTION_COUNT = 5
private const val RESULT_COUNT = 6
private const val SAMPLE_IMAGE_ADDRESS = "https://picsum.photos/seed"
private val titlesShownWithoutPoster = setOf(129L, 70523L)

class SampleMovieTitleSource : MovieTitleSource {

    override val canSearch: Boolean = true

    override suspend fun search(query: String): List<MovieSearchResult> {
        delay(SAMPLE_SEARCH_DELAY_MILLIS)
        val trimmedQuery = query.trim().lowercase()
        val matchingTitles = if (trimmedQuery.isEmpty()) {
            sampleTitles.take(SUGGESTION_COUNT)
        } else {
            sampleTitles
                .map { details -> details to matchScoreOf(details, trimmedQuery) }
                .filter { (_, score) -> score > 0 }
                .sortedByDescending { (_, score) -> score }
                .take(RESULT_COUNT)
                .map { (details, _) -> details }
        }
        return matchingTitles.map { details ->
            MovieSearchResult(
                tmdbId = details.tmdbId,
                kind = details.kind,
                title = details.title,
                yearText = details.yearText,
                tmdbRating = details.tmdbRating,
                posterUrl = details.posterUrl
            )
        }
    }

    override suspend fun loadDetails(tmdbId: Long, kind: MovieKind): MovieDetails? {
        delay(SAMPLE_DETAILS_DELAY_MILLIS)
        return sampleTitles
            .firstOrNull { it.tmdbId == tmdbId && it.kind == kind }
            ?.copy(refreshedAt = System.currentTimeMillis())
    }

    private fun matchScoreOf(details: MovieDetails, query: String): Int {
        val title = details.title.lowercase()
        return when {
            title.startsWith(query) -> 4
            title.split(' ', ':').any { word -> word.startsWith(query) } -> 3
            title.contains(query) -> 2
            details.yearText.startsWith(query) -> 1
            else -> 0
        }
    }
}

private fun castOf(vararg names: String): List<MovieCastMember> = names.map { MovieCastMember(name = it) }

private fun MovieDetails.withSampleImages(): MovieDetails {
    if (tmdbId in titlesShownWithoutPoster) return this
    return copy(
        posterUrl = "$SAMPLE_IMAGE_ADDRESS/emberr-$tmdbId/400/600",
        cast = cast.map { member ->
            member.copy(photoUrl = "$SAMPLE_IMAGE_ADDRESS/emberr-${member.name.filter { it in 'a'..'z' || it in 'A'..'Z' }}/200/200")
        }
    )
}

private val sampleTitles = listOf(
    MovieDetails(
        tmdbId = 157336,
        kind = MovieKind.MOVIE,
        title = "Interstellar",
        yearText = "2014",
        tagline = "The way home runs through the stars.",
        overview = "With Earth's crops failing, a former pilot leaves his children behind to lead a crew through a wormhole near Saturn, searching for a planet humanity can call home, while time moves differently for the people he left.",
        runtimeMinutes = 169,
        ageRating = "PG-13",
        tmdbRating = 8.7,
        genres = listOf("Sci-Fi", "Drama", "Adventure"),
        creditLabel = "Director",
        creditNames = listOf("Christopher Nolan"),
        language = "English",
        countries = listOf("United States", "United Kingdom"),
        releaseDate = LocalDate(2014, 11, 7),
        cast = listOf(
            MovieCastMember(name = "Matthew McConaughey", character = "Cooper"),
            MovieCastMember(name = "Anne Hathaway", character = "Brand"),
            MovieCastMember(name = "Jessica Chastain", character = "Murph"),
            MovieCastMember(name = "Michael Caine", character = "Professor Brand"),
            MovieCastMember(name = "Mackenzie Foy", character = "Young Murph")
        )
    ),
    MovieDetails(
        tmdbId = 27205,
        kind = MovieKind.MOVIE,
        title = "Inception",
        yearText = "2010",
        tagline = "The safest place to hide is a dream.",
        overview = "A thief who steals secrets from inside people's dreams is offered one last job: planting an idea instead of taking one, several dreams deep.",
        runtimeMinutes = 148,
        ageRating = "PG-13",
        tmdbRating = 8.8,
        genres = listOf("Sci-Fi", "Action", "Thriller"),
        creditLabel = "Director",
        creditNames = listOf("Christopher Nolan"),
        language = "English",
        countries = listOf("United States", "United Kingdom"),
        releaseDate = LocalDate(2010, 7, 16),
        cast = castOf("Leonardo DiCaprio", "Joseph Gordon-Levitt", "Elliot Page", "Tom Hardy", "Marion Cotillard")
    ),
    MovieDetails(
        tmdbId = 155,
        kind = MovieKind.MOVIE,
        title = "The Dark Knight",
        yearText = "2008",
        tagline = "Every city gets the hero it can stand.",
        overview = "Batman, Lieutenant Gordon and District Attorney Harvey Dent push back against Gotham's mobs, until a criminal who wants only chaos forces each of them to decide what they're willing to lose.",
        runtimeMinutes = 152,
        ageRating = "PG-13",
        tmdbRating = 9.0,
        genres = listOf("Action", "Crime", "Drama"),
        creditLabel = "Director",
        creditNames = listOf("Christopher Nolan"),
        language = "English",
        countries = listOf("United States", "United Kingdom"),
        releaseDate = LocalDate(2008, 7, 18),
        cast = castOf("Christian Bale", "Heath Ledger", "Aaron Eckhart", "Gary Oldman", "Maggie Gyllenhaal")
    ),
    MovieDetails(
        tmdbId = 496243,
        kind = MovieKind.MOVIE,
        title = "Parasite",
        yearText = "2019",
        tagline = "Every house has a basement.",
        overview = "A struggling family talks its way, one job at a time, into the household of a wealthy one, until a secret beneath the house upends both.",
        runtimeMinutes = 132,
        ageRating = "R",
        tmdbRating = 8.5,
        genres = listOf("Thriller", "Drama", "Comedy"),
        creditLabel = "Director",
        creditNames = listOf("Bong Joon Ho"),
        language = "Korean",
        countries = listOf("South Korea"),
        releaseDate = LocalDate(2019, 5, 30),
        cast = castOf("Song Kang-ho", "Lee Sun-kyun", "Cho Yeo-jeong", "Choi Woo-shik", "Park So-dam")
    ),
    MovieDetails(
        tmdbId = 693134,
        kind = MovieKind.MOVIE,
        title = "Dune: Part Two",
        yearText = "2024",
        tagline = "The desert remembers its own.",
        overview = "Paul Atreides joins the Fremen of Arrakis to avenge his family, while visions of a holy war force him to weigh the woman he loves against the fate of the known universe.",
        runtimeMinutes = 166,
        ageRating = "PG-13",
        tmdbRating = 8.5,
        genres = listOf("Sci-Fi", "Adventure", "Drama"),
        creditLabel = "Director",
        creditNames = listOf("Denis Villeneuve"),
        language = "English",
        countries = listOf("United States", "Canada"),
        releaseDate = LocalDate(2024, 3, 1),
        cast = castOf("Timothée Chalamet", "Zendaya", "Rebecca Ferguson", "Javier Bardem", "Austin Butler")
    ),
    MovieDetails(
        tmdbId = 872585,
        kind = MovieKind.MOVIE,
        title = "Oppenheimer",
        yearText = "2023",
        tagline = "One man's work changed the world for good.",
        overview = "J. Robert Oppenheimer leads the Manhattan Project at Los Alamos, then faces a security hearing years later that turns his achievement against him.",
        runtimeMinutes = 180,
        ageRating = "R",
        tmdbRating = 8.3,
        genres = listOf("Biography", "Drama", "History"),
        creditLabel = "Director",
        creditNames = listOf("Christopher Nolan"),
        language = "English",
        countries = listOf("United States", "United Kingdom"),
        releaseDate = LocalDate(2023, 7, 21),
        cast = castOf("Cillian Murphy", "Emily Blunt", "Matt Damon", "Robert Downey Jr.", "Florence Pugh")
    ),
    MovieDetails(
        tmdbId = 129,
        kind = MovieKind.MOVIE,
        title = "Spirited Away",
        yearText = "2001",
        overview = "Ten-year-old Chihiro wanders into a world of spirits, where she takes a job at a witch's bathhouse to free her parents, who have been turned into pigs.",
        runtimeMinutes = 125,
        ageRating = "PG",
        tmdbRating = 8.6,
        genres = listOf("Animation", "Fantasy", "Adventure"),
        creditLabel = "Director",
        creditNames = listOf("Hayao Miyazaki"),
        language = "Japanese",
        countries = listOf("Japan"),
        releaseDate = LocalDate(2001, 7, 20),
        cast = castOf("Rumi Hiiragi", "Miyu Irino", "Mari Natsuki", "Takashi Naitō", "Yasuko Sawaguchi")
    ),
    MovieDetails(
        tmdbId = 20453,
        kind = MovieKind.MOVIE,
        title = "3 Idiots",
        yearText = "2009",
        tagline = "Chase excellence, and success will follow.",
        overview = "Two friends set out to find the college roommate who vanished after graduation, remembering how he challenged their rote-learning engineering school at every turn.",
        tmdbRating = 8.4,
        genres = listOf("Comedy", "Drama"),
        creditLabel = "Director",
        creditNames = listOf("Rajkumar Hirani"),
        language = "Hindi",
        countries = listOf("India"),
        releaseDate = LocalDate(2009, 12, 25),
        cast = castOf("Aamir Khan", "R. Madhavan", "Sharman Joshi", "Kareena Kapoor", "Boman Irani")
    ),
    MovieDetails(
        tmdbId = 1396,
        kind = MovieKind.SHOW,
        title = "Breaking Bad",
        yearText = "2008–2013",
        tagline = "Chemistry is the study of change.",
        overview = "A high-school chemistry teacher with a terminal diagnosis starts cooking meth to secure his family's future, and slowly becomes someone else entirely.",
        seasonCount = 5,
        episodeCount = 62,
        ageRating = "TV-MA",
        tmdbRating = 9.5,
        genres = listOf("Crime", "Drama", "Thriller"),
        creditLabel = "Created by",
        creditNames = listOf("Vince Gilligan"),
        language = "English",
        countries = listOf("United States"),
        releaseDate = LocalDate(2008, 1, 20),
        cast = castOf("Bryan Cranston", "Aaron Paul", "Anna Gunn", "Dean Norris", "Bob Odenkirk")
    ),
    MovieDetails(
        tmdbId = 95396,
        kind = MovieKind.SHOW,
        title = "Severance",
        yearText = "2022–",
        tagline = "Work stays at work. Literally.",
        overview = "Office workers at Lumon Industries have had their memories surgically split between work and home, until one team starts asking what their jobs really are.",
        seasonCount = 2,
        episodeCount = 19,
        ageRating = "TV-MA",
        tmdbRating = 8.7,
        genres = listOf("Sci-Fi", "Drama", "Mystery"),
        creditLabel = "Created by",
        creditNames = listOf("Dan Erickson"),
        language = "English",
        countries = listOf("United States"),
        releaseDate = LocalDate(2022, 2, 18),
        cast = castOf("Adam Scott", "Britt Lower", "Zach Cherry", "John Turturro", "Patricia Arquette")
    ),
    MovieDetails(
        tmdbId = 136315,
        kind = MovieKind.SHOW,
        title = "The Bear",
        yearText = "2022–",
        tagline = "Every second counts in this kitchen.",
        overview = "A fine-dining chef comes home to Chicago to run his late brother's chaotic sandwich shop, trying to rebuild the kitchen and himself along with it.",
        seasonCount = 4,
        episodeCount = 38,
        ageRating = "TV-MA",
        tmdbRating = 8.5,
        genres = listOf("Drama", "Comedy"),
        creditLabel = "Created by",
        creditNames = listOf("Christopher Storer"),
        language = "English",
        countries = listOf("United States"),
        releaseDate = LocalDate(2022, 6, 23),
        cast = castOf("Jeremy Allen White", "Ayo Edebiri", "Ebon Moss-Bachrach", "Abby Elliott", "Lionel Boyce")
    ),
    MovieDetails(
        tmdbId = 70523,
        kind = MovieKind.SHOW,
        title = "Dark",
        yearText = "2017–2020",
        tagline = "The question isn't where, but when.",
        overview = "When a child goes missing in a small German town, four families uncover a time-travel mystery that stretches across several generations.",
        ageRating = "TV-MA",
        tmdbRating = 8.7,
        genres = listOf("Sci-Fi", "Mystery", "Thriller"),
        creditLabel = "Created by",
        creditNames = listOf("Baran bo Odar", "Jantje Friese"),
        language = "German",
        countries = listOf("Germany"),
        releaseDate = LocalDate(2017, 12, 1),
        cast = castOf("Louis Hofmann", "Lisa Vicari", "Oliver Masucci", "Jördis Triebel", "Maja Schöne")
    )
).map { it.withSampleImages() }
