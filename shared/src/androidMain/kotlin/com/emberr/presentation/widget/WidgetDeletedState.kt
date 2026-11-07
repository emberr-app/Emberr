package com.emberr.presentation.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

@Composable
internal fun WidgetDeletedState(
    headerText: String,
    headerStyle: TextStyle,
    message: String,
    messageFontSize: TextUnit,
    modifier: GlanceModifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = headerText,
                maxLines = 1,
                style = headerStyle,
                modifier = GlanceModifier.defaultWeight()
            )

            Spacer(modifier = GlanceModifier.width(8.dp))

            WidgetDeletedBadge()
        }

        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = message,
                style = TextStyle(color = secondaryTextColor, fontSize = messageFontSize)
            )
        }
    }
}
