// The small "Deleted" label that widgets show when the note or space behind them was deleted.
package com.emberr.presentation.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.emberr.R

@Composable
internal fun WidgetDeletedBadge() {
    Text(
        text = widgetText(R.string.widget_deleted),
        maxLines = 1,
        style = TextStyle(
            color = secondaryTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        ),
        modifier = GlanceModifier
            .background(elevatedSurfaceColor)
            .cornerRadius(8.dp)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
