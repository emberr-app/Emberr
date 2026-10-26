package com.emberr.presentation.shared.editor.blockViews.movie

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.MovieCastMember
import com.emberr.domain.model.MoviePerson
import com.emberr.domain.model.MoviePersonRole
import com.emberr.domain.movie.MovieTitleSource
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.editor.DefaultBlockShape
import org.koin.compose.koinInject

private val PersonSheetDesktopWidth = 560.dp
private val PersonSheetSidePadding = 20.dp
private val PersonSectionGap = 28.dp
private val PersonPhotoWidthDesktop = 112.dp
private val PersonPhotoWidthPhone = 104.dp
private val RolePosterMinWidthDesktop = 96.dp
private val RoleGap = 12.dp
private val RoleRowGap = 20.dp
private val RolePosterWidthPhone = 104.dp
private const val SHORT_BIOGRAPHY_LINES = 5

@Composable
internal fun MoviePersonSheet(member: MovieCastMember, personId: Long, onDismiss: () -> Unit) {
    val titleSource = koinInject<MovieTitleSource>()
    val livePerson = rememberLiveTmdbLoad(personId) { titleSource.loadPerson(personId) }
    var titleToPreview by remember { mutableStateOf<MovieTitleToPreview?>(null) }
    val person = livePerson.loadedValue

    EmberrBottomSheet(
        expanded = true,
        onDismiss = onDismiss,
        title = person?.name?.ifBlank { null } ?: member.name,
        contentHorizontalPadding = 0.dp,
        desktopMaxWidth = PersonSheetDesktopWidth
    ) { closeAnd ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(PersonSectionGap)
        ) {
            MoviePersonHeader(member = member, person = person, livePerson = livePerson)
            if (person != null) {
                if (person.biography.isNotBlank()) {
                    MoviePersonSection(title = "Biography") {
                        MovieReadMoreText(
                            text = person.biography,
                            color = MaterialTheme.colorScheme.onSurface,
                            shortLines = SHORT_BIOGRAPHY_LINES
                        )
                    }
                }
                if (person.roles.isNotEmpty()) {
                    MoviePersonSection(title = "Best known for", padsContent = false) {
                        MoviePersonRoles(roles = person.roles, onOpenRole = { role -> titleToPreview = role.toTitleToPreview() })
                    }
                }
                val links = personLinksOf(person)
                if (links.isNotEmpty()) {
                    MovieLinkChips(
                        links = links,
                        background = chipContainerColor(),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = PersonSheetSidePadding)
                    )
                }
            }
            if (!isDesktopPlatform) {
                EmberrButtonPrimary(
                    text = "Close",
                    onClick = { closeAnd { } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = PersonSheetSidePadding)
                )
            }
        }
    }

    titleToPreview?.let { title -> MovieTitlePreviewDialog(title = title, onDismiss = { titleToPreview = null }) }
}

@Composable
private fun MoviePersonHeader(member: MovieCastMember, person: MoviePerson?, livePerson: LiveTmdbLoadState<MoviePerson>) {
    val photoWidth = if (isDesktopPlatform) PersonPhotoWidthDesktop else PersonPhotoWidthPhone
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = PersonSheetSidePadding),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Box(
            modifier = Modifier
                .width(photoWidth)
                .aspectRatio(2f / 3f)
                .clip(DefaultBlockShape)
                .background(chipContainerColor()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initialsOf(member.name),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (member.photoFilePath != null || member.photoUrl != null) {
                MovieImage(filePath = member.photoFilePath, url = member.photoUrl, modifier = Modifier.fillMaxSize())
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (person == null) {
                LiveTmdbLoadMessage(
                    state = livePerson,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    chipBackground = chipContainerColor()
                )
            } else {
                personFactsOf(person).forEach { (label, value) -> MoviePersonFact(label = label, value = value) }
            }
        }
    }
}

@Composable
private fun MoviePersonFact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MoviePersonSection(title: String, padsContent: Boolean = true, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = PersonSheetSidePadding)
        )
        Box(modifier = if (padsContent) Modifier.padding(horizontal = PersonSheetSidePadding) else Modifier) {
            content()
        }
    }
}

@Composable
private fun MoviePersonRoles(roles: List<MoviePersonRole>, onOpenRole: (MoviePersonRole) -> Unit) {
    if (isDesktopPlatform) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = PersonSheetSidePadding)) {
            val columnCount = ((maxWidth + RoleGap) / (RolePosterMinWidthDesktop + RoleGap)).toInt().coerceAtLeast(1)
            val posterWidth = (maxWidth - RoleGap * (columnCount - 1)) / columnCount
            Column(verticalArrangement = Arrangement.spacedBy(RoleRowGap)) {
                roles.chunked(columnCount).forEach { rowRoles ->
                    Row(horizontalArrangement = Arrangement.spacedBy(RoleGap)) {
                        rowRoles.forEach { role -> MovieRolePoster(role = role, width = posterWidth, onClick = { onOpenRole(role) }) }
                    }
                }
            }
        }
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = PersonSheetSidePadding),
            horizontalArrangement = Arrangement.spacedBy(RoleGap)
        ) {
            items(roles, key = { role -> "${role.kind}:${role.tmdbId}" }) { role ->
                MovieRolePoster(role = role, width = RolePosterWidthPhone, onClick = { onOpenRole(role) })
            }
        }
    }
}

@Composable
private fun MovieRolePoster(role: MoviePersonRole, width: Dp, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(width)
            .clip(DefaultBlockShape)
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MoviePoster(filePath = null, url = role.posterUrl, width = width, cornerRadius = PosterCornerRadius)
        Text(
            text = role.title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val roleLine = listOf(role.roleName, role.yearText).filter { it.isNotBlank() }.joinToString(" · ")
        if (roleLine.isNotEmpty()) {
            Text(
                text = roleLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun personFactsOf(person: MoviePerson): List<Pair<String, String>> = listOfNotNull(
    person.knownFor.ifBlank { null }?.let { "Known for" to it },
    person.birthday?.let { "Born" to releaseDateText(it) },
    person.placeOfBirth.ifBlank { null }?.let { "Birthplace" to it },
    person.deathday?.let { "Died" to releaseDateText(it) }
)

private fun personLinksOf(person: MoviePerson): List<Pair<String, String>> = listOfNotNull(
    person.imdbId.takeIf { it.isNotBlank() }?.let { "IMDb" to "https://www.imdb.com/name/$it" },
    person.instagramHandle.takeIf { it.isNotBlank() }?.let { "Instagram" to "https://www.instagram.com/$it" },
    person.facebookHandle.takeIf { it.isNotBlank() }?.let { "Facebook" to "https://www.facebook.com/$it" },
    person.xHandle.takeIf { it.isNotBlank() }?.let { "X" to "https://x.com/$it" }
)
