// The home screen widget itself: draws today's date, a heading and today's tasks as a nested card.
package com.emberr.presentation.widget.todaytasks

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
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
import com.emberr.presentation.widget.WidgetCheckbox
import com.emberr.presentation.widget.WidgetLabelPill
import com.emberr.presentation.widget.widgetCornerRadius
import com.emberr.presentation.widget.widgetSidePadding
import com.emberr.presentation.widget.widgetTopPadding
import com.emberr.presentation.widget.footerSpaceItem
import com.emberr.presentation.widget.headerSpaceItem
import com.emberr.ui.theme.HighlightColor
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.elevatedSurfaceColor
import com.emberr.presentation.widget.primaryTextColor
import com.emberr.presentation.widget.secondaryTextColor
import com.emberr.presentation.widget.separatorColor
import com.emberr.presentation.widget.surfaceColor
import com.emberr.presentation.widget.taskBlockIdExtra
import com.emberr.presentation.widget.taskIsCheckedExtra
import com.emberr.presentation.widget.taskOccurrenceDateExtra
import com.emberr.presentation.widget.widgetDailyDateExtra
import com.emberr.presentation.widget.widgetDailyScreenExtra
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import com.emberr.presentation.widget.previewTodayTasksContent

private val checkboxTapWidth = 36.dp
private val cardInset = 8.dp
private val cardTopPadding = 14.dp
private val cardTitleHeight = 24.dp
private val cardListTopSpace = cardTopPadding + cardTitleHeight + 4.dp
private val tasksCardCornerRadius = 20.dp
private val overduePillColor = ColorProvider(
    day = HighlightColor.RED.lightBackground,
    night = HighlightColor.RED.darkBackground
)

class TodayTasksWidget : GlanceAppWidget(), KoinComponent {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val content = loadAndCacheTodayTasks(context, id) ?: readCachedTodayTasks(context, id)
        val appWidgetId = resolveAppWidgetId(context, id)

        provideContent {
            WidgetColorTheme {
                val rawCachedContent = currentState<Preferences>()[cachedTodayTasksKey]
                val savedContent = remember(rawCachedContent) { decodeCachedTodayTasks(rawCachedContent) }

                TodayTasksWidgetBody(
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
                TodayTasksWidgetBody(
                    context = context,
                    appWidgetId = INVALID_APPWIDGET_ID,
                    content = previewTodayTasksContent()
                )
            }
        }
    }

    private suspend fun resolveAppWidgetId(context: Context, id: GlanceId): Int =
        try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (cause: Exception) {
            WidgetLog.e("Could not resolve the widget id", cause)
            INVALID_APPWIDGET_ID
        }

    private suspend fun loadAndCacheTodayTasks(
        context: Context,
        id: GlanceId
    ): TodayTasksWidgetContent? {
        val contentReader = runCatching { get<TodayTasksWidgetContentReader>() }.getOrNull() ?: return null
        val freshContent = contentReader.readContentOnce() ?: return null

        if (freshContent != readCachedTodayTasks(context, id)) {
            writeCachedTodayTasks(context, id, freshContent)
        }
        return freshContent
    }
}

@Composable
internal fun TodayTasksWidgetBody(
    context: Context,
    appWidgetId: Int,
    content: TodayTasksWidgetContent?
) {
    val openDailyScreen = actionStartActivity(openDailyScreenIntent(context))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .cornerRadius(widgetCornerRadius)
            .padding(horizontal = cardInset)
            .padding(bottom = cardInset)
    ) {
        DateHeader(content = content, openDailyScreen = openDailyScreen)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(elevatedSurfaceColor)
                .cornerRadius(tasksCardCornerRadius)
                .padding(start = widgetSidePadding)
        ) {
            val rows = content?.rows.orEmpty()
            if (rows.isEmpty()) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(top = cardListTopSpace, end = widgetSidePadding)
                        .clickable(openDailyScreen),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.CenterHorizontally
                ) {
                    Text(
                        text = if (content == null) {
                            "Open Emberr to load today's tasks"
                        } else {
                            "Nothing planned for today"
                        },
                        style = TextStyle(color = secondaryTextColor, fontSize = 14.sp)
                    )
                }
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    headerSpaceItem(height = cardListTopSpace)
                    items(items = rows, itemId = { row -> row.blockId.hashCode().toLong() }) { row ->
                        TodayTaskRowItem(
                            context = context,
                            appWidgetId = appWidgetId,
                            row = row,
                            openDailyScreen = openDailyScreen
                        )
                    }
                    footerSpaceItem()
                }
            }

            WidgetTopFade(height = cardListTopSpace, fadeColor = elevatedSurfaceColor)

            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(cardTopPadding + cardTitleHeight)
                    .padding(top = cardTopPadding, end = widgetSidePadding)
                    .clickable(openDailyScreen),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "Today's tasks",
                    maxLines = 1,
                    style = TextStyle(
                        color = primaryTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun DateHeader(content: TodayTasksWidgetContent?, openDailyScreen: Action) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(
                start = widgetSidePadding - cardInset,
                end = widgetSidePadding - cardInset,
                top = widgetTopPadding,
                bottom = 16.dp
            )
            .clickable(openDailyScreen),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Row(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            val weekdayLabel = content?.weekdayLabel.orEmpty()
            if (weekdayLabel.isNotBlank()) {
                Text(
                    text = weekdayLabel,
                    maxLines = 1,
                    style = TextStyle(
                        color = primaryTextColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = GlanceModifier.width(8.dp))
            }

            Text(
                text = content?.dateLabel.orEmpty(),
                maxLines = 1,
                style = TextStyle(color = secondaryTextColor, fontSize = 15.sp)
            )
        }

        if (content != null && content.totalCount > 0) {
            DoneCountPill(doneCount = content.doneCount, totalCount = content.totalCount)
        }
    }
}

@Composable
private fun DoneCountPill(doneCount: Int, totalCount: Int) {
    Box(
        modifier = GlanceModifier
            .background(separatorColor)
            .cornerRadius(12.dp)
            .padding(1.dp)
    ) {
        Text(
            text = "$doneCount of $totalCount done",
            maxLines = 1,
            style = TextStyle(color = primaryTextColor, fontSize = 12.sp),
            modifier = GlanceModifier
                .background(surfaceColor)
                .cornerRadius(11.dp)
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TodayTaskRowItem(
    context: Context,
    appWidgetId: Int,
    row: TodayTaskRow,
    openDailyScreen: Action
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(end = widgetSidePadding),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        WidgetCheckbox(
            isChecked = row.isChecked,
            checkedFillColor = surfaceColor,
            modifier = GlanceModifier
                .width(checkboxTapWidth)
                .fillMaxHeight()
                .clickable(
                    onClick = actionSendBroadcast(
                        toggleTaskIntent(
                            context = context,
                            appWidgetId = appWidgetId,
                            blockId = row.blockId,
                            occurrenceDate = row.occurrenceDate,
                            isChecked = !row.isChecked
                        )
                    ),
                    rippleOverride = R.drawable.widget_no_ripple
                )
        )

        Text(
            text = row.title,
            maxLines = 2,
            style = TextStyle(
                color = if (row.isChecked) secondaryTextColor else primaryTextColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = if (row.isChecked) {
                    TextDecoration.LineThrough
                } else {
                    TextDecoration.None
                }
            ),
            modifier = GlanceModifier
                .defaultWeight()
                .padding(top = 7.dp, bottom = 7.dp)
                .clickable(openDailyScreen)
        )

        if (row.scheduleLabel != null) {
            Spacer(modifier = GlanceModifier.width(8.dp))

            if (row.isOverdue) {
                WidgetLabelPill(
                    text = row.scheduleLabel,
                    background = overduePillColor,
                    textColor = primaryTextColor,
                    isEmphasized = true
                )
            } else {
                WidgetLabelPill(text = row.scheduleLabel, background = surfaceColor)
            }
        }
    }
}

private fun toggleTaskIntent(
    context: Context,
    appWidgetId: Int,
    blockId: String,
    occurrenceDate: String?,
    isChecked: Boolean
): Intent =
    Intent(context, TodayTasksWidgetActionReceiver::class.java).apply {
        action = toggleTodayTaskAction
        data = "emberr-today://task/$appWidgetId/$blockId".toUri()
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putExtra(taskBlockIdExtra, blockId)
        putExtra(taskIsCheckedExtra, isChecked)
        putExtra(taskOccurrenceDateExtra, occurrenceDate)
    }

private fun openDailyScreenIntent(context: Context): Intent {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

    return Intent(context, MainActivity::class.java)
        .setData("emberr://daily/$today".toUri())
        .putExtra(widgetDailyScreenExtra, true)
        .putExtra(widgetDailyDateExtra, today)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
