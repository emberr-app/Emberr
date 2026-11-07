// The setup screen for the daily quote widget, for choosing its background gradient.
package com.emberr.presentation.widget.dailyquote

import androidx.glance.appwidget.GlanceAppWidget
import com.emberr.presentation.widget.gradient.GradientWidgetSetupActivity

class DailyQuoteWidgetSetupActivity : GradientWidgetSetupActivity() {
    override fun widgetToRedraw(): GlanceAppWidget = DailyQuoteWidget()
}
