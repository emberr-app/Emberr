// The shared setup screen for gradient widgets, opened when one is added or reconfigured, for choosing its background gradient.
package com.emberr.presentation.widget.gradient

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource as androidPainterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.topHeaderBarPadding
import com.emberr.presentation.widget.WidgetLog
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.ui.theme.EmberrTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.check
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.koin.android.ext.android.get

private val swatchHeight = 112.dp
private val swatchCornerRadius = 20.dp

abstract class GradientWidgetSetupActivity : ComponentActivity() {

    protected abstract fun widgetToRedraw(): GlanceAppWidget

    private var selectedGradient by mutableStateOf(WidgetGradient.defaultGradient)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED, resultIntentFor(appWidgetId))

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        loadSavedGradient(appWidgetId)
        enableEdgeToEdge()

        val interfaceScalePercent = get<SettingsManager>().getInterfaceScalePercent()
        setContent {
            EmberrTheme(interfaceScalePercent = interfaceScalePercent) {
                GradientChooser(
                    selectedGradient = selectedGradient,
                    onGradientChosen = { gradient -> applyGradient(appWidgetId, gradient) },
                    onClose = { finish() }
                )
            }
        }
    }

    private fun loadSavedGradient(appWidgetId: Int) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val glanceId = GlanceAppWidgetManager(this@GradientWidgetSetupActivity).getGlanceIdBy(appWidgetId)
                val savedName = getAppWidgetState(
                    this@GradientWidgetSetupActivity,
                    PreferencesGlanceStateDefinition,
                    glanceId
                )[widgetGradientKey]
                withContext(Dispatchers.Main) { selectedGradient = WidgetGradient.named(savedName) }
            } catch (cause: Exception) {
                WidgetLog.e("Could not read the widget's gradient", cause)
            }
        }
    }

    private fun applyGradient(appWidgetId: Int, gradient: WidgetGradient) {
        selectedGradient = gradient
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val glanceId = GlanceAppWidgetManager(this@GradientWidgetSetupActivity).getGlanceIdBy(appWidgetId)
                updateAppWidgetState(
                    context = this@GradientWidgetSetupActivity,
                    definition = PreferencesGlanceStateDefinition,
                    glanceId = glanceId
                ) { preferences ->
                    preferences.toMutablePreferences().apply { this[widgetGradientKey] = gradient.storageName }
                }
                widgetToRedraw().update(this@GradientWidgetSetupActivity, glanceId)
                withContext(Dispatchers.Main) { setResult(RESULT_OK, resultIntentFor(appWidgetId)) }
            } catch (cause: Exception) {
                WidgetLog.e("Could not save the widget's gradient", cause)
            }
            withContext(Dispatchers.Main) { finish() }
        }
    }

    private fun resultIntentFor(appWidgetId: Int): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}

@Composable
private fun GradientChooser(
    selectedGradient: WidgetGradient,
    onGradientChosen: (WidgetGradient) -> Unit,
    onClose: () -> Unit
) {
    val hazeState = remember { HazeState() }
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val headerHeight = with(density) { headerHeightPx.toDp() }
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val gridState = rememberLazyGridState()
    val headerFadeAlpha by remember {
        derivedStateOf {
            val isScrolled = gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 0
            if (isScrolled) 1f else 0f
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = headerHeight + 8.dp,
                bottom = navigationBarPadding + 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(items = WidgetGradient.entries, key = { gradient -> gradient.storageName }) { gradient ->
                GradientSwatch(
                    gradient = gradient,
                    isSelected = gradient == selectedGradient,
                    onClick = { onGradientChosen(gradient) }
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .onGloballyPositioned { coordinates -> headerHeightPx = coordinates.size.height.toFloat() }
        ) {
            EmberrTopHeaderBar(
                title = "Choose a gradient",
                hazeState = hazeState,
                topEdgeFadeAlpha = headerFadeAlpha,
                contentPadding = topHeaderBarPadding(bottom = 16.dp),
                onBackClick = onClose
            )
        }
    }
}

@Composable
private fun GradientSwatch(gradient: WidgetGradient, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(swatchCornerRadius))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(swatchHeight)
                .clip(RoundedCornerShape(swatchCornerRadius))
                .paint(
                    painter = androidPainterResource(gradient.drawableResource),
                    contentScale = ContentScale.FillBounds
                )
                .padding(12.dp)
        ) {
            Text(
                text = "What's on your mind?",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.check),
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = gradient.displayName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )
    }
}
