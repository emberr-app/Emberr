// The manifest-registered receiver that hands DailyJotWidget to the Android widget framework.
package com.emberr.presentation.widget.dailyjot

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class DailyJotWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyJotWidget()
}
