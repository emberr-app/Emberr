package com.emberr.presentation.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.presentation.people.personAvatarColors
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.user2
import okio.Path.Companion.toPath
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

private const val InitialsSizeRatio = 0.37f
private const val EmojiSizeRatio = 0.52f
private const val BlankNameIconSizeRatio = 0.45f
private const val BlankNameIconAlpha = 0.7f
private const val MorePeopleTextSizeRatio = 0.4f

fun personInitials(name: String): String {
    val wordInitials = name.trim()
        .split(Regex("\\s+"))
        .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
    if (wordInitials.isEmpty()) return "?"
    val firstInitial = wordInitials.first().toString()
    val lastInitial = if (wordInitials.size > 1) wordInitials.last().toString() else ""
    return (firstInitial + lastInitial).uppercase()
}

@Composable
fun PersonAvatar(
    personNoteId: String,
    name: String,
    icon: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    photoPath: String? = null
) {
    val colors = personAvatarColors(personNoteId)
    val density = LocalDensity.current
    val mediaStorageHelper = koinInject<MediaStorageHelper>()
    val localPhotoPath = remember(photoPath) { photoPath?.let { mediaStorageHelper.getAbsoluteMediaPath(it).toPath() } }
    var photoFailedToLoad by remember(localPhotoPath) { mutableStateOf(false) }
    val showsPhoto = localPhotoPath != null && !photoFailedToLoad
    val emoji = icon?.takeIf { it.isNotBlank() && !showsPhoto }
    val background = if (emoji != null) {
        Modifier.background(colors.emojiBackground)
    } else {
        Modifier.background(Brush.linearGradient(listOf(colors.gradientStart, colors.gradientEnd)))
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(background),
        contentAlignment = Alignment.Center
    ) {
        when {
            showsPhoto -> AsyncImage(
                model = localPhotoPath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onError = { photoFailedToLoad = true },
                modifier = Modifier.fillMaxSize()
            )
            emoji != null -> {
                val emojiSize = with(density) { (size * EmojiSizeRatio).toSp() }
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = emojiSize, lineHeight = emojiSize),
                    maxLines = 1,
                    softWrap = false
                )
            }
            name.isBlank() -> Icon(
                painter = painterResource(Res.drawable.user2),
                contentDescription = null,
                tint = colors.initials.copy(alpha = BlankNameIconAlpha),
                modifier = Modifier.size(size * BlankNameIconSizeRatio)
            )
            else -> {
                val initialsSize = with(density) { (size * InitialsSizeRatio).toSp() }
                Text(
                    text = personInitials(name),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = initialsSize, lineHeight = initialsSize),
                    fontWeight = FontWeight.Bold,
                    color = colors.initials,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun MorePeopleAvatar(hiddenCount: Int, size: Dp, modifier: Modifier = Modifier) {
    val fontSize = with(LocalDensity.current) { (size * MorePeopleTextSizeRatio).toSp() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+$hiddenCount",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = fontSize, lineHeight = fontSize),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}
