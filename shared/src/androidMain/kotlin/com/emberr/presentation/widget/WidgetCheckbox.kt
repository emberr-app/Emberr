package com.emberr.presentation.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.emberr.R

private val checkboxIconSize = 24.dp

@Composable
fun WidgetCheckbox(
    isChecked: Boolean,
    modifier: GlanceModifier = GlanceModifier,
    checkedFillColor: ColorProvider = elevatedSurfaceColor,
    iconAlignment: Alignment = Alignment.CenterStart
) {
    Box(modifier = modifier, contentAlignment = iconAlignment) {
        if (isChecked) {
            Box {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_checkbox_filled),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(checkedFillColor),
                    modifier = GlanceModifier.size(checkboxIconSize)
                )
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_checkbox_tick),
                    contentDescription = "Done",
                    colorFilter = ColorFilter.tint(highlightColor),
                    modifier = GlanceModifier.size(checkboxIconSize)
                )
            }
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_checkbox_empty),
                contentDescription = "Not done",
                colorFilter = ColorFilter.tint(secondaryTextColor),
                modifier = GlanceModifier.size(checkboxIconSize)
            )
        }
    }
}
