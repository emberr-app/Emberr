// The home screen widget itself: draws one note's icon and name as a shortcut that opens it.
package com.emberr.presentation.widget.noteshortcut

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.glance.action.Action
import androidx.glance.LocalSize
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.currentState
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
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
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.color.ColorProvider
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.emberr.R
import com.emberr.presentation.widget.elevatedSurfaceColor
import com.emberr.ui.theme.HighlightColor
import com.emberr.MainActivity
import com.emberr.presentation.widget.WidgetDeletedBadge
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.WidgetNoteSource
import com.emberr.presentation.widget.primaryTextColor
import com.emberr.presentation.widget.secondaryTextColor
import com.emberr.presentation.widget.surfaceColor
import com.emberr.presentation.widget.widgetNoteIdExtra
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import com.emberr.presentation.widget.writeDeletedFlag

private val pillHeight = 66.dp
private val pillCornerRadius = 33.dp
private val iconCircleSize = 48.dp
private const val iconToCircleRatio = 0.46f
private val compactWidthThreshold = 100.dp
private val compactCircleMargin = 8.dp
private val minimumCompactCircleSize = 36.dp
private val maximumCompactCircleSize = 64.dp

class NoteShortcutWidget : GlanceAppWidget(), KoinComponent {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val noteIdAtStart = readShortcutNoteId(context, id)?.takeIf { it.isNotBlank() }
        if (noteIdAtStart != null && loadAndCacheShortcut(context, id, noteIdAtStart) == null) {
            val noteSource = runCatching { get<WidgetNoteSource>() }.getOrNull()
            if (noteSource?.isNoteDeleted(noteIdAtStart) == true) writeDeletedFlag(context, id, shortcutNoteDeletedKey)
        }

        val appWidgetId = resolveAppWidgetId(context, id)

        provideContent {
            val savedState = currentState<Preferences>()
            val storedNoteId = savedState[shortcutNoteIdKey]?.takeIf { it.isNotBlank() }

            if (storedNoteId == null) {
                NoteShortcutSetupPrompt(context = context, appWidgetId = appWidgetId)
            } else {
                NoteShortcutWidgetBody(
                    context = context,
                    content = decodeCachedShortcut(savedState[cachedShortcutKey]),
                    isNoteDeleted = savedState[shortcutNoteDeletedKey] == true,
                    appWidgetId = appWidgetId
                )
            }
        }
    }

    private suspend fun loadAndCacheShortcut(
        context: Context,
        id: GlanceId,
        noteId: String
    ): NoteShortcutWidgetContent? {
        val contentReader = runCatching { get<NoteShortcutWidgetContentReader>() }.getOrNull() ?: return null
        val freshContent = contentReader.readContentOnce(noteId) ?: return null

        if (freshContent != readCachedShortcut(context, id)) {
            writeCachedShortcut(context, id, freshContent)
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
internal fun NoteShortcutWidgetBody(
    context: Context,
    content: NoteShortcutWidgetContent?,
    isNoteDeleted: Boolean,
    appWidgetId: Int
) {
    if (isCompactSize()) {
        when {
            isNoteDeleted -> CompactShortcut(
                icon = content?.icon,
                circleColor = elevatedSurfaceColor,
                tapAction = actionStartActivity(openPickerIntent(context, appWidgetId))
            )

            content == null -> CompactShortcut(icon = null, circleColor = elevatedSurfaceColor, tapAction = null)

            else -> CompactShortcut(
                icon = content.icon,
                circleColor = iconCircleColorFor(content.noteId),
                tapAction = actionStartActivity(openNoteIntent(context, content.noteId))
            )
        }
        return
    }

    if (isNoteDeleted) {
        DeletedShortcut(context = context, content = content, appWidgetId = appWidgetId)
        return
    }

    if (content == null) {
        ShortcutSurface(tapModifier = GlanceModifier) {
            Text(
                text = "Open Emberr to load this note",
                maxLines = 1,
                style = TextStyle(color = secondaryTextColor, fontSize = 14.sp)
            )
        }
        return
    }

    ShortcutSurface(
        tapModifier = GlanceModifier.clickable(
            actionStartActivity(openNoteIntent(context, content.noteId))
        ),
        hasIconCircle = true
    ) {
        NoteIconCircle(icon = content.icon, circleColor = iconCircleColorFor(content.noteId))

        Spacer(modifier = GlanceModifier.width(12.dp))

        Text(
            text = content.title,
            maxLines = 1,
            style = TextStyle(
                color = primaryTextColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.defaultWeight()
        )
    }
}

@Composable
private fun DeletedShortcut(context: Context, content: NoteShortcutWidgetContent?, appWidgetId: Int) {
    ShortcutSurface(
        tapModifier = GlanceModifier.clickable(
            actionStartActivity(openPickerIntent(context, appWidgetId))
        ),
        hasIconCircle = true
    ) {
        NoteIconCircle(icon = content?.icon, circleColor = elevatedSurfaceColor)

        Spacer(modifier = GlanceModifier.width(12.dp))

        Text(
            text = content?.title ?: "Untitled",
            maxLines = 1,
            style = TextStyle(
                color = secondaryTextColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.LineThrough
            ),
            modifier = GlanceModifier.defaultWeight()
        )

        Spacer(modifier = GlanceModifier.width(8.dp))

        WidgetDeletedBadge()
    }
}

@Composable
private fun NoteShortcutSetupPrompt(context: Context, appWidgetId: Int) {
    if (isCompactSize()) {
        CompactShortcut(
            icon = null,
            circleColor = elevatedSurfaceColor,
            tapAction = actionStartActivity(openPickerIntent(context, appWidgetId)),
            fallbackIconResource = R.drawable.ic_widget_plus
        )
        return
    }

    ShortcutSurface(
        tapModifier = GlanceModifier.clickable(
            actionStartActivity(openPickerIntent(context, appWidgetId))
        )
    ) {
        Text(
            text = "Tap to choose a note",
            maxLines = 1,
            style = TextStyle(color = secondaryTextColor, fontSize = 14.sp)
        )
    }
}

@Composable
private fun isCompactSize(): Boolean = LocalSize.current.width < compactWidthThreshold

@Composable
private fun CompactShortcut(
    icon: String?,
    circleColor: ColorProvider,
    tapAction: Action?,
    fallbackIconResource: Int = R.drawable.ic_widget_ghost
) {
    val widgetSize = LocalSize.current
    val circleSize = (minOf(widgetSize.width, widgetSize.height) - compactCircleMargin)
        .coerceIn(minimumCompactCircleSize, maximumCompactCircleSize)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .then(if (tapAction == null) GlanceModifier else GlanceModifier.clickable(tapAction)),
        contentAlignment = Alignment.Center
    ) {
        NoteIconCircle(
            icon = icon,
            circleColor = circleColor,
            circleSize = circleSize,
            fallbackIconResource = fallbackIconResource
        )
    }
}

@Composable
private fun NoteIconCircle(
    icon: String?,
    circleColor: ColorProvider,
    circleSize: Dp = iconCircleSize,
    fallbackIconResource: Int = R.drawable.ic_widget_ghost
) {
    Box(
        modifier = GlanceModifier
            .size(circleSize)
            .background(circleColor)
            .cornerRadius(circleSize / 2),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Text(text = icon, style = TextStyle(color = primaryTextColor, fontSize = (circleSize.value * iconToCircleRatio).sp))
        } else {
            Image(
                provider = ImageProvider(fallbackIconResource),
                contentDescription = null,
                colorFilter = ColorFilter.tint(primaryTextColor),
                modifier = GlanceModifier.size(circleSize * iconToCircleRatio)
            )
        }
    }
}

private val iconCirclePalette = HighlightColor.entries.filter { color -> color != HighlightColor.GREY }

private fun iconCircleColorFor(noteId: String): ColorProvider {
    val paletteColor = iconCirclePalette[noteId.hashCode().mod(iconCirclePalette.size)]
    return ColorProvider(day = paletteColor.lightBackground, night = paletteColor.darkBackground)
}

@Composable
private fun ShortcutSurface(
    tapModifier: GlanceModifier,
    hasIconCircle: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = GlanceModifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(pillHeight)
                .background(surfaceColor)
                .cornerRadius(pillCornerRadius)
                .padding(start = if (hasIconCircle) 9.dp else 20.dp, end = 20.dp)
                .then(tapModifier),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            content()
        }
    }
}

private fun openNoteIntent(context: Context, noteId: String): Intent =
    Intent(context, MainActivity::class.java)
        .setData("emberr://note/$noteId".toUri())
        .putExtra(widgetNoteIdExtra, noteId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun openPickerIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, NoteShortcutWidgetPickerActivity::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
