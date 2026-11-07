// The setup screen for the jot widget, for choosing its background gradient.
package com.emberr.presentation.widget.dailyjot

import androidx.glance.appwidget.GlanceAppWidget
import com.emberr.presentation.widget.gradient.GradientWidgetSetupActivity

class DailyJotWidgetSetupActivity : GradientWidgetSetupActivity() {
    override fun widgetToRedraw(): GlanceAppWidget = DailyJotWidget()
}
