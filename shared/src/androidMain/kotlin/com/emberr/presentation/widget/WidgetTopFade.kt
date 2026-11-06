// The gradient behind a floating title row, so list rows fade out as they scroll underneath it.
package com.emberr.presentation.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.unit.ColorProvider
import com.emberr.R

internal val widgetTopFadeSpread = 12.dp

@Composable
internal fun WidgetTopFade(height: Dp, fadeColor: ColorProvider = surfaceColor) {
    Image(
        provider = ImageProvider(R.drawable.ic_widget_top_fade),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        colorFilter = ColorFilter.tint(fadeColor),
        modifier = GlanceModifier.fillMaxWidth().height(height + widgetTopFadeSpread)
    )
}
