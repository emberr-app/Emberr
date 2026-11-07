// The colours every home screen widget shares so they all match the app's look.
package com.emberr.presentation.widget

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.emberr.ui.theme.HighlightColor

private const val LIGHT_SEPARATOR_ALPHA = 0.17f
private const val DARK_SEPARATOR_ALPHA = 0.23f

@Composable
private fun themeColor(pickColor: ColorScheme.() -> Color): ColorProvider {
    val colorSchemes = LocalWidgetColorSchemes.current
    return ColorProvider(
        day = colorSchemes.light.pickColor(),
        night = colorSchemes.dark.pickColor()
    )
}

internal val surfaceColor: ColorProvider
    @Composable get() = themeColor { background }
internal val primaryTextColor: ColorProvider
    @Composable get() = themeColor { onBackground }
internal val secondaryTextColor: ColorProvider
    @Composable get() = themeColor { outline }
internal val separatorColor: ColorProvider
    @Composable get() {
        val colorSchemes = LocalWidgetColorSchemes.current
        return ColorProvider(
            day = colorSchemes.light.onBackground.copy(alpha = LIGHT_SEPARATOR_ALPHA),
            night = colorSchemes.dark.onBackground.copy(alpha = DARK_SEPARATOR_ALPHA)
        )
    }
internal val elevatedSurfaceColor: ColorProvider
    @Composable get() = themeColor { surface }
internal val highlightColor: ColorProvider
    @Composable get() = themeColor { primary }
internal val onHighlightColor: ColorProvider
    @Composable get() = themeColor { onPrimary }
internal fun highlightedTextBackgroundColor(colorName: String?): ColorProvider {
    val highlightColor = HighlightColor.named(colorName)
    return ColorProvider(day = highlightColor.lightBackground, night = highlightColor.darkBackground)
}
