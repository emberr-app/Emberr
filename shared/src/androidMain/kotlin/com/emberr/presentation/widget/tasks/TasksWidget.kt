// The home screen widget itself: draws the task list with the same title and top right icons as the Tasks screen.
package com.emberr.presentation.widget.tasks

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
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
import com.emberr.presentation.widget.WidgetTopFade
import com.emberr.presentation.widget.WidgetAddButton
import com.emberr.presentation.widget.WidgetCheckbox
import com.emberr.presentation.widget.WidgetLabelPill
import com.emberr.presentation.widget.elevatedSurfaceColor
import com.emberr.presentation.widget.widgetCornerRadius
import com.emberr.presentation.widget.widgetSidePadding
import com.emberr.presentation.widget.widgetTopPadding
import com.emberr.presentation.widget.footerSpaceItem
import com.emberr.presentation.widget.headerSpaceItem
import com.emberr.presentation.widget.widgetHeaderRowHeight
import com.emberr.presentation.widget.widgetListTopSpace
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.primaryTextColor
import com.emberr.presentation.widget.secondaryTextColor
import com.emberr.presentation.widget.surfaceColor
import com.emberr.presentation.widget.taskBlockIdExtra
import com.emberr.presentation.widget.taskIsCheckedExtra
import com.emberr.presentation.widget.taskOccurrenceDateExtra
import com.emberr.presentation.widget.widgetNewTaskExtra
import com.emberr.presentation.widget.widgetTasksScreenExtra
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

private val topBarIconSize = 20.dp
private val checkboxTapWidth = 36.dp
private val rowVerticalPadding = 5.dp

class TasksWidget : GlanceAppWidget(), KoinComponent {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val isShowingCompleted = readShowingCompleted(context, id)

        val content = loadAndCacheTasks(context, id, isShowingCompleted)
            ?: readCachedTasks(context, id)

        val appWidgetId = resolveAppWidgetId(context, id)

        provideContent {
            val rawCachedContent = currentState<Preferences>()[cachedTasksKey]
            val savedContent = remember(rawCachedContent) { decodeCachedTasks(rawCachedContent) }

            TasksWidgetBody(
                context = context,
                appWidgetId = appWidgetId,
                content = savedContent ?: content
            )
        }
    }

    private suspend fun loadAndCacheTasks(
        context: Context,
        id: GlanceId,
        isShowingCompleted: Boolean
    ): TasksWidgetContent? {
        val contentReader = runCatching { get<TasksWidgetContentReader>() }.getOrNull() ?: return null
        val freshContent = contentReader.readContentOnce(isShowingCompleted) ?: return null

        if (freshContent != readCachedTasks(context, id)) {
            writeCachedTasks(context, id, freshContent)
        }
        return freshContent
    }

    private suspend fun resolveAppWidgetId(context: Context, id: GlanceId): Int =
        try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (cause: Exception) {
            WidgetLog.e("Could not resolve the widget id", cause)
            INVALID_APPWIDGET_ID
        }
}

@Composable
internal fun TasksWidgetBody(
    context: Context,
    appWidgetId: Int,
    content: TasksWidgetContent?
) {
    val isShowingCompleted = content?.isShowingCompleted == true
    val openTasksScreen = actionStartActivity(openTasksScreenIntent(context))

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .cornerRadius(widgetCornerRadius)
            .padding(start = widgetSidePadding)
    ) {
        val rows = content?.rows.orEmpty()
        if (rows.isEmpty()) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(top = widgetListTopSpace, end = widgetSidePadding)
                    .clickable(openTasksScreen),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = when {
                        content == null -> "Open Emberr to load your tasks"
                        isShowingCompleted -> "No completed tasks yet."
                        else -> "All caught up!"
                    },
                    style = TextStyle(color = secondaryTextColor, fontSize = 15.sp)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                headerSpaceItem(height = widgetHeaderRowHeight)
                items(items = rows, itemId = { row -> row.key.hashCode().toLong() }) { row ->
                    when (row) {
                        is TasksWidgetRow.SectionHeading -> SectionHeadingRow(row)
                        is TasksWidgetRow.Task -> TaskRow(
                            task = row,
                            appWidgetId = appWidgetId,
                            context = context
                        )
                    }
                }
                footerSpaceItem()
            }
        }

        WidgetTopFade(height = widgetHeaderRowHeight)

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(widgetHeaderRowHeight)
                .padding(top = widgetTopPadding, end = widgetSidePadding),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = if (isShowingCompleted) "Completed" else "Tasks",
                maxLines = 1,
                style = TextStyle(
                    color = primaryTextColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight().clickable(openTasksScreen)
            )

            Image(
                provider = ImageProvider(R.drawable.ic_widget_check_square),
                contentDescription = "Completed tasks",
                colorFilter = ColorFilter.tint(
                    if (isShowingCompleted) primaryTextColor else secondaryTextColor
                ),
                modifier = GlanceModifier
                    .size(topBarIconSize)
                    .clickable(actionSendBroadcast(toggleCompletedViewIntent(context, appWidgetId)))
            )

            Spacer(modifier = GlanceModifier.width(12.dp))

            WidgetAddButton(
                contentDescription = "Add task",
                onClick = actionStartActivity(addTaskIntent(context))
            )
        }
    }
}

@Composable
private fun SectionHeadingRow(heading: TasksWidgetRow.SectionHeading) {
    Text(
        text = heading.label,
        maxLines = 1,
        style = TextStyle(
            color = secondaryTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        ),
        modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp, end = widgetSidePadding)
    )
}

@Composable
private fun TaskRow(task: TasksWidgetRow.Task, appWidgetId: Int, context: Context) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(end = widgetSidePadding),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        WidgetCheckbox(
            isChecked = task.isChecked,
            iconAlignment = Alignment.TopStart,
            modifier = GlanceModifier
                .width(checkboxTapWidth)
                .fillMaxHeight()
                .padding(top = rowVerticalPadding)
                .clickable(
                    onClick = actionSendBroadcast(
                        toggleTaskIntent(
                            context = context,
                            appWidgetId = appWidgetId,
                            blockId = task.blockId,
                            occurrenceDate = task.occurrenceDate,
                            isChecked = !task.isChecked
                        )
                    ),
                    rippleOverride = R.drawable.widget_no_ripple
                )
        )

        Column(modifier = GlanceModifier.defaultWeight().padding(top = rowVerticalPadding, bottom = rowVerticalPadding)) {
            Text(
                text = task.text,
                maxLines = 2,
                style = TextStyle(
                    color = if (task.isChecked) secondaryTextColor else primaryTextColor,
                    fontSize = 15.sp,
                    textDecoration = if (task.isChecked) TextDecoration.LineThrough else TextDecoration.None
                ),
                modifier = GlanceModifier.padding(top = 1.dp)
            )

            if (task.timeLabel != null) {
                Text(
                    text = task.timeLabel,
                    maxLines = 1,
                    style = TextStyle(color = secondaryTextColor, fontSize = 12.sp)
                )
            }
        }

        if (task.dayLabel != null) {
            Box(modifier = GlanceModifier.padding(start = 8.dp, top = 1.dp)) {
                WidgetLabelPill(text = task.dayLabel, background = elevatedSurfaceColor, textColor = primaryTextColor)
            }
        }
    }
}

private fun openTasksScreenIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .setData("emberr://tasks".toUri())
        .putExtra(widgetTasksScreenExtra, true)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun addTaskIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .setData("emberr://tasks/new".toUri())
        .putExtra(widgetTasksScreenExtra, true)
        .putExtra(widgetNewTaskExtra, true)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun toggleCompletedViewIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, TasksWidgetActionReceiver::class.java).apply {
        action = toggleCompletedViewAction
        data = "emberr-tasks://view/$appWidgetId".toUri()
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }

private fun toggleTaskIntent(
    context: Context,
    appWidgetId: Int,
    blockId: String,
    occurrenceDate: String?,
    isChecked: Boolean
): Intent =
    Intent(context, TasksWidgetActionReceiver::class.java).apply {
        action = toggleTaskAction
        data = "emberr-tasks://task/$appWidgetId/$blockId".toUri()
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        putExtra(taskBlockIdExtra, blockId)
        putExtra(taskIsCheckedExtra, isChecked)
        putExtra(taskOccurrenceDateExtra, occurrenceDate)
    }
