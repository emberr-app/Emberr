package com.emberr.presentation.whatsnew

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrCheckbox
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.ui.theme.LocalAppIsDark
import com.emberr.ui.theme.availableColorThemes
import com.emberr.ui.theme.sourceColorSchemes
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private val ThemeShowDuration = 1400.milliseconds
private const val THEME_COLOR_FADE_MILLIS = 700
private val NoteCardShape = RoundedCornerShape(14.dp)
private val SwatchSize = 16.dp
private val SwatchOverlap = (-5).dp

@Composable
internal fun ThemesUpdatePreview() {
    val colorThemes = availableColorThemes()
    var shownThemeIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(colorThemes.size) {
        while (true) {
            delay(ThemeShowDuration)
            shownThemeIndex = (shownThemeIndex + 1) % colorThemes.size
        }
    }

    val shownTheme = colorThemes[shownThemeIndex.coerceAtMost(colorThemes.lastIndex)]
    val themeColors = shownTheme.colorSchemeFor(
        darkTheme = LocalAppIsDark.current,
        themedTextAndIcons = true,
        sourceColorSchemes = shownTheme.sourceColorSchemes()
    )
    val fadingColors = themeColors.copy(
        primary = fadingColor(themeColors.primary),
        secondary = fadingColor(themeColors.secondary),
        background = fadingColor(themeColors.background),
        onBackground = fadingColor(themeColors.onBackground),
        surface = fadingColor(themeColors.surface),
        onSurface = fadingColor(themeColors.onSurface),
        outline = fadingColor(themeColors.outline)
    )

    UpdatePreviewFrame {
        MaterialTheme(colorScheme = fadingColors) {
            ThemePreviewNote()
        }
        ThemeNameChip(
            themeName = shownTheme.displayName,
            swatchColors = listOf(fadingColors.background, fadingColors.surface, fadingColors.onSurface)
        )
    }
}

@Composable
private fun fadingColor(targetColor: Color): Color {
    val shownColor by animateColorAsState(targetColor, tween(THEME_COLOR_FADE_MILLIS), label = "theme_preview_color")
    return shownColor
}

@Composable
private fun ThemePreviewNote() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = if (isDesktopPlatform) 20.dp else 34.dp)
    ) {
        UpdatePreviewNoteTopButtons(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(18.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "Weekend trip",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Leave by 8am to beat the traffic.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            PreviewChecklistLine(text = "Train tickets", isChecked = true)
            PreviewChecklistLine(text = "Passport", isChecked = false)
            PreviewChecklistLine(text = "Camera charger", isChecked = false)
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(NoteCardShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Reading list",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "The Overstory, Piranesi, Project Hail Mary",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun PreviewChecklistLine(text: String, isChecked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EmberrCheckbox(checked = isChecked, onCheckedChange = null)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isChecked) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onBackground,
            textDecoration = if (isChecked) TextDecoration.LineThrough else null
        )
    }
}

@Composable
private fun BoxScope.ThemeNameChip(themeName: String, swatchColors: List<Color>) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(16.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .emberrBorder(CircleShape)
            .padding(start = 8.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(SwatchOverlap)) {
            swatchColors.forEach { swatchColor ->
                Box(
                    modifier = Modifier
                        .size(SwatchSize)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .emberrBorder(CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        AnimatedContent(
            targetState = themeName,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "theme_preview_name"
        ) { shownName ->
            Text(
                text = shownName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.widthIn(min = 110.dp)
            )
        }
    }
}
