package com.emberr.presentation.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.components.IconTooltipPosition
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.BOTTOM_BAR_BOTTOM_PADDING
import com.emberr.presentation.BottomBarSearchField
import com.emberr.presentation.EXPANDED_BOTTOM_BAR_PILL_HEIGHT
import com.emberr.presentation.search.defaultHighlightStyle
import com.emberr.presentation.search.highlightMatches
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.EmberrShadowElevation
import com.emberr.presentation.shared.components.NoRippleIndicationNodeFactory
import com.emberr.presentation.shared.components.customEmberrShadow
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.topHeaderBarPadding
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.refresh_cw
import emberr.shared.generated.resources.search
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource

private val CalendarSearchBarReservedHeight = EXPANDED_BOTTOM_BAR_PILL_HEIGHT + BOTTOM_BAR_BOTTOM_PADDING
private val SEARCH_BUTTON_SIZE = 44.dp
private val CLOSED_SEARCH_ICON_SIZE = 22.dp
private val OPEN_SEARCH_ICON_SIZE = 18.dp
private val OPEN_SEARCH_ICON_START_OFFSET = 20.dp

@Composable
internal fun calendarSearchBarBottomPadding(): Dp =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + CalendarSearchBarReservedHeight

@Composable
internal fun CalendarSearchBar(
    isOpen: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenClick: () -> Unit,
    onCloseClick: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val keyboardInsets = WindowInsets.ime
    val navigationBarInsets = WindowInsets.navigationBars
    val headerEndPadding = topHeaderBarPadding().calculateEndPadding(LocalLayoutDirection.current)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .offset {
                val keyboardLift = keyboardInsets.getBottom(this) - navigationBarInsets.getBottom(this)
                IntOffset(0, -keyboardLift.coerceAtLeast(0))
            }
            .navigationBarsPadding()
            .padding(start = 16.dp, end = headerEndPadding, bottom = BOTTOM_BAR_BOTTOM_PADDING),
        contentAlignment = Alignment.BottomEnd
    ) {
        val openWidth = maxWidth
        val morphAnimationSpec = tween<Dp>(durationMillis = 350, easing = FastOutSlowInEasing)
        val barWidth by animateDpAsState(
            targetValue = if (isOpen) openWidth else SEARCH_BUTTON_SIZE,
            animationSpec = morphAnimationSpec
        )
        val barHeight by animateDpAsState(
            targetValue = if (isOpen) EXPANDED_BOTTOM_BAR_PILL_HEIGHT else SEARCH_BUTTON_SIZE,
            animationSpec = morphAnimationSpec
        )

        val searchIconStartOffset by animateDpAsState(
            targetValue = if (isOpen) OPEN_SEARCH_ICON_START_OFFSET else (SEARCH_BUTTON_SIZE - CLOSED_SEARCH_ICON_SIZE) / 2,
            animationSpec = morphAnimationSpec
        )
        val searchIconSize by animateDpAsState(
            targetValue = if (isOpen) OPEN_SEARCH_ICON_SIZE else CLOSED_SEARCH_ICON_SIZE,
            animationSpec = morphAnimationSpec
        )
        val searchIconAlpha by animateFloatAsState(
            targetValue = if (isOpen) 0.6f else 1f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
        )

        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier
                .padding(bottom = (EXPANDED_BOTTOM_BAR_PILL_HEIGHT - barHeight) / 2)
                .size(width = barWidth, height = barHeight)
                .emberrBorder(CircleShape)
                .customEmberrShadow(CircleShape, elevation = EmberrShadowElevation.Standard)
                .clip(CircleShape)
                .emberrBlur(hazeState, EmberrBlur.Regular)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = isOpen,
                    enter = fadeIn(tween(durationMillis = 200, delayMillis = 100)),
                    exit = fadeOut(tween(durationMillis = 150))
                ) {
                    Box(
                        modifier = Modifier
                            .wrapContentSize(Alignment.CenterStart, unbounded = true)
                            .size(width = openWidth, height = EXPANDED_BOTTOM_BAR_PILL_HEIGHT)
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        BottomBarSearchField(
                            query = query,
                            onQueryChange = onQueryChange,
                            onCloseClick = onCloseClick,
                            placeholder = "Search events",
                            isSearchIconVisible = false
                        )
                    }
                }

                Icon(
                    painter = painterResource(Res.drawable.search),
                    contentDescription = if (isOpen) null else "Search events",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = searchIconStartOffset)
                        .size(searchIconSize)
                        .alpha(searchIconAlpha)
                )

                if (!isOpen) {
                    Box(modifier = Modifier.matchParentSize()) {
                        IconTooltip(text = "Search events", position = IconTooltipPosition.Above) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = NoRippleIndicationNodeFactory,
                                        onClick = onOpenClick
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CalendarSearchResultsList(
    query: String,
    results: CalendarSearchResults?,
    categories: List<CalendarCategory>,
    hazeState: HazeState,
    topPadding: Dp,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val trimmedQuery = query.trim()

    Box(
        modifier = modifier
            .fillMaxSize()
            .hazeSource(state = hazeState, zIndex = 1f)
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
            .imePadding()
            .navigationBarsPadding()
            .padding(bottom = CalendarSearchBarReservedHeight)
    ) {
        when {
            results == null -> Unit
            results.isEmpty -> Text(
                text = "No matching events found.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp)
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = topPadding + 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                searchResultSection(
                    events = results.upcomingEvents,
                    keyPrefix = "upcoming",
                    query = trimmedQuery,
                    categories = categories,
                    onEventClick = onEventClick
                )
                if (results.pastEvents.isNotEmpty()) {
                    item(key = "past_header") {
                        Text(
                            text = "Past",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 16.dp)
                        )
                    }
                }
                searchResultSection(
                    events = results.pastEvents,
                    keyPrefix = "past",
                    query = trimmedQuery,
                    categories = categories,
                    onEventClick = onEventClick
                )
            }
        }
    }
}

private fun LazyListScope.searchResultSection(
    events: List<CalendarEvent>,
    keyPrefix: String,
    query: String,
    categories: List<CalendarCategory>,
    onEventClick: (CalendarEvent) -> Unit
) {
    events.groupBy { it.dateString }.forEach { (dateString, eventsOnDate) ->
        item(key = "${keyPrefix}_date_$dateString") {
            Text(
                text = formatFullDate(LocalDate.parse(dateString)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 4.dp, top = 10.dp)
            )
        }
        items(eventsOnDate, key = { event -> "${keyPrefix}_${event.blockId}_${event.dateString}" }) { event ->
            CalendarSearchResultRow(
                event = event,
                category = categories.firstOrNull { it.id == event.categoryId },
                query = query,
                onClick = { onEventClick(event) }
            )
        }
    }
}

@Composable
private fun CalendarSearchResultRow(
    event: CalendarEvent,
    category: CalendarCategory?,
    query: String,
    onClick: () -> Unit
) {
    val highlightStyle = defaultHighlightStyle(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    val matchingDetail = event.matchingDetail(query)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    category?.colorHex?.toCategoryColor() ?: MaterialTheme.colorScheme.outline,
                    CircleShape
                )
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                text = highlightMatches(event.text.ifBlank { "Untitled event" }, query, highlightStyle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            matchingDetail?.let { detail ->
                Text(
                    text = highlightMatches(detail, query, highlightStyle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (event.recurrenceRule != null) {
            Icon(
                painter = painterResource(Res.drawable.refresh_cw),
                contentDescription = "Repeats",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = event.timeOfDayLabel(LocalUsesTwentyFourHourClock.current),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
