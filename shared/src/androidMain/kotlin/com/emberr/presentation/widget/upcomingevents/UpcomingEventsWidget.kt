// The home screen widget itself: a scrollable, date-grouped list of every upcoming event.
package com.emberr.presentation.widget.upcomingevents

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.emberr.MainActivity
import com.emberr.presentation.widget.WidgetTopFade
import com.emberr.presentation.widget.calendarEventUriScheme
import com.emberr.presentation.widget.widgetBirthdayPersonNoteIdExtra
import com.emberr.presentation.widget.WidgetAddButton
import com.emberr.presentation.widget.elevatedSurfaceColor
import com.emberr.presentation.widget.primaryTextColor
import com.emberr.presentation.widget.widgetCornerRadius
import com.emberr.presentation.widget.widgetSidePadding
import com.emberr.presentation.widget.widgetTopPadding
import com.emberr.presentation.widget.footerSpaceItem
import com.emberr.presentation.widget.headerSpaceItem
import com.emberr.presentation.widget.widgetHeaderRowHeight
import com.emberr.presentation.widget.widgetListTopSpace
import com.emberr.presentation.widget.secondaryTextColor
import com.emberr.presentation.widget.surfaceColor
import com.emberr.presentation.widget.widgetCalendarScreenExtra
import com.emberr.presentation.widget.widgetNewEventExtra
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

private val dateColumnWidth = 30.dp
private val eventCardCornerRadius = 10.dp
private const val lightCardWhiteBlend = 0.5f
private val eventCardTitleColor = ColorProvider(Color(0xFF1A1A1A))
private val eventCardTimeColor = ColorProvider(Color(0xB31A1A1A))

class UpcomingEventsWidget : GlanceAppWidget(), KoinComponent {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val content = loadAndCacheUpcomingEvents(context, id) ?: readCachedUpcomingEvents(context, id)

        provideContent {
            val rawCachedContent = currentState<Preferences>()[cachedUpcomingEventsKey]
            val savedContent = remember(rawCachedContent) { decodeCachedUpcomingEvents(rawCachedContent) }

            UpcomingEventsWidgetBody(context = context, content = savedContent ?: content)
        }
    }

    private suspend fun loadAndCacheUpcomingEvents(
        context: Context,
        id: GlanceId
    ): UpcomingEventsWidgetContent? {
        val contentReader = runCatching { get<UpcomingEventsWidgetContentReader>() }.getOrNull() ?: return null
        val freshContent = contentReader.readContentOnce() ?: return null

        if (freshContent != readCachedUpcomingEvents(context, id)) {
            writeCachedUpcomingEvents(context, id, freshContent)
        }
        return freshContent
    }
}

@Composable
internal fun UpcomingEventsWidgetBody(
    context: Context,
    content: UpcomingEventsWidgetContent?
) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .cornerRadius(widgetCornerRadius)
            .padding(start = widgetSidePadding)
    ) {
        EventList(context = context, content = content)

        WidgetTopFade(height = widgetListTopSpace)

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(widgetHeaderRowHeight)
                .padding(top = widgetTopPadding, end = widgetSidePadding),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "Upcoming",
                maxLines = 1,
                style = TextStyle(
                    color = primaryTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(openCalendarScreenIntent(context)))
            )

            WidgetAddButton(
                contentDescription = "New event",
                onClick = actionStartActivity(newEventIntent(context))
            )
        }
    }
}

@Composable
private fun EventList(context: Context, content: UpcomingEventsWidgetContent?) {
    val events = content?.events.orEmpty()

    if (events.isEmpty()) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(top = widgetListTopSpace, end = widgetSidePadding),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = if (content == null) "Open Emberr to load events" else "No upcoming events",
                maxLines = 2,
                style = TextStyle(color = secondaryTextColor, fontSize = 15.sp)
            )
        }
        return
    }

    val eventsByDate = events.groupBy { event -> event.dateGroupKey }

    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        headerSpaceItem()
        eventsByDate.forEach { (dateGroupKey, eventsOnDate) ->
            val firstEvent = eventsOnDate.first()
            val isRepeatingGroup = firstEvent.dayLabel != null
            val needsHeader = isRepeatingGroup || firstEvent.weekdayShortLabel.isBlank()

            if (needsHeader) {
                item(itemId = dateGroupKey.hashCode().toLong()) {
                    SectionHeader(label = firstEvent.dateLabel)
                }
            }

            items(items = eventsOnDate, itemId = { event -> event.blockId.hashCode().toLong() }) { event ->
                EventRow(
                    context = context,
                    event = event,
                    showsDate = isRepeatingGroup || event == firstEvent
                )
            }
        }
        footerSpaceItem()
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
        modifier = GlanceModifier.padding(top = 2.dp, bottom = 4.dp, end = widgetSidePadding)
    )
}

@Composable
private fun EventRow(context: Context, event: UpcomingEvent, showsDate: Boolean) {
    val categoryColor = categoryColorOf(event.accentColorHex)

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 6.dp, end = widgetSidePadding)
            .clickable(actionStartActivity(openEventIntent(context, event.blockId, event.birthdayPersonNoteId))),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        Column(
            modifier = GlanceModifier.width(dateColumnWidth).padding(top = 4.dp),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            if (showsDate && event.weekdayShortLabel.isNotBlank()) {
                Text(
                    text = event.weekdayShortLabel,
                    maxLines = 1,
                    style = TextStyle(color = secondaryTextColor, fontSize = 10.sp)
                )
                Text(
                    text = event.dayOfMonthLabel,
                    maxLines = 1,
                    style = TextStyle(
                        color = primaryTextColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = GlanceModifier.width(6.dp))

        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .background(categoryColor?.let(::eventCardColorFor) ?: elevatedSurfaceColor)
                .cornerRadius(eventCardCornerRadius)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = event.title,
                maxLines = 1,
                style = TextStyle(
                    color = if (categoryColor != null) eventCardTitleColor else primaryTextColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            )

            if (event.timeLabel != null) {
                Text(
                    text = event.timeLabel,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (categoryColor != null) eventCardTimeColor else secondaryTextColor,
                        fontSize = 12.sp,
                        fontWeight = if (event.isOverdue) FontWeight.Medium else FontWeight.Normal
                    )
                )
            }
        }
    }
}

private fun categoryColorOf(colorHex: String?): Color? =
    colorHex?.let { hex ->
        try {
            Color(hex.toColorInt())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

private fun eventCardColorFor(categoryColor: Color): ColorProvider =
    ColorProvider(
        day = lerp(categoryColor, Color.White, lightCardWhiteBlend),
        night = categoryColor
    )

private fun openEventIntent(context: Context, blockId: String, birthdayPersonNoteId: String?): Intent =
    Intent(context, MainActivity::class.java)
        .setData("$calendarEventUriScheme://event/$blockId".toUri())
        .putExtra(widgetBirthdayPersonNoteIdExtra, birthdayPersonNoteId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun openCalendarScreenIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .setData("emberr://calendar".toUri())
        .putExtra(widgetCalendarScreenExtra, true)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun newEventIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .setData("emberr://calendar/new".toUri())
        .putExtra(widgetCalendarScreenExtra, true)
        .putExtra(widgetNewEventExtra, true)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
