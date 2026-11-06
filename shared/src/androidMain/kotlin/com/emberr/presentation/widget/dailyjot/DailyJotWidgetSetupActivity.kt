// The setup screen for the jot widget, for choosing its background gradient.
package com.emberr.presentation.widget.dailyjot

import androidx.glance.appwidget.GlanceAppWidget

class DailyJotWidgetSetupActivity : GradientWidgetSetupActivity() {
    override fun widgetToRedraw(): GlanceAppWidget = DailyJotWidget()
}
