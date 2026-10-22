package com.emberr.presentation.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.util.time.daysOfWeekStartingFrom
import com.emberr.domain.util.time.startOfWeek
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.ui.theme.LocalFirstDayOfWeek
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_left
import emberr.shared.generated.resources.chevron_right
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Clock

internal val CalendarSidePanelWidth = 304.dp
internal val CalendarSidePanelContentGap = 12.dp
private val SidePanelHorizontalPadding = 20.dp
private val SidePanelRowShape = RoundedCornerShape(10.dp)
private val SidePanelRowHeight = 38.dp
private val SidePanelRowInset = 10.dp
private const val SidePanelHoverAlpha = 0.055f
private const val MiniMonthWeekCount = 6
private val MiniMonthCellHeight = 38.dp
private val MiniMonthDayNumberSize = 26.dp
private val MiniMonthVisibleDaysCorner = 10.dp
private const val MiniMonthVisibleDaysAlpha = 0.12f

@Composable
internal fun CalendarSidePanel(
    selectedDate: LocalDate,
    visibleDates: List<LocalDate>,
    viewModel: CalendarViewModel,
    categories: List<CalendarCategory>,
    hiddenCategoryVisibilityKeys: Set<String>,
    spaces: List<SpaceEntity>,
    onDateClick: (LocalDate) -> Unit,
    onToggleVisibility: (visibilityKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isAnySpaceHidden = spaces.any { space -> spaceVisibilityKeyFor(space.spaceId) in hiddenCategoryVisibilityKeys }
    var keepsSpacesSectionShown by remember { mutableStateOf(false) }
    if (spaces.size >= 2 || isAnySpaceHidden) keepsSpacesSectionShown = true

    Box(
        modifier = modifier
            .width(CalendarSidePanelWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = SidePanelHorizontalPadding)
                .padding(bottom = 24.dp)
        ) {
            MiniMonthCalendar(
                selectedDate = selectedDate,
                visibleDates = visibleDates,
                viewModel = viewModel,
                categories = categories,
                onDateClick = onDateClick
            )

            SidePanelSectionTitle(title = "Categories")
            categories.forEach { category ->
                SidePanelVisibilityRow(
                    label = category.name,
                    color = category.colorHex.toCategoryColor(),
                    isShown = category.id !in hiddenCategoryVisibilityKeys,
                    onClick = { onToggleVisibility(category.id) }
                )
            }
            SidePanelVisibilityRow(
                label = "No Category",
                color = MaterialTheme.colorScheme.outline,
                isShown = NO_CATEGORY_VISIBILITY_KEY !in hiddenCategoryVisibilityKeys,
                onClick = { onToggleVisibility(NO_CATEGORY_VISIBILITY_KEY) }
            )

            if (keepsSpacesSectionShown && spaces.isNotEmpty()) {
                SidePanelSectionTitle(title = "Spaces")
                spaces.forEach { space ->
                    val spaceVisibilityKey = spaceVisibilityKeyFor(space.spaceId)
                    SidePanelVisibilityRow(
                        label = space.displayName.trim().ifBlank { "Untitled space" },
                        color = MaterialTheme.colorScheme.outline,
                        isShown = spaceVisibilityKey !in hiddenCategoryVisibilityKeys,
                        onClick = { onToggleVisibility(spaceVisibilityKey) }
                    )
                }
            }
        }

        VerticalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}

@Composable
private fun MiniMonthCalendar(
    selectedDate: LocalDate,
    visibleDates: List<LocalDate>,
    viewModel: CalendarViewModel,
    categories: List<CalendarCategory>,
    onDateClick: (LocalDate) -> Unit
) {
    var shownMonth by remember(selectedDate) { mutableStateOf(LocalDate(selectedDate.year, selectedDate.month, 1)) }
    val firstDayOfWeek = LocalFirstDayOfWeek.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = SidePanelRowInset, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatMonthYear(shownMonth),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            MiniMonthArrowButton(
                icon = painterResource(Res.drawable.chevron_left),
                tooltip = "Previous month",
                onClick = { shownMonth = shownMonth.plus(-1, DateTimeUnit.MONTH) }
            )
            Spacer(modifier = Modifier.width(4.dp))
            MiniMonthArrowButton(
                icon = painterResource(Res.drawable.chevron_right),
                tooltip = "Next month",
                onClick = { shownMonth = shownMonth.plus(1, DateTimeUnit.MONTH) }
            )
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            daysOfWeekStartingFrom(firstDayOfWeek).forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = day.name.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (day == DayOfWeek.SUNDAY) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        }
                    )
                }
            }
        }

        AnimatedContent(
            targetState = shownMonth,
            transitionSpec = {
                val direction = if (targetState > initialState) {
                    AnimatedContentTransitionScope.SlideDirection.Left
                } else {
                    AnimatedContentTransitionScope.SlideDirection.Right
                }
                (slideIntoContainer(direction, tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300))) togetherWith
                        (slideOutOfContainer(direction, tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300)))
            },
            label = "MiniMonthTransition"
        ) { month ->
            MiniMonthGrid(
                month = month,
                visibleDates = visibleDates,
                viewModel = viewModel,
                categories = categories,
                onDateClick = onDateClick
            )
        }
    }
}

@Composable
private fun MiniMonthGrid(
    month: LocalDate,
    visibleDates: List<LocalDate>,
    viewModel: CalendarViewModel,
    categories: List<CalendarCategory>,
    onDateClick: (LocalDate) -> Unit
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val firstDayOfWeek = LocalFirstDayOfWeek.current
    val gridDates = remember(month, firstDayOfWeek) {
        val gridStart = month.startOfWeek(firstDayOfWeek)
        (0 until MiniMonthWeekCount * 7).map { offset -> gridStart.plus(offset.toLong(), DateTimeUnit.DAY) }
    }
    val gridEvents by remember(gridDates) { viewModel.eventsInRange(gridDates.first(), gridDates.last()) }
        .collectAsState(initial = emptyList())
    val firstEventByDate = remember(gridEvents) {
        gridEvents
            .sortedBy { event -> event.reminderTimestamp }
            .flatMap { event -> event.datesShownOn().map { date -> date to event } }
            .distinctBy { (date, _) -> date }
            .toMap()
    }
    val visibleDateSet = remember(visibleDates) { visibleDates.toSet() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        gridDates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().height(MiniMonthCellHeight)) {
                week.forEachIndexed { index, date ->
                    val isVisible = date in visibleDateSet
                    val firstEvent = firstEventByDate[date]
                    MiniMonthDayCell(
                        date = date,
                        isInShownMonth = date.month == month.month,
                        isToday = date == today,
                        isVisible = isVisible,
                        joinsPreviousDay = isVisible && index > 0 && week[index - 1] in visibleDateSet,
                        joinsNextDay = isVisible && index < week.lastIndex && week[index + 1] in visibleDateSet,
                        eventColor = firstEvent?.let { event ->
                            categories.firstOrNull { it.id == event.categoryId }?.colorHex?.toCategoryColor()
                                ?: MaterialTheme.colorScheme.outline
                        },
                        onClick = { onDateClick(date) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniMonthDayCell(
    date: LocalDate,
    isInShownMonth: Boolean,
    isToday: Boolean,
    isVisible: Boolean,
    joinsPreviousDay: Boolean,
    joinsNextDay: Boolean,
    eventColor: Color?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val startCorner = if (joinsPreviousDay) 0.dp else MiniMonthVisibleDaysCorner
    val endCorner = if (joinsNextDay) 0.dp else MiniMonthVisibleDaysCorner
    val cellShape = RoundedCornerShape(
        topStart = startCorner,
        bottomStart = startCorner,
        topEnd = endCorner,
        bottomEnd = endCorner
    )
    val cellColor = when {
        isVisible -> MaterialTheme.colorScheme.primary.copy(alpha = MiniMonthVisibleDaysAlpha)
        isHovered -> MaterialTheme.colorScheme.onSurface.copy(alpha = SidePanelHoverAlpha)
        else -> Color.Transparent
    }
    val dayNumberColor = if (date.dayOfWeek == DayOfWeek.SUNDAY) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val outsideMonthAlpha = if (isInShownMonth) 1f else 0.3f

    Column(
        modifier = modifier
            .clip(cellShape)
            .background(cellColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(MiniMonthDayNumberSize)
                .then(
                    if (isToday) Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.day.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isToday) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    dayNumberColor.copy(alpha = outsideMonthAlpha)
                }
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(4.dp)
                .then(
                    if (eventColor != null) Modifier.background(eventColor.copy(alpha = outsideMonthAlpha), CircleShape)
                    else Modifier
                )
        )
    }
}

@Composable
private fun MiniMonthArrowButton(
    icon: Painter,
    tooltip: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    IconTooltip(text = tooltip) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(SidePanelRowShape)
                .background(
                    if (isHovered) MaterialTheme.colorScheme.onSurface.copy(alpha = SidePanelHoverAlpha)
                    else Color.Transparent
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = tooltip,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isHovered) 0.9f else 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SidePanelSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = Modifier.padding(start = SidePanelRowInset, top = 36.dp, bottom = 10.dp)
    )
}

@Composable
private fun SidePanelVisibilityRow(
    label: String,
    color: Color,
    isShown: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val rowColor by animateColorAsState(
        targetValue = if (isHovered) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = SidePanelHoverAlpha)
        } else {
            Color.Transparent
        },
        animationSpec = tween(180, easing = FastOutSlowInEasing)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SidePanelRowHeight)
            .clip(SidePanelRowShape)
            .background(rowColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = SidePanelRowInset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryVisibilityMark(color = color, isShown = isShown)

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isShown) 0.82f else 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}
