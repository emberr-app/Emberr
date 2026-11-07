// The home screen widget itself: a compact month on the left and that month's events on the right.
package com.emberr.presentation.widget.calendaragenda

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
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
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.emberr.MainActivity
import com.emberr.R
import com.emberr.presentation.widget.WidgetColorTheme
import com.emberr.presentation.widget.WidgetTopFade
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.calendarEventUriScheme
import com.emberr.presentation.widget.widgetBirthdayPersonNoteIdExtra
import com.emberr.presentation.widget.highlightColor
import com.emberr.presentation.widget.onHighlightColor
import com.emberr.presentation.widget.WidgetLabelPill
import com.emberr.presentation.widget.elevatedSurfaceColor
import com.emberr.presentation.widget.primaryTextColor
import com.emberr.presentation.widget.footerSpaceItem
import com.emberr.presentation.widget.headerSpaceItem
import com.emberr.presentation.widget.widgetCornerRadius
import com.emberr.presentation.widget.secondaryTextColor
import com.emberr.presentation.widget.surfaceColor
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import androidx.core.graphics.toColorInt
import com.emberr.presentation.widget.previewCalendarAgendaContent

private val chevronSize = 18.dp
private val dayHighlightSize = 18.dp
private val eventDotSize = 8.dp
private val monthCardCornerRadius = 16.dp
private val agendaEdgePadding = 12.dp
private val agendaHeaderRowHeight = agendaEdgePadding + 28.dp
private val agendaListTopSpace = agendaHeaderRowHeight + 4.dp
private val headerHeight = 24.dp
private const val repeatingHeaderItemId = -1L
private const val otherEventsHeaderItemId = -2L

class CalendarAgendaWidget : GlanceAppWidget(), KoinComponent {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val shownMonth = readAgendaShownMonth(context, id)
        val content = loadAndCacheAgenda(context, id, shownMonth) ?: readCachedAgenda(context, id)
        val appWidgetId = resolveAppWidgetId(context, id)

        provideContent {
            WidgetColorTheme {
                val rawCachedContent = currentState<Preferences>()[cachedAgendaKey]
                val savedContent = remember(rawCachedContent) { decodeCachedAgenda(rawCachedContent) }

                CalendarAgendaWidgetBody(
                    context = context,
                    appWidgetId = appWidgetId,
                    content = savedContent ?: content
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            WidgetColorTheme {
                CalendarAgendaWidgetBody(
                    context = context,
                    appWidgetId = INVALID_APPWIDGET_ID,
                    content = previewCalendarAgendaContent()
                )
            }
        }
    }

    private suspend fun loadAndCacheAgenda(
        context: Context,
        id: GlanceId,
        shownMonth: String?
    ): CalendarAgendaWidgetContent? {
        val contentReader = runCatching { get<CalendarAgendaWidgetContentReader>() }.getOrNull() ?: return null
        val freshContent = contentReader.readContentOnce(shownMonth) ?: return null

        if (freshContent != readCachedAgenda(context, id)) {
            writeCachedAgenda(context, id, freshContent)
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
internal fun CalendarAgendaWidgetBody(
    context: Context,
    appWidgetId: Int,
    content: CalendarAgendaWidgetContent?
) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .cornerRadius(widgetCornerRadius)
            .padding(start = agendaEdgePadding)
    ) {
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .padding(top = agendaEdgePadding, bottom = agendaEdgePadding)
        ) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(elevatedSurfaceColor)
                    .cornerRadius(monthCardCornerRadius)
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                MonthGrid(content = content)
            }
        }

        Spacer(modifier = GlanceModifier.width(12.dp))

        Box(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            EventList(context = context, content = content)

            WidgetTopFade(height = agendaListTopSpace)

            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(agendaHeaderRowHeight)
                    .padding(top = agendaEdgePadding, end = agendaEdgePadding),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = content?.monthLabel.orEmpty(),
                    maxLines = 1,
                    style = TextStyle(
                        color = primaryTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight().padding(top = 4.dp)
                )

                Image(
                    provider = ImageProvider(R.drawable.ic_widget_chevron_left),
                    contentDescription = "Previous month",
                    colorFilter = ColorFilter.tint(secondaryTextColor),
                    modifier = GlanceModifier
                        .size(chevronSize)
                        .clickable(
                            actionSendBroadcast(agendaMonthStepIntent(context, appWidgetId, isForward = false))
                        )
                )

                Spacer(modifier = GlanceModifier.width(12.dp))

                Image(
                    provider = ImageProvider(R.drawable.ic_widget_chevron_right),
                    contentDescription = "Next month",
                    colorFilter = ColorFilter.tint(secondaryTextColor),
                    modifier = GlanceModifier
                        .size(chevronSize)
                        .clickable(
                            actionSendBroadcast(agendaMonthStepIntent(context, appWidgetId, isForward = true))
                        )
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.MonthGrid(content: CalendarAgendaWidgetContent?) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(headerHeight),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        content?.weekdayLabels.orEmpty().forEach { label ->
            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    maxLines = 1,
                    style = TextStyle(color = secondaryTextColor, fontSize = 10.sp)
                )
            }
        }
    }

    content?.weeks.orEmpty().forEach { week ->
        Row(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            week.forEach { cell ->
                Box(
                    modifier = GlanceModifier.defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    DayCell(cell = cell)
                }
            }
        }
    }
}

@Composable
private fun DayCell(cell: AgendaDayCell) {
    if (cell.dayNumber == null) {
        Spacer(modifier = GlanceModifier.size(dayHighlightSize))
        return
    }

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
                color = when {
                    cell.isToday -> onHighlightColor
                    cell.hasEvents -> primaryTextColor
                    else -> secondaryTextColor
                },
                fontSize = 11.sp,
                fontWeight = if (cell.isToday || cell.hasEvents) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun EventList(context: Context, content: CalendarAgendaWidgetContent?) {
    val events = content?.events.orEmpty()

    if (events.isEmpty()) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(top = agendaListTopSpace, end = agendaEdgePadding),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = if (content == null) "Open Emberr to load events" else "No events this month",
                maxLines = 2,
                style = TextStyle(color = secondaryTextColor, fontSize = 15.sp)
            )
        }
        return
    }

    val (repeatingEvents, otherEvents) = events.partition { event -> event.isRepeating }

    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        headerSpaceItem(height = agendaListTopSpace)
        if (repeatingEvents.isNotEmpty()) {
            item(itemId = repeatingHeaderItemId) { SectionHeader(label = "Repeating") }
            items(items = repeatingEvents, itemId = { event -> "repeating/${event.blockId}".hashCode().toLong() }) { event ->
                EventRow(context = context, event = event)
            }
            if (otherEvents.isNotEmpty()) {
                item(itemId = otherEventsHeaderItemId) { SectionHeader(label = "Events") }
            }
        }
        items(items = otherEvents, itemId = { event -> "${event.blockId}/${event.occurrenceDate}".hashCode().toLong() }) { event ->
            EventRow(context = context, event = event)
        }
        footerSpaceItem(height = agendaEdgePadding)
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        maxLines = 1,
        style = TextStyle(
            color = secondaryTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        ),
        modifier = GlanceModifier.padding(top = 4.dp, bottom = 4.dp, end = agendaEdgePadding)
    )
}

@Composable
private fun EventRow(context: Context, event: AgendaEvent) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 7.dp, end = agendaEdgePadding)
            .clickable(actionStartActivity(openEventIntent(context, event.blockId, event.birthdayPersonNoteId)))
    ) {
        Box(
            modifier = GlanceModifier
                .padding(top = 7.dp)
                .size(eventDotSize)
                .background(accentColorFrom(event.accentColorHex))
                .cornerRadius(eventDotSize / 2)
        ) {}

        Spacer(modifier = GlanceModifier.width(8.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = event.title,
                maxLines = 1,
                style = TextStyle(
                    color = if (event.isDone) secondaryTextColor else primaryTextColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (event.isDone) TextDecoration.LineThrough else TextDecoration.None
                )
            )

            Text(
                text = event.whenLabel,
                maxLines = 1,
                style = TextStyle(color = secondaryTextColor, fontSize = 12.sp)
            )
        }

        if (event.dayLabel != null) {
            Box(modifier = GlanceModifier.padding(start = 8.dp, top = 4.dp)) {
                WidgetLabelPill(text = event.dayLabel, background = elevatedSurfaceColor, textColor = primaryTextColor)
            }
        }
    }
}

private fun accentColorFrom(colorHex: String?): Color {
    val parsed = colorHex?.let { hex ->
        try {
            Color(hex.toColorInt())
        } catch (_: IllegalArgumentException) {
            null
        }
    }
    return parsed ?: Color(0xFF848484)
}

private fun agendaMonthStepIntent(context: Context, appWidgetId: Int, isForward: Boolean): Intent =
    Intent(context, CalendarAgendaWidgetActionReceiver::class.java).apply {
        action = if (isForward) showNextAgendaMonthAction else showPreviousAgendaMonthAction
        data = "emberr-agenda://step/$appWidgetId/$isForward".toUri()
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }

private fun openEventIntent(context: Context, blockId: String, birthdayPersonNoteId: String?): Intent =
    Intent(context, MainActivity::class.java)
        .setData("$calendarEventUriScheme://event/$blockId".toUri())
        .putExtra(widgetBirthdayPersonNoteIdExtra, birthdayPersonNoteId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
