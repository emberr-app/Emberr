// The manifest-registered receiver that hands DailyQuoteWidget to the Android widget framework.
package com.emberr.presentation.widget.dailyquote

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class DailyQuoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyQuoteWidget()
}
