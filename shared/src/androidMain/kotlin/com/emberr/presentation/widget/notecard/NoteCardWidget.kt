// The home screen widget itself: draws one note as a card like the favorite note cards in the app.
package com.emberr.presentation.widget.notecard

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.emberr.MainActivity
import com.emberr.R
import com.emberr.presentation.widget.WidgetColorTheme
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.WidgetNoteSource
import com.emberr.presentation.widget.surfaceColor
import com.emberr.presentation.widget.highlightColor
import com.emberr.presentation.widget.primaryTextColor
import com.emberr.presentation.widget.secondaryTextColor
import com.emberr.presentation.widget.widgetCornerRadius
import com.emberr.presentation.widget.widgetNoteIdExtra
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import com.emberr.presentation.widget.writeDeletedFlag
import com.emberr.presentation.widget.WidgetDeletedState
import com.emberr.presentation.widget.previewNoteCardContent
import com.emberr.presentation.widget.RedrawIfStateChangedAtStart
import com.emberr.presentation.widget.readWidgetState

private val minimumCoverBandHeight = 40.dp
private val iconOverhang = 12.dp
private val cardSidePadding = 14.dp
private val favoriteStarReservedWidth = 16.dp
private val cardTextVerticalPadding = 12.dp
private val noteContentSidePadding = 18.dp
private val titleGapBelowIcon = 15.dp
private val cardTextBottomPadding = 18.dp
private val titleLineHeight = 25.dp
private val emojiIconStartNudge = 2.dp
private val emojiIconTopNudge = 6.dp
private val favoriteStarSize = 14.dp
private val favoriteStarPadding = 14.dp
private val canvasIconSize = 22.dp
private const val coverBitmapTargetWidthPixels = 600
private val coverBandTint = ColorProvider(day = Color(0x0D000000), night = Color(0x0DFFFFFF))

class NoteCardWidget : GlanceAppWidget(), KoinComponent {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val stateAtStart = readWidgetState(context, id)
        val noteIdAtStart = readCardNoteId(context, id)?.takeIf { it.isNotBlank() }
        if (noteIdAtStart != null && loadAndCacheCard(context, id, noteIdAtStart) == null) {
            val noteSource = runCatching { get<WidgetNoteSource>() }.getOrNull()
            if (noteSource?.isNoteDeleted(noteIdAtStart) == true) writeDeletedFlag(context, id, cardNoteDeletedKey)
        }

        val appWidgetId = resolveAppWidgetId(context, id)
        val stateChangedAtStart = readWidgetState(context, id) != stateAtStart

        provideContent {
            RedrawIfStateChangedAtStart(context, id, stateChangedAtStart)
            WidgetColorTheme {
                val savedState = currentState<Preferences>()
                val rawCachedCard = savedState[cachedCardKey]
                val content = remember(rawCachedCard) { decodeCachedCard(rawCachedCard) }

                NoteCardWidgetBody(
                    context = context,
                    appWidgetId = appWidgetId,
                    chosenNoteId = savedState[cardNoteIdKey]?.takeIf { it.isNotBlank() },
                    content = content,
                    isNoteDeleted = savedState[cardNoteDeletedKey] == true
                )
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            WidgetColorTheme(followsSettingChanges = false) {
                NoteCardWidgetBody(
                    context = context,
                    appWidgetId = INVALID_APPWIDGET_ID,
                    chosenNoteId = previewNoteCardContent.noteId,
                    content = previewNoteCardContent,
                    isNoteDeleted = false
                )
            }
        }
    }

    private suspend fun loadAndCacheCard(context: Context, id: GlanceId, noteId: String): NoteCardWidgetContent? {
        val contentReader = runCatching { get<NoteCardWidgetContentReader>() }.getOrNull() ?: return null
        val freshContent = contentReader.readContentOnce(noteId) ?: return null

        if (freshContent != readCachedCard(context, id)) {
            writeCachedCard(context, id, freshContent)
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
internal fun NoteCardWidgetBody(
    context: Context,
    appWidgetId: Int,
    chosenNoteId: String?,
    content: NoteCardWidgetContent?,
    isNoteDeleted: Boolean
) {
    val chooseNoteAction = actionStartActivity(openPickerIntent(context, appWidgetId))

    when {
        chosenNoteId == null -> CardMessage(message = "Tap to choose a note", tapAction = chooseNoteAction)

        isNoteDeleted -> DeletedCard(title = content?.title, chooseAnotherNoteAction = chooseNoteAction)

        content == null -> CardMessage(message = "Open Emberr to load this note", tapAction = null)

        else -> NoteCard(
            content = content,
            openNoteAction = actionStartActivity(openNoteIntent(context, content.noteId))
        )
    }
}

@Composable
private fun NoteCard(content: NoteCardWidgetContent, openNoteAction: Action) {
    val hasIcon = content.icon != null
    val hasCanvasIcon = content.isCanvas && !hasIcon
    val hasCoverBand = content.coverImageAbsolutePath != null || content.isCanvas || hasIcon
    val titleTopPadding = if (hasIcon || hasCanvasIcon) iconOverhang + titleGapBelowIcon else cardTextVerticalPadding
    val coverBandHeight = (LocalSize.current.height - titleTopPadding - titleLineHeight - cardTextBottomPadding)
        .coerceAtLeast(minimumCoverBandHeight)
    val coverBitmap = remember(content.coverImageAbsolutePath) {
        content.coverImageAbsolutePath?.let(::loadCoverBitmap)
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .cornerRadius(widgetCornerRadius)
            .clickable(openNoteAction)
    ) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            if (hasCoverBand) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(coverBandHeight)
                        .background(coverBandTint)
                ) {
                    if (coverBitmap != null) {
                        Image(
                            provider = ImageProvider(coverBitmap),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = GlanceModifier.fillMaxSize()
                        )
                    }
                }
            }

            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .padding(
                        start = noteContentSidePadding,
                        end = if (content.isFavorite && !hasCoverBand) noteContentSidePadding + favoriteStarReservedWidth else noteContentSidePadding,
                        top = titleTopPadding,
                        bottom = if (hasCoverBand) cardTextBottomPadding else cardTextVerticalPadding
                    ),
                verticalAlignment = if (hasCoverBand) Alignment.Vertical.Bottom else Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = content.title,
                    maxLines = if (hasCoverBand) 1 else 3,
                    style = TextStyle(
                        color = primaryTextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }

        if (hasIcon) {
            Text(
                text = content.icon.orEmpty(),
                style = TextStyle(color = primaryTextColor, fontSize = 24.sp),
                modifier = GlanceModifier.padding(start = noteContentSidePadding - emojiIconStartNudge, top = coverBandHeight - iconOverhang - emojiIconTopNudge)
            )
        }

        if (hasCanvasIcon) {
            Box(modifier = GlanceModifier.padding(start = noteContentSidePadding, top = coverBandHeight - iconOverhang)) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_canvas),
                    contentDescription = "Canvas",
                    colorFilter = ColorFilter.tint(primaryTextColor),
                    modifier = GlanceModifier.size(canvasIconSize)
                )
            }
        }

        if (content.isFavorite) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
                Box(modifier = GlanceModifier.padding(favoriteStarPadding)) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_star),
                        contentDescription = "Favorite",
                        colorFilter = ColorFilter.tint(highlightColor),
                        modifier = GlanceModifier.size(favoriteStarSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun DeletedCard(title: String?, chooseAnotherNoteAction: Action) {
    WidgetDeletedState(
        headerText = title ?: "Untitled",
        headerStyle = TextStyle(
            color = secondaryTextColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.LineThrough
        ),
        message = "This note was deleted. Tap to choose another",
        messageFontSize = 13.sp,
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .cornerRadius(widgetCornerRadius)
            .padding(cardSidePadding)
            .clickable(chooseAnotherNoteAction)
    )
}

@Composable
private fun CardMessage(message: String, tapAction: Action?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(surfaceColor)
            .cornerRadius(widgetCornerRadius)
            .padding(cardSidePadding)
            .then(if (tapAction == null) GlanceModifier else GlanceModifier.clickable(tapAction)),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = message,
            style = TextStyle(color = secondaryTextColor, fontSize = 14.sp)
        )
    }
}

private fun loadCoverBitmap(absolutePath: String): Bitmap? =
    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(absolutePath, bounds)

        if (bounds.outWidth <= 0) {
            null
        } else {
            var sampleSize = 1
            while (bounds.outWidth / (sampleSize * 2) >= coverBitmapTargetWidthPixels) {
                sampleSize *= 2
            }
            BitmapFactory.decodeFile(absolutePath, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        }
    } catch (cause: Exception) {
        WidgetLog.e("Could not load the cover image for a note card", cause)
        null
    }

private fun openNoteIntent(context: Context, noteId: String): Intent =
    Intent(context, MainActivity::class.java)
        .setData("emberr://note/$noteId".toUri())
        .putExtra(widgetNoteIdExtra, noteId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

private fun openPickerIntent(context: Context, appWidgetId: Int): Intent =
    Intent(context, NoteCardWidgetPickerActivity::class.java)
        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
