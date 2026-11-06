// The home screen widget itself: a gradient card that opens today's daily note ready to keep typing.
package com.emberr.presentation.widget.dailyjot

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.currentState
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.emberr.MainActivity
import com.emberr.R
import com.emberr.presentation.widget.widgetContinueDailyExtra
import com.emberr.presentation.widget.widgetCornerRadius
import com.emberr.presentation.widget.widgetDailyScreenExtra

private val onGradientTextColor = ColorProvider(Color.White)
private val buttonGlassColor = ColorProvider(Color(0x33FFFFFF))
private val ghostIconSize = 22.dp
private val dateOpticalInset = 2.dp
private val jotButtonHeight = 42.dp
private val penIconSize = 16.dp

class DailyJotWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val gradient = DailyJotGradient.named(currentState<Preferences>()[dailyJotGradientKey])
            DailyJotWidgetBody(context = context, gradient = gradient)
        }
    }
}

@Composable
private fun DailyJotWidgetBody(context: Context, gradient: DailyJotGradient) {
    val continueTypingAction = actionStartActivity(continueTodaysDailyIntent(context))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(
                imageProvider = ImageProvider(gradient.drawableResource),
                contentScale = ContentScale.FillBounds
            )
            .cornerRadius(widgetCornerRadius)
            .padding(horizontal = 18.dp, vertical = 18.dp)
            .clickable(continueTypingAction)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
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
                AndroidRemoteViews(remoteViews = RemoteViews(context.packageName, R.layout.widget_daily_jot_clock))
            }
        }

        Spacer(modifier = GlanceModifier.height(16.dp))

        Text(
            text = "What's on your mind?",
            maxLines = 2,
            style = TextStyle(
                color = onGradientTextColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.defaultWeight()
        )

        Spacer(modifier = GlanceModifier.height(12.dp))

        JotButton(onClick = continueTypingAction)
    }
}

@Composable
private fun JotButton(onClick: Action) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(jotButtonHeight)
            .background(buttonGlassColor)
            .cornerRadius(jotButtonHeight / 2)
            .clickable(onClick),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Image(
            provider = ImageProvider(R.drawable.ic_widget_pen),
            contentDescription = null,
            colorFilter = ColorFilter.tint(onGradientTextColor),
            modifier = GlanceModifier.size(penIconSize)
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        Text(
            text = "Jot it down",
            maxLines = 1,
            style = TextStyle(
                color = onGradientTextColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

private fun continueTodaysDailyIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .setData("emberr://daily/continue".toUri())
        .putExtra(widgetDailyScreenExtra, true)
        .putExtra(widgetContinueDailyExtra, true)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
