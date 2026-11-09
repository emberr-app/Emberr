// The home screen widget itself: draws the list of notes with a title and an add button.
package com.emberr.presentation.widget.notelist

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.currentState
import androidx.glance.appwidget.GlanceAppWidgetManager
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.emberr.MainActivity
import com.emberr.R
import com.emberr.presentation.widget.WidgetColorTheme
import com.emberr.presentation.widget.WidgetTopFade
import com.emberr.presentation.widget.WidgetAddButton
import com.emberr.presentation.widget.elevatedSurfaceColor
import com.emberr.presentation.widget.primaryTextColor
import com.emberr.presentation.widget.widgetBottomPadding
import com.emberr.presentation.widget.widgetCornerRadius
import com.emberr.presentation.widget.widgetSidePadding
import com.emberr.presentation.widget.widgetTopPadding
import com.emberr.presentation.widget.footerSpaceItem
import com.emberr.presentation.widget.headerSpaceItem
import com.emberr.presentation.widget.widgetHeaderRowHeight
import com.emberr.presentation.widget.widgetListTopSpace
import com.emberr.presentation.widget.secondaryTextColor
import com.emberr.presentation.widget.surfaceColor
import com.emberr.presentation.widget.widgetHomeScreenExtra
import com.emberr.presentation.widget.widgetNewNoteExtra
import com.emberr.presentation.widget.widgetNoteIdExtra
import com.emberr.presentation.widget.widgetSpaceIdExtra
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.isPinnedSpaceDeleted
import com.emberr.presentation.widget.readWidgetSpaceId
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import com.emberr.presentation.widget.WidgetDeletedState
import com.emberr.presentation.widget.previewNoteListContent
import com.emberr.presentation.widget.RedrawIfStateChangedAtStart
import com.emberr.presentation.widget.readWidgetState

private val headerIconSize = 18.dp
private val noteCardCornerRadius = 14.dp
private val rowIconSize = 20.dp
private val rowIconSlotWidth = 32.dp

class NoteListWidget : GlanceAppWidget(), KoinComponent {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stateAtStart = readWidgetState(context, id)
        val loadedContent = loadAndCacheNoteList(context, id) ?: readCachedNoteList(context, id)
        val spaceId = readWidgetSpaceId(context, id)
        val appWidgetId = resolveAppWidgetId(context, id)
        val stateChangedAtStart = readWidgetState(context, id) != stateAtStart

        provideContent {
            RedrawIfStateChangedAtStart(context, id, stateChangedAtStart)
            WidgetColorTheme {
                val savedState = currentState<Preferences>()
                val rawCachedNoteList = savedState[cachedNoteListKey]
                val cachedContent = remember(rawCachedNoteList) { decodeCachedNoteList(rawCachedNoteList) }

                NoteListWidgetBody(
                    context = context,
                    spaceId = spaceId,
                    content = cachedContent ?: loadedContent,
                    appWidgetId = appWidgetId
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            WidgetColorTheme(followsSettingChanges = false) {
                NoteListWidgetBody(
                    context = context,
                    spaceId = "",
                    content = previewNoteListContent,
                    appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                )
            }
        }
    }

    private suspend fun loadAndCacheNoteList(context: Context, id: GlanceId): NoteListWidgetContent? {
        val contentReader = runCatching { get<NoteListWidgetContentReader>() }.getOrNull() ?: return null
        val freshContent = if (isPinnedSpaceDeleted(context, id)) {
            deletedSpaceNoteList
        } else {
            contentReader.readContentOnce(readWidgetSpaceId(context, id)) ?: return null
        }

        if (freshContent != readCachedNoteList(context, id)) {
            writeCachedNoteList(context, id, freshContent)
        }
        return freshContent
    }

    private suspend fun resolveAppWidgetId(context: Context, id: GlanceId): Int =
        try {
            GlanceAppWidgetManager(context).getAppWidgetId(id)
        } catch (cause: Exception) {
            WidgetLog.e("Could not resolve the widget id", cause)
            AppWidgetManager.INVALID_APPWIDGET_ID
        }
}

@Composable
internal fun NoteListWidgetBody(
    context: Context,
    spaceId: String,
    content: NoteListWidgetContent?,
    appWidgetId: Int
) {
    if (content?.isSpaceDeleted == true) {
        DeletedSpaceNoteList(context = context, appWidgetId = appWidgetId)
        return
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .cornerRadius(widgetCornerRadius)
            .padding(start = widgetSidePadding)
    ) {
        val notes = content?.notes.orEmpty()
        if (notes.isEmpty()) {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(top = widgetListTopSpace, end = widgetSidePadding)
                    .clickable(actionStartActivity(openHomeScreenIntent(context, spaceId))),
                verticalAlignment = Alignment.Vertical.CenterVertically,
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = if (content == null) {
                        "Open Emberr to load your notes"
                    } else {
                        "No notes yet. Tap to write one."
                    },
                    style = TextStyle(color = secondaryTextColor, fontSize = 15.sp)
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                headerSpaceItem()
                items(items = notes, itemId = { note -> note.noteId.hashCode().toLong() }) { note ->
                    NoteRow(context = context, note = note)
                }
                footerSpaceItem()
            }
        }

        WidgetTopFade(height = widgetListTopSpace)

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(widgetHeaderRowHeight)
                .padding(top = widgetTopPadding, end = widgetSidePadding),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(openHomeScreenIntent(context, spaceId))),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_ghost),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(primaryTextColor),
                    modifier = GlanceModifier.size(headerIconSize)
                )

                Spacer(modifier = GlanceModifier.width(10.dp))

                Text(
                    text = "Notes",
                    maxLines = 1,
                    style = TextStyle(
                        color = primaryTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            WidgetAddButton(
                contentDescription = "New note",
                onClick = actionStartActivity(newNoteIntent(context, spaceId))
            )
        }
    }
}

@Composable
private fun DeletedSpaceNoteList(context: Context, appWidgetId: Int) {
    WidgetDeletedState(
        headerText = "Notes",
        headerStyle = TextStyle(
            color = secondaryTextColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        ),
        message = "This space was deleted. Tap to choose another",
        messageFontSize = 15.sp,
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .cornerRadius(widgetCornerRadius)
            .padding(horizontal = widgetSidePadding)
            .padding(top = widgetTopPadding, bottom = widgetBottomPadding)
            .clickable(actionStartActivity(openSpacePickerIntent(context, appWidgetId)))
    )
}

@Composable
private fun NoteRow(context: Context, note: NoteListWidgetRow) {
    Column(modifier = GlanceModifier.fillMaxWidth().padding(end = widgetSidePadding, bottom = 6.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(elevatedSurfaceColor)
                .cornerRadius(noteCardCornerRadius)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clickable(actionStartActivity(openNoteIntent(context, note.noteId))),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            NoteRowIcon(icon = note.icon)

            Spacer(modifier = GlanceModifier.width(6.dp))

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = note.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = primaryTextColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )

                if (note.snippet.isNotBlank()) {
                    Text(
                        text = note.snippet,
                        maxLines = 1,
                        style = TextStyle(color = secondaryTextColor, fontSize = 12.sp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteRowIcon(icon: String?) {
    Box(modifier = GlanceModifier.width(rowIconSlotWidth), contentAlignment = Alignment.Center) {
        if (icon != null) {
            Text(text = icon, style = TextStyle(color = primaryTextColor, fontSize = 20.sp))
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_doc_text),
                contentDescription = null,
                colorFilter = ColorFilter.tint(secondaryTextColor),
                modifier = GlanceModifier.size(rowIconSize)
            )
        }
    }
}

private fun openSpacePickerIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, NoteListWidgetPickerActivity::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

private fun openHomeScreenIntent(context: Context, spaceId: String): Intent =
    Intent(context, MainActivity::class.java)
        .setData("emberr://notes".toUri())
        .putExtra(widgetHomeScreenExtra, true)
        .putExtra(widgetSpaceIdExtra, spaceId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun newNoteIntent(context: Context, spaceId: String): Intent =
    Intent(context, MainActivity::class.java)
        .setData("emberr://notes/new".toUri())
        .putExtra(widgetHomeScreenExtra, true)
        .putExtra(widgetNewNoteExtra, true)
        .putExtra(widgetSpaceIdExtra, spaceId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun openNoteIntent(context: Context, noteId: String): Intent =
    Intent(context, MainActivity::class.java)
        .setData("emberr://note/$noteId".toUri())
        .putExtra(widgetNoteIdExtra, noteId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
