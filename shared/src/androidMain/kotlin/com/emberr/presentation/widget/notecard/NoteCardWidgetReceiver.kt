// The manifest-registered receiver that hands NoteCardWidget to the Android widget framework.
package com.emberr.presentation.widget.notecard

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class NoteCardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NoteCardWidget()
}
