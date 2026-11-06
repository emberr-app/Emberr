// The pieces every gradient card widget shares: its background, the ghost and date row, its text color and its tap action.
package com.emberr.presentation.widget.dailyjot

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.emberr.MainActivity
import com.emberr.R
import com.emberr.presentation.widget.widgetContinueDailyExtra
import com.emberr.presentation.widget.widgetCornerRadius
import com.emberr.presentation.widget.widgetDailyScreenExtra

internal val onGradientTextColor = ColorProvider(Color.White)
internal val gradientCardPadding = 18.dp
private val ghostIconSize = 22.dp
private val dateOpticalInset = 2.dp

internal fun GlanceModifier.gradientCardBackground(gradient: DailyJotGradient): GlanceModifier =
    this
        .background(
            imageProvider = ImageProvider(gradient.drawableResource),
            contentScale = ContentScale.FillBounds
        )
        .cornerRadius(widgetCornerRadius)

@Composable
internal fun GradientCardHeader(
    context: Context,
    dateContent: (@Composable () -> Unit)? = null,
    extraSideInset: Dp = 0.dp
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(horizontal = extraSideInset),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_ghost),
            contentDescription = null,
            colorFilter = ColorFilter.tint(onGradientTextColor),
            modifier = GlanceModifier.size(ghostIconSize)
        )

        Box(
            modifier = GlanceModifier.defaultWeight().padding(end = dateOpticalInset),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (dateContent != null) {
                dateContent()
            } else {
                AndroidRemoteViews(remoteViews = RemoteViews(context.packageName, R.layout.widget_daily_jot_clock))
            }
        }
    }
}

internal fun continueTodaysDailyIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .setData("emberr://daily/continue".toUri())
        .putExtra(widgetDailyScreenExtra, true)
        .putExtra(widgetContinueDailyExtra, true)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
