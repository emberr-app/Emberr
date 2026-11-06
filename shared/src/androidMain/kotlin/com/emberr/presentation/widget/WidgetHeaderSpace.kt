// The empty rows that keep a widget's list clear of its floating title row and its bottom edge.
package com.emberr.presentation.widget

import androidx.compose.ui.unit.Dp
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.lazy.LazyListScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.height

private const val headerSpaceItemId = -(1L shl 40)
private const val footerSpaceItemId = headerSpaceItemId - 1

internal fun LazyListScope.headerSpaceItem(height: Dp = widgetListTopSpace) {
    item(itemId = headerSpaceItemId) {
        Spacer(modifier = GlanceModifier.height(height))
    }
}

internal fun LazyListScope.footerSpaceItem(height: Dp = widgetBottomPadding) {
    item(itemId = footerSpaceItemId) {
        Spacer(modifier = GlanceModifier.height(height))
    }
}
