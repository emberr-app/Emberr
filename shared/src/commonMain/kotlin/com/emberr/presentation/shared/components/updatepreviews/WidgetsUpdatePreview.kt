package com.emberr.presentation.shared.components.updatepreviews

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
private val DuskGradientColors = Color(0xFF4F5B8A) to Color(0xFF4F7F8A)
private val CopperGradientColors = Color(0xFF7A2E2A) to Color(0xFFB8642F)
private val LightWallpaperColors = listOf(Color(0xFFCFCFD4), Color(0xFF9EA0A8))
private val DarkWallpaperColors = listOf(Color(0xFF2A2B31), Color(0xFF0D0D10))
private const val WIDGET_COUNT = 3

@Composable
internal fun WidgetsUpdatePreview() {
    var shownWidgetCount by remember { mutableIntStateOf(0) }
    var areWidgetsLeaving by remember { mutableStateOf(false) }
    var jotTapCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            areWidgetsLeaving = false
            shownWidgetCount = 0
            delay(700)
            repeat(WIDGET_COUNT) {
                shownWidgetCount++
                delay(600)
            }
            delay(1000)
            jotTapCount++
            delay(2600)
            areWidgetsLeaving = true
            delay(650)
        }
    }

    val todayLabel = remember { shortTodayLabel() }
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

private fun shortTodayLabel(): String {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val monthName = today.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$monthName ${today.day}"
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

private fun gradientCardBrush(colors: Pair<Color, Color>): Brush =
    Brush.verticalGradient(0f to colors.first, 0.3f to colors.first, 1f to colors.second)

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
                text = "The secret of getting ahead is getting started.",
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
    val coverBandTint = if (LocalAppIsDark.current) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
    val coverBandHeight = SmallWidgetHeight - NoteCardTitleTopPadding - NoteCardTitleLineHeight - NoteCardBottomPadding

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SmallWidgetHeight)
            .clip(WidgetShape)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(coverBandHeight)
                .background(coverBandTint)
        )
        Text(
            text = "📚",
            style = TextStyle(fontSize = 24.sp),
            modifier = Modifier.padding(start = NoteCardSidePadding - 2.dp, top = coverBandHeight - NoteCardIconOverhang - 6.dp)
        )
        Text(
            text = "Reading list",
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
            text = "What's on your mind?",
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
            text = "Jot it down",
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
