package com.emberr.presentation.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp

private const val InitialsSizeRatio = 0.4f

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
fun PersonAvatar(name: String, size: Dp, modifier: Modifier = Modifier) {
    AvatarCircle(text = personInitials(name), size = size, modifier = modifier)
}

@Composable
fun MorePeopleAvatar(hiddenCount: Int, size: Dp, modifier: Modifier = Modifier) {
    AvatarCircle(text = "+$hiddenCount", size = size, modifier = modifier)
}

@Composable
private fun AvatarCircle(text: String, size: Dp, modifier: Modifier) {
    val fontSize = with(LocalDensity.current) { (size * InitialsSizeRatio).toSp() }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = fontSize, lineHeight = fontSize),
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false
        )
    }
}
