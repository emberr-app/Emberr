// The home screen widget itself: today's quote from the daily page on a gradient card.
package com.emberr.presentation.widget.dailyquote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.format.DateFormat
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.emberr.R
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.gradient.WidgetGradient
import com.emberr.presentation.widget.gradient.GradientCardHeader
import com.emberr.presentation.widget.gradient.continueTodaysDailyIntent
import com.emberr.presentation.widget.gradient.widgetGradientKey
import com.emberr.presentation.widget.gradient.gradientCardBackground
import com.emberr.presentation.widget.gradient.gradientCardPadding

private const val maximumQuoteLines = 4
private val quoteFontSizeStepsInSp = listOf(18f, 17f, 16f, 15f, 14f, 13f)
private const val authorFontSizeInSp = 13f
private const val dateFontSizeInSp = 13f
private const val datePattern = "MMM d"
private const val gapBetweenQuoteAndAuthorInDp = 8f
private const val quoteLineSpacingMultiplier = 1.1f
private const val headerRowHeightInDp = 30f
private val headerSideInset = 4.dp
private const val quoteTextColor = 0xFFFFFFFF.toInt()
private const val authorTextColor = 0xBFFFFFFF.toInt()
private const val dateTextColor = 0xE6FFFFFF.toInt()

class DailyQuoteWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        saveTodaysQuoteIfNeeded(context, id)

        provideContent {
            val savedState = currentState<Preferences>()
            DailyQuoteWidgetBody(
                context = context,
                gradient = WidgetGradient.named(savedState[widgetGradientKey]),
                quoteText = savedState[quoteTextKey],
                quoteAuthor = savedState[quoteAuthorKey]
            )
        }
    }
}

@Composable
private fun DailyQuoteWidgetBody(
    context: Context,
    gradient: WidgetGradient,
    quoteText: String?,
    quoteAuthor: String?
) {
    val widgetSize = LocalSize.current
    val shownQuote = quoteText ?: "Open Emberr to load today's quote"
    val renderedQuote = remember(shownQuote, quoteAuthor, widgetSize) {
        renderQuoteImage(context, shownQuote, quoteAuthor, widgetSize)
    }
    val todayLabel = DateFormat.format(datePattern, System.currentTimeMillis()).toString()
    val renderedDate = remember(todayLabel) { renderDateImage(context, todayLabel) }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .gradientCardBackground(gradient)
            .padding(gradientCardPadding)
            .clickable(actionStartActivity(continueTodaysDailyIntent(context)))
    ) {
        GradientCardHeader(
            context = context,
            extraSideInset = headerSideInset,
            dateContent = renderedDate?.let { date ->
                {
                    Image(
                        provider = ImageProvider(date.bitmap),
                        contentDescription = todayLabel,
                        modifier = GlanceModifier.width(date.widthInDp.dp).height(date.heightInDp.dp)
                    )
                }
            }
        )

        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.Center
        ) {
            if (renderedQuote != null) {
                Image(
                    provider = ImageProvider(renderedQuote.bitmap),
                    contentDescription = listOfNotNull(shownQuote, quoteAuthor).joinToString(", "),
                    modifier = GlanceModifier
                        .width(renderedQuote.widthInDp.dp)
                        .height(renderedQuote.heightInDp.dp)
                )
            }
        }
    }
}

private class RenderedText(val bitmap: Bitmap, val widthInDp: Float, val heightInDp: Float)

private fun renderQuoteImage(context: Context, quote: String, author: String?, widgetSize: DpSize): RenderedText? =
    try {
        val displayMetrics = context.resources.displayMetrics
        val pixelsPerDp = displayMetrics.density
        val pixelsPerSp = displayMetrics.density * context.resources.configuration.fontScale

        val availableWidthInDp = widgetSize.width.value - gradientCardPadding.value * 2
        val availableHeightInDp = widgetSize.height.value - gradientCardPadding.value * 2 - headerRowHeightInDp
        val availableWidthInPixels = (availableWidthInDp * pixelsPerDp).toInt().coerceAtLeast(1)
        val availableHeightInPixels = (availableHeightInDp * pixelsPerDp).toInt().coerceAtLeast(1)

        val quoteTypeface = Typeface.create(ResourcesCompat.getFont(context, R.font.lora_medium), Typeface.ITALIC)
        val authorTypeface = ResourcesCompat.getFont(context, R.font.lora_regular)

        val authorLayout = author?.takeIf { it.isNotBlank() }?.let { authorName ->
            val authorPaint = textPaint(authorTypeface, authorFontSizeInSp * pixelsPerSp, authorTextColor)
            buildCenteredLayout(authorName, authorPaint, availableWidthInPixels, maximumLines = 1, lineSpacing = 1f)
        }
        val gapInPixels = if (authorLayout != null) gapBetweenQuoteAndAuthorInDp * pixelsPerDp else 0f
        val heightLeftForQuote = availableHeightInPixels - gapInPixels - (authorLayout?.height ?: 0)

        fun quoteLayoutAt(fontSizeInSp: Float) = buildCenteredLayout(
            quote,
            textPaint(quoteTypeface, fontSizeInSp * pixelsPerSp, quoteTextColor),
            availableWidthInPixels,
            maximumQuoteLines,
            quoteLineSpacingMultiplier
        )

        val quoteLayout = quoteFontSizeStepsInSp
            .asSequence()
            .map(::quoteLayoutAt)
            .firstOrNull { layout -> layout.fitsWithoutCutting() && layout.height <= heightLeftForQuote }
            ?: quoteLayoutAt(quoteFontSizeStepsInSp.last())

        val totalHeightInPixels = (quoteLayout.height + gapInPixels + (authorLayout?.height ?: 0)).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(availableWidthInPixels, totalHeightInPixels, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        quoteLayout.draw(canvas)
        if (authorLayout != null) {
            canvas.translate(0f, quoteLayout.height + gapInPixels)
            authorLayout.draw(canvas)
        }

        RenderedText(
            bitmap = bitmap,
            widthInDp = availableWidthInPixels / pixelsPerDp,
            heightInDp = totalHeightInPixels / pixelsPerDp
        )
    } catch (cause: Exception) {
        WidgetLog.e("Could not draw the quote for the widget", cause)
        null
    }

private fun renderDateImage(context: Context, dateLabel: String): RenderedText? =
    try {
        val displayMetrics = context.resources.displayMetrics
        val pixelsPerDp = displayMetrics.density
        val pixelsPerSp = displayMetrics.density * context.resources.configuration.fontScale
        val datePaint = textPaint(
            ResourcesCompat.getFont(context, R.font.lora_regular),
            dateFontSizeInSp * pixelsPerSp,
            dateTextColor
        )
        val widthInPixels = datePaint.measureText(dateLabel).toInt().coerceAtLeast(1) + 2
        val fontMetrics = datePaint.fontMetrics
        val heightInPixels = (fontMetrics.descent - fontMetrics.ascent).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(widthInPixels, heightInPixels, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawText(dateLabel, 0f, -fontMetrics.ascent, datePaint)

        RenderedText(
            bitmap = bitmap,
            widthInDp = widthInPixels / pixelsPerDp,
            heightInDp = heightInPixels / pixelsPerDp
        )
    } catch (cause: Exception) {
        WidgetLog.e("Could not draw the date for the quote widget", cause)
        null
    }

private fun textPaint(typeface: Typeface?, textSizeInPixels: Float, color: Int): TextPaint =
    TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        this.textSize = textSizeInPixels
        this.color = color
    }

private fun buildCenteredLayout(
    text: String,
    paint: TextPaint,
    widthInPixels: Int,
    maximumLines: Int,
    lineSpacing: Float
): StaticLayout =
    StaticLayout.Builder.obtain(text, 0, text.length, paint, widthInPixels)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(0f, lineSpacing)
        .setMaxLines(maximumLines)
        .setEllipsize(TextUtils.TruncateAt.END)
        .setIncludePad(false)
        .build()

private fun StaticLayout.fitsWithoutCutting(): Boolean =
    lineCount == 0 || getEllipsisCount(lineCount - 1) == 0
