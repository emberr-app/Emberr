// The round filled "+" button that sits at the right of a widget's header.
package com.emberr.presentation.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.size
import com.emberr.R

private val addButtonSize = 32.dp
private val addButtonIconSize = 18.dp

@Composable
internal fun WidgetAddButton(contentDescription: String, onClick: Action) {
    Box(
        modifier = GlanceModifier
            .size(addButtonSize)
            .background(highlightColor)
            .cornerRadius(addButtonSize / 2)
            .clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_plus),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(onHighlightColor),
            modifier = GlanceModifier.size(addButtonIconSize)
        )
    }
}
