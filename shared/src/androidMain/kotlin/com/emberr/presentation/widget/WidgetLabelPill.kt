// The small rounded label used for times and day names at the end of widget rows.
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
import androidx.glance.unit.ColorProvider

@Composable
internal fun WidgetLabelPill(
    text: String,
    background: ColorProvider,
    textColor: ColorProvider = secondaryTextColor,
    isEmphasized: Boolean = false
) {
    Text(
        text = text,
        maxLines = 1,
        style = TextStyle(
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isEmphasized) FontWeight.Medium else FontWeight.Normal
        ),
        modifier = GlanceModifier
            .background(background)
            .cornerRadius(10.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
