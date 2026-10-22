package com.emberr.presentation.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.time.FirstDayOfWeekPreference
import com.emberr.presentation.settings.SettingsChoice
import com.emberr.presentation.settings.SettingsChoiceRow
import com.emberr.presentation.settings.SettingsGroup
import com.emberr.presentation.settings.SettingsRowIcon
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.KmpBackHandler
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.TopHeaderBarButtonSize
import com.emberr.presentation.shared.components.TopHeaderTitlePlacement
import com.emberr.presentation.shared.components.topHeaderBarPadding
import com.emberr.ui.theme.LocalAppIsDark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.calendar
import emberr.shared.generated.resources.chevron_right
import emberr.shared.generated.resources.sidebar
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private val SidebarShape = RoundedCornerShape(22.dp)
private val SidebarEdgePadding = 12.dp
private val SidebarRightEdgePadding = 6.dp
private val SidebarMaxWidth = 340.dp
private const val SidebarWidthFraction = 0.9f
private val SidebarSwipeCloseDistance = 80.dp
private const val SidebarScrimAlpha = 0.25f
private const val DesktopLightCardTintAlpha = 0.05f
private val SidebarContentHorizontalPadding = 8.dp
private val SidebarHeaderBlurHeight = 90.dp
private val SidebarHeaderGradientHeight = 110.dp

@Composable
fun CalendarSidebar(
    title: String,
    isOpen: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    KmpBackHandler(enabled = isOpen, onBack = onClose)

    val coroutineScope = rememberCoroutineScope()
    val swipeOffsetPx = remember { Animatable(0f) }
    val swipeCloseDistancePx = with(LocalDensity.current) { SidebarSwipeCloseDistance.toPx() }
    val panelColor = if (LocalAppIsDark.current) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background

    LaunchedEffect(isOpen) {
        if (isOpen) swipeOffsetPx.snapTo(0f)
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sidebarWidth = minOf(maxWidth * SidebarWidthFraction, SidebarMaxWidth)

        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = SidebarScrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
            )
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { it },
            exit = slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { it },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(swipeOffsetPx.value.roundToInt(), 0) }
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(
                        start = SidebarEdgePadding,
                        top = SidebarEdgePadding,
                        bottom = SidebarEdgePadding,
                        end = SidebarRightEdgePadding
                    )
                    .width(sidebarWidth)
                    .fillMaxHeight()
                    .emberrBorder(SidebarShape)
                    .clip(SidebarShape)
                    .background(panelColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { dragAmount ->
                            coroutineScope.launch {
                                swipeOffsetPx.snapTo((swipeOffsetPx.value + dragAmount).coerceAtLeast(0f))
                            }
                        },
                        onDragStopped = {
                            if (swipeOffsetPx.value > swipeCloseDistancePx) {
                                onClose()
                            } else {
                                swipeOffsetPx.animateTo(0f)
                            }
                        }
                    )
            ) {
                val panelHazeState = remember { HazeState() }
                val panelScrollState = rememberScrollState()
                var headerHeightPx by remember { mutableFloatStateOf(0f) }
                val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }
                val headerBlurStyle = EmberrBlur.Regular.copy(
                    backgroundColor = panelColor,
                    tints = listOf(HazeTint(panelColor.copy(alpha = 0.45f))),
                    fallbackTint = HazeTint(panelColor)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(state = panelHazeState)
                        .verticalScroll(panelScrollState)
                        .padding(horizontal = SidebarContentHorizontalPadding)
                        .padding(top = headerHeight, bottom = 24.dp),
                    content = content
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates -> headerHeightPx = coordinates.size.height.toFloat() }
                ) {
                    EmberrTopHeaderBar(
                        title = title,
                        titlePlacement = TopHeaderTitlePlacement.Start,
                        showBackButton = false,
                        reserveBackButtonSpace = false,
                        topEdgeColor = panelColor,
                        topEdgeBlurHeight = SidebarHeaderBlurHeight,
                        topEdgeGradientHeight = SidebarHeaderGradientHeight,
                        flatFadeHeight = headerHeight + 4.dp,
                        hazeState = panelHazeState,
                        hazeStyle = headerBlurStyle,
                        applyStatusBarPadding = false,
                        contentPadding = topHeaderBarPadding(
                            top = 8.dp,
                            horizontal = SidebarContentHorizontalPadding + 16.dp,
                            bottom = 8.dp
                        ),
                        actions = {
                            IconTooltip(text = "Close sidebar") {
                                Box(
                                    modifier = Modifier
                                        .size(TopHeaderBarButtonSize)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = onClose
                                        ),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.sidebar),
                                        contentDescription = "Close sidebar",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarSettingsContent(
    firstDayOfWeekPreference: FirstDayOfWeekPreference,
    onFirstDayOfWeekPreferenceChange: (FirstDayOfWeekPreference) -> Unit,
    categories: List<CalendarCategory>,
    onAddCategory: (name: String, colorHex: String) -> Unit,
    onUpdateCategory: (id: String, name: String, colorHex: String) -> Unit,
    onDeleteCategory: (id: String) -> Unit,
    hiddenCategoryVisibilityKeys: Set<String>,
    onToggleCategoryVisibility: (visibilityKey: String) -> Unit,
    spaces: List<SpaceEntity>,
    modifier: Modifier = Modifier,
    viewMode: CalendarViewMode? = null,
    onViewModeChange: (CalendarViewMode) -> Unit = {}
) {
    var showFirstDayOfWeekOptions by remember { mutableStateOf(false) }
    val cardColor = when {
        LocalAppIsDark.current -> MaterialTheme.colorScheme.surfaceVariant
        !isDesktopPlatform -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = DesktopLightCardTintAlpha)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (viewMode != null) {
            SettingsGroup(title = "Views", cardColor = cardColor) {
                calendarViewModeLabels.forEachIndexed { index, (mode, label) ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 14.dp, end = 16.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                    CalendarViewModeRow(
                        label = label,
                        isSelected = mode == viewMode,
                        onClick = { onViewModeChange(mode) }
                    )
                }
            }
        }

        CategorySection(
            categories = categories,
            onAddCategory = onAddCategory,
            onUpdateCategory = onUpdateCategory,
            onDeleteCategory = onDeleteCategory,
            hiddenCategoryVisibilityKeys = hiddenCategoryVisibilityKeys,
            onToggleCategoryVisibility = onToggleCategoryVisibility,
            spaces = spaces,
            cardColor = cardColor
        )

        SettingsGroup(title = "General", cardColor = cardColor) {
            if (isDesktopPlatform) {
                SettingsChoiceRow(
                    icon = painterResource(Res.drawable.calendar),
                    title = "First Day of Week",
                    value = firstDayOfWeekPreference.displayName,
                    choices = FirstDayOfWeekPreference.entries.map { option ->
                        SettingsChoice(label = option.displayName, isSelected = option == firstDayOfWeekPreference) {
                            onFirstDayOfWeekPreferenceChange(option)
                        }
                    }
                )
            } else {
                FirstDayOfWeekStackedRow(
                    selectedLabel = firstDayOfWeekPreference.displayName,
                    onClick = { showFirstDayOfWeekOptions = true }
                )
            }
        }
    }

    if (!isDesktopPlatform) {
        EmberrBottomSheet(
            expanded = showFirstDayOfWeekOptions,
            onDismiss = { showFirstDayOfWeekOptions = false },
            title = "First Day of Week",
            contentHorizontalPadding = 0.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                FirstDayOfWeekPreference.entries.forEach { option ->
                    EmberrBottomSheetOption(
                        label = option.displayName,
                        isSelected = option == firstDayOfWeekPreference,
                        onClick = {
                            onFirstDayOfWeekPreferenceChange(option)
                            showFirstDayOfWeekOptions = false
                        }
                    )
                }

                EmberrButtonPrimary(
                    text = "Close",
                    onClick = { showFirstDayOfWeekOptions = false },
                    modifier = Modifier.fillMaxWidth()
                        .padding(top = 12.dp, start = 20.dp, end = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun CalendarViewModeRow(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
private fun FirstDayOfWeekStackedRow(selectedLabel: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowIcon(icon = painterResource(Res.drawable.calendar))

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "First Day of Week",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            painter = painterResource(Res.drawable.chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            modifier = Modifier.size(18.dp)
        )
    }
}
