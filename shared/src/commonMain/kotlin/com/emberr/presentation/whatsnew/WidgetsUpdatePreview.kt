package com.emberr.presentation.whatsnew

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emberr.ui.theme.FontStylePreference
import com.emberr.ui.theme.LocalAppIsDark
import com.emberr.ui.theme.fontFamilyFor
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.emberr_ghost
import emberr.shared.generated.resources.pen
import emberr.shared.generated.resources.star
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import emberr.shared.generated.resources.whatsnew_preview_jot_button
import emberr.shared.generated.resources.whatsnew_preview_jot_prompt
import emberr.shared.generated.resources.whatsnew_preview_quote
import emberr.shared.generated.resources.whatsnew_preview_reading_list
import org.jetbrains.compose.resources.stringResource
import com.emberr.domain.util.time.formatDate
import com.emberr.domain.util.time.DateTextStyle

private val WidgetShape = RoundedCornerShape(24.dp)
private val GradientCardPadding = 18.dp
private val QuoteWidgetHeight = 170.dp
private val SmallWidgetHeight = 182.dp
private val JotButtonHeight = 42.dp
private val NoteCardTitleTopPadding = 27.dp
private val NoteCardTitleLineHeight = 25.dp
private val NoteCardBottomPadding = 18.dp
private val NoteCardSidePadding = 18.dp
private val NoteCardIconOverhang = 12.dp
private val OnGradientTextColor = Color.White
private val JotButtonGlassColor = Color(0x33FFFFFF)
private val JotTapColor = Color(0x66FFFFFF)
private val DuskGradientColors = GradientColors(top = Color(0xFF4F5B8A), bottom = Color(0xFF4F7F8A))
private val CopperGradientColors = GradientColors(top = Color(0xFF7A2E2A), bottom = Color(0xFFB8642F))
private val CoverSkyTopColor = Color(0xFFCF8A74)
private val CoverSkyMiddleColor = Color(0xFFEDB08C)
private val CoverHorizonColor = Color(0xFFF8DCC0)
private val CoverSunColor = Color(0xFFFFF4E4)
private val CoverFarthestRidgeColor = Color(0xFFE2A48A)
private val CoverFarRidgeColor = Color(0xFFC88173)
private val CoverMiddleRidgeColor = Color(0xFF9C5E62)
private val CoverNearRidgeColor = Color(0xFF6B4150)
private val CoverFarthestRidgeHeights = listOf(0.54f, 0.47f, 0.51f, 0.43f, 0.49f, 0.45f, 0.52f)
private val CoverFarRidgeHeights = listOf(0.64f, 0.58f, 0.63f, 0.55f, 0.61f, 0.66f)
private val CoverMiddleRidgeHeights = listOf(0.73f, 0.79f, 0.69f, 0.75f, 0.7f)
private val CoverNearRidgeHeights = listOf(0.87f, 0.82f, 0.9f, 0.84f)
private val LightWallpaperColors = listOf(Color(0xFFCFCFD4), Color(0xFF9EA0A8))
private val DarkWallpaperColors = listOf(Color(0xFF2A2B31), Color(0xFF0D0D10))
private const val WIDGET_COUNT = 3

private class GradientColors(val top: Color, val bottom: Color)

@Composable
internal fun WidgetsUpdatePreview() {
    var shownWidgetCount by remember { mutableIntStateOf(0) }
    var areWidgetsLeaving by remember { mutableStateOf(false) }
    var jotTapCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            areWidgetsLeaving = false
            shownWidgetCount = 0
            delay(700.milliseconds)
            repeat(WIDGET_COUNT) {
                shownWidgetCount++
                delay(600.milliseconds)
            }
            delay(1000.milliseconds)
            jotTapCount++
            delay(2600.milliseconds)
            areWidgetsLeaving = true
            delay(650.milliseconds)
        }
    }

    val todayLabel = remember { formatDate(Clock.System.todayIn(TimeZone.currentSystemDefault()), DateTextStyle.SHORT_MONTH_DAY) }
    val wallpaperColors = if (LocalAppIsDark.current) DarkWallpaperColors else LightWallpaperColors

    UpdatePreviewFrame(fitsContentHeight = true) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(wallpaperColors))
                .padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PoppingWidget(isShown = shownWidgetCount > 0 && !areWidgetsLeaving) {
                QuoteWidgetCopy(todayLabel = todayLabel)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PoppingWidget(isShown = shownWidgetCount > 1 && !areWidgetsLeaving, modifier = Modifier.weight(1f)) {
                    NoteCardWidgetCopy()
                }
                PoppingWidget(isShown = shownWidgetCount > 2 && !areWidgetsLeaving, modifier = Modifier.weight(1f)) {
                    JotWidgetCopy(todayLabel = todayLabel, tapCount = jotTapCount)
                }
            }
        }
    }
}

@Composable
private fun PoppingWidget(isShown: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val widgetAlpha by animateFloatAsState(
        targetValue = if (isShown) 1f else 0f,
        animationSpec = tween(if (isShown) 300 else 450),
        label = "widget_preview_alpha"
    )
    val widgetScale by animateFloatAsState(
        targetValue = if (isShown) 1f else 0.85f,
        animationSpec = if (isShown) {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        } else {
            tween(450)
        },
        label = "widget_preview_scale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            alpha = widgetAlpha
            scaleX = widgetScale
            scaleY = widgetScale
        }
    ) {
        content()
    }
}

private fun gradientCardBrush(colors: GradientColors): Brush =
    Brush.verticalGradient(0f to colors.top, 0.3f to colors.top, 1f to colors.bottom)

@Composable
private fun GradientCardHeader(todayLabel: String, dateFontFamily: FontFamily, sideInset: Dp = 0.dp) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = sideInset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.emberr_ghost),
            contentDescription = null,
            tint = OnGradientTextColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = todayLabel,
            style = TextStyle(fontFamily = dateFontFamily, fontSize = 13.sp, color = OnGradientTextColor.copy(alpha = 0.9f)),
            modifier = Modifier.padding(end = 2.dp)
        )
    }
}

@Composable
private fun QuoteWidgetCopy(todayLabel: String) {
    val loraFontFamily = fontFamilyFor(FontStylePreference.LORA)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(QuoteWidgetHeight)
            .clip(WidgetShape)
            .background(gradientCardBrush(DuskGradientColors))
            .padding(GradientCardPadding)
    ) {
        GradientCardHeader(todayLabel = todayLabel, dateFontFamily = loraFontFamily, sideInset = 4.dp)
        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(Res.string.whatsnew_preview_quote),
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontFamily = loraFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontStyle = FontStyle.Italic,
                    fontSize = 18.sp,
                    color = OnGradientTextColor
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Mark Twain",
                style = TextStyle(fontFamily = loraFontFamily, fontSize = 13.sp, color = OnGradientTextColor.copy(alpha = 0.75f))
            )
        }
    }
}

@Composable
private fun NoteCardWidgetCopy() {
    val coverBandHeight = SmallWidgetHeight - NoteCardTitleTopPadding - NoteCardTitleLineHeight - NoteCardBottomPadding

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SmallWidgetHeight)
            .clip(WidgetShape)
            .background(MaterialTheme.colorScheme.background)
    ) {
        SunsetCoverPicture(
            modifier = Modifier
                .fillMaxWidth()
                .height(coverBandHeight)
        )
        Text(
            text = "📚",
            style = TextStyle(fontSize = 24.sp),
            modifier = Modifier.padding(start = NoteCardSidePadding - 2.dp, top = coverBandHeight - NoteCardIconOverhang - 6.dp)
        )
        Text(
            text = stringResource(Res.string.whatsnew_preview_reading_list),
            maxLines = 1,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = NoteCardSidePadding, end = NoteCardSidePadding, bottom = NoteCardBottomPadding)
        )
        Icon(
            painter = painterResource(Res.drawable.star),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(14.dp)
                .size(14.dp)
        )
    }
}

@Composable
private fun SunsetCoverPicture(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(Brush.verticalGradient(listOf(CoverSkyTopColor, CoverSkyMiddleColor, CoverHorizonColor)))
        val sunCenter = Offset(size.width * 0.68f, size.height * 0.5f)
        val sunRadius = size.height * 0.13f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(CoverSunColor.copy(alpha = 0.6f), Color.Transparent),
                center = sunCenter,
                radius = sunRadius * 4f
            ),
            radius = sunRadius * 4f,
            center = sunCenter
        )
        drawCircle(color = CoverSunColor, radius = sunRadius, center = sunCenter)
        drawRidge(CoverFarthestRidgeColor, CoverFarthestRidgeHeights)
        drawRidge(CoverFarRidgeColor, CoverFarRidgeHeights)
        drawRect(
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, CoverHorizonColor.copy(alpha = 0.4f)),
                startY = size.height * 0.5f,
                endY = size.height * 0.8f
            )
        )
        drawRidge(CoverMiddleRidgeColor, CoverMiddleRidgeHeights)
        drawRidge(CoverNearRidgeColor, CoverNearRidgeHeights)
    }
}

private fun DrawScope.drawRidge(color: Color, heights: List<Float>) {
    val points = heights.mapIndexed { index, height ->
        Offset(size.width * index / (heights.size - 1), size.height * height)
    }
    val ridge = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.zipWithNext { current, next ->
            val middle = Offset((current.x + next.x) / 2f, (current.y + next.y) / 2f)
            quadraticTo(current.x, current.y, middle.x, middle.y)
        }
        lineTo(points.last().x, points.last().y)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(ridge, color)
}

@Composable
private fun JotWidgetCopy(todayLabel: String, tapCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(SmallWidgetHeight)
            .clip(WidgetShape)
            .background(gradientCardBrush(CopperGradientColors))
            .padding(GradientCardPadding)
    ) {
        GradientCardHeader(todayLabel = todayLabel, dateFontFamily = FontFamily.Default)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(Res.string.whatsnew_preview_jot_prompt),
            maxLines = 2,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = OnGradientTextColor
            ),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        JotButtonCopy(tapCount = tapCount)
    }
}

@Composable
private fun JotButtonCopy(tapCount: Int) {
    val tapProgress = remember { Animatable(1f) }

    LaunchedEffect(tapCount) {
        if (tapCount == 0) return@LaunchedEffect
        tapProgress.snapTo(0f)
        tapProgress.animateTo(1f, tween(650))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(JotButtonHeight)
            .clip(CircleShape)
            .background(JotButtonGlassColor)
            .drawBehind {
                val progress = tapProgress.value
                if (progress < 1f) {
                    drawCircle(
                        color = JotTapColor.copy(alpha = JotTapColor.alpha * (1f - progress)),
                        radius = size.width * 0.6f * progress
                    )
                }
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.pen),
            contentDescription = null,
            tint = OnGradientTextColor,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(Res.string.whatsnew_preview_jot_button),
            maxLines = 1,
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = OnGradientTextColor
            )
        )
    }
}
