// The home screen widget itself: draws a month grid with task dots and today highlighted.
package com.emberr.presentation.widget.calendar

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
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
import com.emberr.MainActivity
import com.emberr.R
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.highlightColor
import com.emberr.presentation.widget.onHighlightColor
import com.emberr.presentation.widget.elevatedSurfaceColor
import com.emberr.presentation.widget.primaryTextColor
import com.emberr.presentation.widget.widgetBottomPadding
import com.emberr.presentation.widget.widgetCornerRadius
import com.emberr.presentation.widget.widgetSidePadding
import com.emberr.presentation.widget.widgetTopPadding
import com.emberr.presentation.widget.secondaryTextColor
import com.emberr.presentation.widget.surfaceColor
import com.emberr.presentation.widget.calendarDateUriScheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import androidx.core.graphics.toColorInt

private val chevronSize = 16.dp
private val monthStepButtonSize = 32.dp
private val dayHighlightSize = 30.dp
private val dotSize = 6.dp

class CalendarWidget : GlanceAppWidget(), KoinComponent {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val shownMonth = readShownMonth(context, id)
        val content = loadAndCacheCalendar(context, id, shownMonth)
            ?: readCachedCalendar(context, id)
        val appWidgetId = resolveAppWidgetId(context, id)

        provideContent {
            CalendarWidgetBody(
                context = context,
                appWidgetId = appWidgetId,
                content = content
            )
        }
    }

    private suspend fun loadAndCacheCalendar(
        context: Context,
        id: GlanceId,
        shownMonth: String?
    ): CalendarWidgetContent? {
        val contentReader = runCatching { get<CalendarWidgetContentReader>() }.getOrNull() ?: return null
        val freshContent = contentReader.readContentOnce(shownMonth) ?: return null

        if (freshContent != readCachedCalendar(context, id)) {
            writeCachedCalendar(context, id, freshContent)
        }
        return freshContent
    }

    private fun resolveAppWidgetId(context: Context, id: GlanceId): Int =
        try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (cause: Exception) {
            WidgetLog.e("Could not resolve the widget id", cause)
            INVALID_APPWIDGET_ID
        }
}

@Composable
internal fun CalendarWidgetBody(
    context: Context,
    appWidgetId: Int,
    content: CalendarWidgetContent?
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .cornerRadius(widgetCornerRadius)
            .padding(horizontal = widgetSidePadding)
            .padding(top = widgetTopPadding, bottom = widgetBottomPadding)
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            MonthStepButton(
                iconResource = R.drawable.ic_widget_chevron_left,
                contentDescription = "Previous month",
                onClick = actionSendBroadcast(monthStepIntent(context, appWidgetId, isForward = false))
            )

            Row(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = content?.monthLabel.orEmpty(),
                    maxLines = 1,
                    style = TextStyle(
                        color = primaryTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                val yearLabel = content?.yearLabel.orEmpty()
                if (yearLabel.isNotBlank()) {
                    Spacer(modifier = GlanceModifier.width(6.dp))

                    Text(
                        text = yearLabel,
                        maxLines = 1,
                        style = TextStyle(color = secondaryTextColor, fontSize = 18.sp)
                    )
                }
            }

            MonthStepButton(
                iconResource = R.drawable.ic_widget_chevron_right,
                contentDescription = "Next month",
                onClick = actionSendBroadcast(monthStepIntent(context, appWidgetId, isForward = true))
            )
        }

        Spacer(modifier = GlanceModifier.height(20.dp))

        Row(modifier = GlanceModifier.fillMaxWidth()) {
            content?.weekdayLabels.orEmpty().forEach { label ->
                Box(
                    modifier = GlanceModifier.defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        maxLines = 1,
                        style = TextStyle(color = secondaryTextColor, fontSize = 12.sp)
                    )
                }
            }
        }

        Spacer(modifier = GlanceModifier.height(6.dp))

        content?.weeks.orEmpty().forEach { week ->
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                week.forEach { cell ->
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .then(
                                if (cell.dateString == null) {
                                    GlanceModifier
                                } else {
                                    GlanceModifier.clickable(
                                        actionStartActivity(openCalendarDayIntent(context, cell.dateString))
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        DayCell(cell = cell)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthStepButton(iconResource: Int, contentDescription: String, onClick: Action) {
    Box(
        modifier = GlanceModifier
            .size(monthStepButtonSize)
            .background(elevatedSurfaceColor)
            .cornerRadius(monthStepButtonSize / 2)
            .clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            provider = ImageProvider(iconResource),
            contentDescription = contentDescription,
            colorFilter = ColorFilter.tint(primaryTextColor),
            modifier = GlanceModifier.size(chevronSize)
        )
    }
}

@Composable
private fun DayCell(cell: CalendarDayCell) {
    if (cell.dayNumber == null) {
        Spacer(modifier = GlanceModifier.size(dayHighlightSize))
        return
    }

    Column(horizontalAlignment = Alignment.Horizontal.CenterHorizontally) {
        Box(
            modifier = GlanceModifier
                .size(dayHighlightSize)
                .then(
                    if (cell.isToday) {
                        GlanceModifier.background(highlightColor).cornerRadius(dayHighlightSize / 2)
                    } else {
                        GlanceModifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cell.dayNumber.toString(),
                maxLines = 1,
                style = TextStyle(
                    color = if (cell.isToday) onHighlightColor else primaryTextColor,
                    fontSize = 15.sp,
                    fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal
                )
            )
        }

        if (cell.dotColorHexes.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(4.dp))

            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                cell.dotColorHexes.forEachIndexed { dotIndex, colorHex ->
                    if (dotIndex > 0) {
                        Spacer(modifier = GlanceModifier.width(2.dp))
                    }
                    Box(
                        modifier = GlanceModifier
                            .size(dotSize)
                            .background(dotColorFrom(colorHex))
                            .cornerRadius(dotSize),
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }
        }
    }
}

private fun dotColorFrom(colorHex: String): Color =
    try {
        Color(colorHex.toColorInt())
    } catch (_: IllegalArgumentException) {
        Color(0xFF848484)
    }

private fun monthStepIntent(context: Context, appWidgetId: Int, isForward: Boolean): Intent =
    Intent(context, CalendarWidgetActionReceiver::class.java).apply {
        action = if (isForward) showNextMonthAction else showPreviousMonthAction
        data = "emberr-calendar://step/$appWidgetId/$isForward".toUri()
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }

private fun openCalendarDayIntent(context: Context, dateString: String): Intent =
    Intent(context, MainActivity::class.java)
        .setData("$calendarDateUriScheme://day/$dateString".toUri())
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
