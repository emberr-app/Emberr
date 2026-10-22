package com.emberr.presentation.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.emberr.domain.util.eventbus.WidgetCalendarDateBus
import com.emberr.domain.util.eventbus.WidgetCalendarEventBus
import com.emberr.domain.util.eventbus.WidgetComposeRequest
import com.emberr.domain.util.eventbus.WidgetComposeRequestBus
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.model.DEFAULT_ALL_DAY_REMINDER_HOUR
import com.emberr.domain.model.RecurrenceEditScope
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.EmberrShadowElevation
import com.emberr.presentation.shared.components.TopHeaderBarButtonSize
import com.emberr.presentation.shared.components.customEmberrShadow
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.TopEdgeBlurHeight
import com.emberr.presentation.shared.components.TopEdgeGradientHeight
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.topHeaderBarPadding
import com.emberr.presentation.shared.components.EmberrVerticalScrollbar
import com.emberr.presentation.shared.components.KmpBackHandler
import com.emberr.presentation.shared.components.smoothWheelScroll
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.cog
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.sidebar
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.number
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.milliseconds
import com.emberr.domain.util.time.formatHourLabel
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import com.emberr.ui.theme.LocalFirstDayOfWeek
import com.emberr.domain.util.time.daysOfWeekStartingFrom
import com.emberr.domain.util.time.daysSinceStartOfWeek
import com.emberr.domain.util.time.startOfWeek

enum class CalendarViewMode { DAY, THREE_DAY, WEEK, MONTH }

internal val calendarViewModeLabels = listOf(
    CalendarViewMode.DAY to "Day",
    CalendarViewMode.THREE_DAY to "3 Day",
    CalendarViewMode.WEEK to "Week",
    CalendarViewMode.MONTH to "Month"
)

private val CalendarTitleHeight = 52.dp
private val DayCalendarTitleHeight = 72.dp

@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit = {},
    showBackButton: Boolean = true,
    onOpenSourceNote: ((CalendarEvent) -> Unit)? = null,
    viewModel: CalendarViewModel = koinViewModel()
) {
    val internalHazeState = remember { HazeState() }

    var showSidebar by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val viewMode by viewModel.viewMode.collectAsState()
    var selectedDate by remember { mutableStateOf(Clock.System.todayIn(TimeZone.currentSystemDefault())) }
    val categories by viewModel.categories.collectAsState()
    val hiddenCategoryVisibilityKeys by viewModel.hiddenCategoryVisibilityKeys.collectAsState()
    val spaces by viewModel.spaces.collectAsState()
    val firstDayOfWeekPreference by viewModel.firstDayOfWeekPreference.collectAsState()
    val firstDayOfWeek = LocalFirstDayOfWeek.current
    val events by remember(selectedDate) { viewModel.eventsForDate(selectedDate.toString()) }
        .collectAsState(initial = emptyList())
    var eventEditorState by remember { mutableStateOf<EventEditorState?>(null) }
    val shownEvent = eventEditorState?.original
    val sourceNoteTitle by produceState<String?>(null, shownEvent?.blockId) {
        value = shownEvent?.let { event -> viewModel.sourceNoteTitle(event) }
    }
    // Holds the continuation to run once the user picks a scope in RecurrenceScopeChooser -
    // set by whichever of Edit/Delete triggered it, null the rest of the time.
    var pendingScopeAction by remember { mutableStateOf<((RecurrenceEditScope) -> Unit)?>(null) }
    val eventResizeState = remember { EventResizeState() }
    val eventMoveTracker = remember { EventMoveTracker() }
    val allDayDragState = remember { AllDayDragState() }
    LaunchedEffect(selectedDate, viewMode) {
        eventResizeState.eventInResizeMode = null
        allDayDragState.selectedEvent = null
    }
    var slideDirection by remember { mutableStateOf(AnimatedContentTransitionScope.SlideDirection.Left) }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    val areSearchResultsVisible = isSearchOpen && searchQuery.isNotBlank()
    val focusManager = LocalFocusManager.current
    val closeSearch: () -> Unit = {
        focusManager.clearFocus()
        viewModel.onSearchQueryChange("")
        isSearchOpen = false
    }

    val scrollState = remember(viewMode) { ScrollState(0) }
    val topEdgeFadeAlpha by remember(scrollState) {
        derivedStateOf { if (scrollState.value > 0) 1f else 0f }
    }

    val density = LocalDensity.current
    var topBarHeightPx by remember { mutableFloatStateOf(0f) }
    val topBarHeightDp = with(density) { topBarHeightPx.toDp() }
    var isTopBarMeasuredInDayView by remember { mutableStateOf(false) }
    val sidePanelTop = if (isTopBarMeasuredInDayView) {
        topBarHeightDp - (DayCalendarTitleHeight - CalendarTitleHeight)
    } else {
        topBarHeightDp
    }
    var timelineHeaderHeightPx by remember { mutableFloatStateOf(0f) }
    val timelineHeaderHeightDp = with(density) { timelineHeaderHeightPx.toDp() }
    var allDayListDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(Unit) {
        WidgetCalendarEventBus.requestedEvents.collect { request ->
            WidgetCalendarEventBus.consumeRequestedEvent()
            val event = viewModel.eventForBlock(request.blockId, request.occurrenceDate).first()
            if (event != null) {
                selectedDate = LocalDate.parse(event.dateString)
                eventEditorState = event.toEditorState()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (WidgetComposeRequestBus.consume(WidgetComposeRequest.NEW_EVENT)) {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            selectedDate = now.date
            eventEditorState = EventEditorState(
                original = null,
                name = "",
                date = now.date,
                hour = now.hour,
                minute = 0,
                categoryId = null,
                durationMinutes = 30
            )
        }
    }

    LaunchedEffect(Unit) {
        WidgetCalendarDateBus.requestedDates.collect { requestedDate ->
            WidgetCalendarDateBus.consumeRequestedDate()
            try {
                selectedDate = LocalDate.parse(requestedDate)
                viewModel.setViewMode(CalendarViewMode.DAY)
            } catch (cause: IllegalArgumentException) {
                cause.printStackTrace()
            }
        }
    }

    val dayCount = if (viewMode == CalendarViewMode.THREE_DAY) 3 else 7
    val multiDayDates = remember(selectedDate, dayCount, firstDayOfWeek) {
        val anchor = if (dayCount == 7) selectedDate.startOfWeek(firstDayOfWeek) else selectedDate
        (0 until dayCount).map { offset -> anchor.plus(offset.toLong(), DateTimeUnit.DAY) }
    }
    SideEffect {
        eventMoveTracker.visibleDates = if (viewMode == CalendarViewMode.DAY) listOf(selectedDate) else multiDayDates
    }
    val sidePanelSpace = if (isDesktopPlatform) CalendarSidePanelWidth + CalendarSidePanelContentGap else 0.dp

    val onEventMove: (EventMovePreview) -> Unit = { move ->
        val targetDateString = move.targetDate.toString()
        val allDay = if (move.becomesAllDay) AllDaySpan() else null
        if (move.event.recurrenceRule == null) {
            viewModel.moveEvent(move.event, targetDateString, move.timeRange, allDay = allDay)
        } else {
            pendingScopeAction = { scope -> viewModel.moveEvent(move.event, targetDateString, move.timeRange, scope, allDay) }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = internalHazeState)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(start = sidePanelSpace)
            ) {
                val onEventClick: (CalendarEvent) -> Unit = { event ->
                    eventEditorState = event.toEditorState()
                }
                val onEventResize: (CalendarEvent, EventTimeRange) -> Unit = { event, newTimeRange ->
                    if (event.recurrenceRule == null) {
                        viewModel.resizeEvent(event, newTimeRange)
                    } else {
                        pendingScopeAction = { scope -> viewModel.resizeEvent(event, newTimeRange, scope) }
                    }
                }

                when (viewMode) {
                    CalendarViewMode.DAY -> {
                        CalendarTimeGrid(
                            selectedDate = selectedDate,
                            slideDirection = slideDirection,
                            events = events,
                            categories = categories,
                            scrollState = scrollState,
                            topBarHeightDp = topBarHeightDp,
                            headerHeight = timelineHeaderHeightDp,
                            onSwipePreviousDay = {
                                slideDirection = AnimatedContentTransitionScope.SlideDirection.Right
                                selectedDate = selectedDate.plus(-1, DateTimeUnit.DAY)
                            },
                            onSwipeNextDay = {
                                slideDirection = AnimatedContentTransitionScope.SlideDirection.Left
                                selectedDate = selectedDate.plus(1, DateTimeUnit.DAY)
                            },
                            onHourClick = { hour ->
                                eventEditorState = EventEditorState(
                                    original = null,
                                    name = "",
                                    date = selectedDate,
                                    hour = hour,
                                    minute = 0,
                                    categoryId = null,
                                    durationMinutes = 30
                                )
                            },
                            resizeState = eventResizeState,
                            moveTracker = eventMoveTracker,
                            onEventClick = onEventClick,
                            onEventResize = onEventResize,
                            onEventMove = onEventMove,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                    CalendarViewMode.MONTH -> {
                        MonthView(
                            selectedDate = selectedDate,
                            slideDirection = slideDirection,
                            viewModel = viewModel,
                            categories = categories,
                            selectedDateEvents = events,
                            onSwipePreviousMonth = {
                                slideDirection = AnimatedContentTransitionScope.SlideDirection.Right
                                selectedDate =
                                    LocalDate(selectedDate.year, selectedDate.month, 1).plus(-1, DateTimeUnit.MONTH)
                            },
                            onSwipeNextMonth = {
                                slideDirection = AnimatedContentTransitionScope.SlideDirection.Left
                                selectedDate =
                                    LocalDate(selectedDate.year, selectedDate.month, 1).plus(1, DateTimeUnit.MONTH)
                            },
                            onDayClick = { date ->
                                slideDirection = if (date < selectedDate) {
                                    AnimatedContentTransitionScope.SlideDirection.Right
                                } else {
                                    AnimatedContentTransitionScope.SlideDirection.Left
                                }
                                selectedDate = date
                            },
                            onEventClick = onEventClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = topBarHeightDp)
                        )
                    }
                    else -> {
                        MultiDayTimeGrid(
                            startDate = selectedDate,
                            dayCount = dayCount,
                            slideDirection = slideDirection,
                            viewModel = viewModel,
                            categories = categories,
                            scrollState = scrollState,
                            topBarHeightDp = topBarHeightDp,
                            headerHeight = timelineHeaderHeightDp,
                            onSwipePrevious = {
                                slideDirection = AnimatedContentTransitionScope.SlideDirection.Right
                                selectedDate = selectedDate.plus(-dayCount.toLong(), DateTimeUnit.DAY)
                            },
                            onSwipeNext = {
                                slideDirection = AnimatedContentTransitionScope.SlideDirection.Left
                                selectedDate = selectedDate.plus(dayCount.toLong(), DateTimeUnit.DAY)
                            },
                            onHourClick = { date, hour ->
                                eventEditorState = EventEditorState(
                                    original = null,
                                    name = "",
                                    date = date,
                                    hour = hour,
                                    minute = 0,
                                    categoryId = null,
                                    durationMinutes = 30
                                )
                            },
                            resizeState = eventResizeState,
                            moveTracker = eventMoveTracker,
                            onEventClick = onEventClick,
                            onEventResize = onEventResize,
                            onEventMove = onEventMove,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }

            if (!isDesktopPlatform) {
                AnimatedVisibility(
                    visible = areSearchResultsVisible,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier.fillMaxSize().zIndex(5f)
                ) {
                    CalendarSearchResultsList(
                        query = searchQuery,
                        results = searchResults,
                        categories = categories,
                        hazeState = internalHazeState,
                        topPadding = topBarHeightDp,
                        onEventClick = { event ->
                            closeSearch()
                            val eventDate = LocalDate.parse(event.dateString)
                            slideDirection = if (eventDate < selectedDate) {
                                AnimatedContentTransitionScope.SlideDirection.Right
                            } else {
                                AnimatedContentTransitionScope.SlideDirection.Left
                            }
                            selectedDate = eventDate
                            eventEditorState = event.toEditorState()
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .zIndex(10f)
                    .onGloballyPositioned { coordinates ->
                        topBarHeightPx = coordinates.size.height.toFloat()
                        isTopBarMeasuredInDayView = viewMode == CalendarViewMode.DAY
                    }
                    .pointerInput(Unit) { detectTapGestures {} }
            ) {
                EmberrTopHeaderBar(
                    hazeState = internalHazeState,
                    topEdgeFadeAlpha = topEdgeFadeAlpha,
                    topEdgeBlurHeight = if (viewMode != CalendarViewMode.MONTH) {
                        topBarHeightDp + timelineHeaderHeightDp + 48.dp
                    } else {
                        TopEdgeBlurHeight
                    },
                    topEdgeGradientHeight = if (viewMode != CalendarViewMode.MONTH) {
                        topBarHeightDp + timelineHeaderHeightDp + 108.dp
                    } else {
                        TopEdgeGradientHeight
                    },
                    flatFadeHeight = if (viewMode != CalendarViewMode.MONTH && !areSearchResultsVisible) {
                        topBarHeightDp + timelineHeaderHeightDp
                    } else {
                        null
                    },
                    contentPadding = topHeaderBarPadding(bottom = 16.dp),
                    verticalAlignment = Alignment.Top,
                    showBackButton = showBackButton,
                    backTooltip = "Back",
                    onBackClick = onNavigateBack,
                    centerContent = {
                        CalendarTitle(
                            selectedDate = selectedDate,
                            viewMode = viewMode,
                            slideDirection = slideDirection
                        )
                    },
                    actions = {
                        Row(horizontalArrangement = Arrangement.spacedBy(if (isDesktopPlatform) 12.dp else 8.dp)) {
                            if (viewMode == CalendarViewMode.MONTH) {
                                TopBarIconButton(
                                    icon = painterResource(Res.drawable.plus),
                                    contentDescription = "Add event",
                                    bgColor = Color.Transparent,
                                    tint = MaterialTheme.colorScheme.primary,
                                    hazeState = internalHazeState,
                                    hazeStyle = EmberrBlur.Regular,
                                    tooltip = "Add event",
                                    onClick = {
                                        eventEditorState = EventEditorState(
                                            original = null,
                                            name = "",
                                            date = selectedDate,
                                            hour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour,
                                            minute = 0,
                                            categoryId = null,
                                            durationMinutes = 30
                                        )
                                    }
                                )
                            }
                            if (isDesktopPlatform) {
                                CalendarViewModeSwitcher(
                                    viewMode = viewMode,
                                    onViewModeChange = viewModel::setViewMode,
                                    hazeState = internalHazeState
                                )
                            }
                            TopBarIconButton(
                                icon = painterResource(if (isDesktopPlatform) Res.drawable.cog else Res.drawable.sidebar),
                                contentDescription = if (isDesktopPlatform) "Calendar settings" else "Calendar sidebar",
                                bgColor = Color.Transparent,
                                tint = MaterialTheme.colorScheme.primary,
                                hazeState = internalHazeState,
                                hazeStyle = EmberrBlur.Regular,
                                tooltip = "Calendar settings",
                                onClick = {
                                    if (isDesktopPlatform) showSettingsSheet = true else showSidebar = true
                                }
                            )
                        }
                    }
                )
            }

            if (viewMode != CalendarViewMode.MONTH && !areSearchResultsVisible) {
                TimelineHeader(
                    dates = if (viewMode == CalendarViewMode.DAY) listOf(selectedDate) else multiDayDates,
                    showsDayLabels = viewMode != CalendarViewMode.DAY,
                    slideDirection = slideDirection,
                    viewModel = viewModel,
                    categories = categories,
                    onEventClick = { event -> eventEditorState = event.toEditorState() },
                    onEmptyAllDayClick = { date ->
                        eventEditorState = EventEditorState(
                            original = null,
                            name = "",
                            date = date,
                            hour = DEFAULT_ALL_DAY_REMINDER_HOUR,
                            minute = 0,
                            categoryId = null,
                            allDay = AllDaySpan()
                        )
                    },
                    onMoreAllDayClick = { date -> allDayListDate = date },
                    allDayDragState = allDayDragState,
                    moveTracker = eventMoveTracker,
                    onDropIntoTimeline = onEventMove,
                    onAllDaySpanChange = { change ->
                        if (change.event.recurrenceRule == null) {
                            viewModel.changeAllDaySpan(change.event, change.startDate, change.lengthInDays)
                        } else {
                            pendingScopeAction = { scope ->
                                viewModel.changeAllDaySpan(change.event, change.startDate, change.lengthInDays, scope)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .zIndex(11f)
                        .padding(start = sidePanelSpace, top = topBarHeightDp)
                        .onSizeChanged { size -> timelineHeaderHeightPx = size.height.toFloat() }
                )
            }

            if (isDesktopPlatform) {
                CalendarSidePanel(
                    selectedDate = selectedDate,
                    visibleDates = when (viewMode) {
                        CalendarViewMode.DAY, CalendarViewMode.MONTH -> listOf(selectedDate)
                        else -> multiDayDates
                    },
                    viewModel = viewModel,
                    categories = categories,
                    hiddenCategoryVisibilityKeys = hiddenCategoryVisibilityKeys,
                    spaces = spaces,
                    onDateClick = { date ->
                        slideDirection = if (date < selectedDate) {
                            AnimatedContentTransitionScope.SlideDirection.Right
                        } else {
                            AnimatedContentTransitionScope.SlideDirection.Left
                        }
                        selectedDate = date
                    },
                    onToggleVisibility = viewModel::toggleCategoryVisibility,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .zIndex(12f)
                        .padding(top = sidePanelTop)
                )
            }

            if (!isDesktopPlatform) {
                CalendarSearchBar(
                    isOpen = isSearchOpen,
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onOpenClick = { isSearchOpen = true },
                    onCloseClick = closeSearch,
                    hazeState = internalHazeState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(20f)
                )
                KmpBackHandler(enabled = isSearchOpen, onBack = closeSearch)

                CalendarSidebar(
                    title = "Settings",
                    isOpen = showSidebar,
                    onClose = { showSidebar = false },
                    hazeState = internalHazeState,
                    modifier = Modifier.zIndex(30f)
                ) {
                    CalendarSettingsContent(
                        viewMode = viewMode,
                        onViewModeChange = { mode ->
                            viewModel.setViewMode(mode)
                            showSidebar = false
                        },
                        firstDayOfWeekPreference = firstDayOfWeekPreference,
                        onFirstDayOfWeekPreferenceChange = viewModel::setFirstDayOfWeekPreference,
                        categories = categories,
                        onAddCategory = viewModel::addCategory,
                        onUpdateCategory = viewModel::updateCategory,
                        onDeleteCategory = viewModel::deleteCategory,
                        hiddenCategoryVisibilityKeys = hiddenCategoryVisibilityKeys,
                        onToggleCategoryVisibility = viewModel::toggleCategoryVisibility,
                        spaces = spaces
                    )
                }
            }
        }
    }

    EmberrBottomSheet(
        expanded = showSettingsSheet,
        onDismiss = { showSettingsSheet = false },
        title = "Calendar Settings",
        contentHorizontalPadding = 0.dp
    ) {
        CalendarSettingsContent(
            firstDayOfWeekPreference = firstDayOfWeekPreference,
            onFirstDayOfWeekPreferenceChange = viewModel::setFirstDayOfWeekPreference,
            categories = categories,
            onAddCategory = viewModel::addCategory,
            onUpdateCategory = viewModel::updateCategory,
            onDeleteCategory = viewModel::deleteCategory,
            hiddenCategoryVisibilityKeys = hiddenCategoryVisibilityKeys,
            onToggleCategoryVisibility = viewModel::toggleCategoryVisibility,
            spaces = spaces,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }

    EventEditorSheet(
        state = eventEditorState,
        categories = categories,
        onNameChange = { name -> eventEditorState = eventEditorState?.copy(name = name) },
        onDateChange = { date -> eventEditorState = eventEditorState?.copy(date = date) },
        onTimeChange = { hour, minute -> eventEditorState = eventEditorState?.copy(hour = hour, minute = minute) },
        onDurationChange = { minutes -> eventEditorState = eventEditorState?.copy(durationMinutes = minutes) },
        onCategoryChange = { categoryId -> eventEditorState = eventEditorState?.copy(categoryId = categoryId) },
        onUrlChange = { url -> eventEditorState = eventEditorState?.copy(url = url) },
        onDescriptionChange = { description -> eventEditorState = eventEditorState?.copy(description = description) },
        onRecurrenceChange = { rule -> eventEditorState = eventEditorState?.copy(recurrenceRule = rule) },
        onAllDayChange = { allDay -> eventEditorState = eventEditorState?.copy(allDay = allDay) },
        onEditClick = {
            val recurring = eventEditorState?.original?.recurrenceRule != null
            if (recurring) {
                pendingScopeAction = { scope -> eventEditorState = eventEditorState?.copy(isEditing = true, editScope = scope) }
            } else {
                eventEditorState = eventEditorState?.copy(isEditing = true)
            }
        },
        onCancelEdit = { eventEditorState = eventEditorState?.original?.toEditorState() },
        onSave = {
            eventEditorState?.let { state ->
                viewModel.saveEvent(
                    original = state.original,
                    dateString = state.date.toString(),
                    timestamp = state.toEpochMillis(),
                    name = state.name,
                    categoryId = state.categoryId,
                    durationMinutes = state.durationMinutes,
                    url = state.url,
                    description = state.description,
                    allDay = state.allDay,
                    recurrenceRule = state.recurrenceRule,
                    editScope = state.editScope
                )
            }
            eventEditorState = eventEditorState?.viewAfterSaving()
        },
        onDelete = eventEditorState?.original?.let { original ->
            {
                if (original.recurrenceRule != null) {
                    pendingScopeAction = { scope ->
                        viewModel.deleteEvent(original, scope)
                        eventEditorState = null
                    }
                } else {
                    viewModel.deleteEvent(original)
                    eventEditorState = null
                }
            }
        },
        onDismiss = { eventEditorState = null },
        sourceNoteTitle = sourceNoteTitle,
        onOpenSourceNote = onOpenSourceNote?.let { openSourceNote ->
            shownEvent?.let { event ->
                {
                    eventEditorState = null
                    openSourceNote(event)
                }
            }
        }
    )

    val allDayListEvents by remember(allDayListDate) {
        allDayListDate?.let { date -> viewModel.eventsForDate(date.toString()) } ?: flowOf(emptyList<CalendarEvent>())
    }.collectAsState(initial = emptyList())
    AllDayEventsSheet(
        date = allDayListDate,
        events = allDayListEvents.filter { it.allDay != null },
        categories = categories,
        onEventClick = { event -> eventEditorState = event.toEditorState() },
        onDismiss = { allDayListDate = null }
    )

    pendingScopeAction?.let { runWithScope ->
        RecurrenceScopeChooser(
            onDismiss = {
                if (pendingScopeAction != null) {
                    eventResizeState.preview = null
                    eventMoveTracker.preview = null
                    allDayDragState.preview = null
                }
                pendingScopeAction = null
            },
            onScopeSelected = { scope ->
                pendingScopeAction = null
                runWithScope(scope)
            }
        )
    }
}

@Composable
private fun CalendarTitle(
    selectedDate: LocalDate,
    viewMode: CalendarViewMode,
    slideDirection: AnimatedContentTransitionScope.SlideDirection
) {
    val titleColor = MaterialTheme.colorScheme.onSurface

    AnimatedContent(
        targetState = selectedDate,
        transitionSpec = {
            (slideIntoContainer(slideDirection, tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300))) togetherWith
                    (slideOutOfContainer(slideDirection, tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300)))
        },
        contentKey = { date -> if (viewMode == CalendarViewMode.MONTH) monthKey(date) else date },
        label = "CalendarTitleTransition",
        modifier = Modifier.height(if (viewMode == CalendarViewMode.DAY) DayCalendarTitleHeight else CalendarTitleHeight)
    ) { date ->
        Box(
            modifier = Modifier.fillMaxHeight(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (viewMode == CalendarViewMode.MONTH) {
                Text(
                    text = formatMonthYear(date),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = formatSelectedDateTitle(date),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = formatDayOfWeek(date),
                        style = MaterialTheme.typography.labelSmall,
                        color = titleColor.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineHeader(
    dates: List<LocalDate>,
    showsDayLabels: Boolean,
    slideDirection: AnimatedContentTransitionScope.SlideDirection,
    viewModel: CalendarViewModel,
    categories: List<CalendarCategory>,
    onEventClick: (CalendarEvent) -> Unit,
    onEmptyAllDayClick: (LocalDate) -> Unit,
    onMoreAllDayClick: (LocalDate) -> Unit,
    allDayDragState: AllDayDragState,
    moveTracker: EventMoveTracker,
    onDropIntoTimeline: (EventMovePreview) -> Unit,
    onAllDaySpanChange: (AllDaySpanPreview) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.onPlaced { moveTracker.allDayDropZone = it }) {
        AnimatedContent(
            targetState = dates,
            transitionSpec = {
                (slideIntoContainer(slideDirection, tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300))) togetherWith
                        (slideOutOfContainer(slideDirection, tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300)))
            },
            label = "TimelineHeaderTransition"
        ) { windowDates ->
          Column {
            if (showsDayLabels) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(56.dp))
                windowDates.forEachIndexed { index, date ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (windowDates.size >= 7) formatSingleLetterDayLabel(date) else formatShortDayLabel(date),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (date.dayOfWeek == DayOfWeek.SUNDAY) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1
                        )
                    }
                    if (index != windowDates.lastIndex) {
                        VerticalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
            }
            val windowEvents by remember(windowDates) { viewModel.eventsInRange(windowDates.first(), windowDates.last()) }
                .collectAsState(initial = emptyList())
            AllDayStrip(
                windowDates = windowDates,
                events = windowEvents,
                categories = categories,
                dragState = allDayDragState,
                moveTracker = moveTracker,
                onEventClick = onEventClick,
                onEmptyDateClick = onEmptyAllDayClick,
                onMoreClick = onMoreAllDayClick,
                onDropIntoTimeline = onDropIntoTimeline,
                onAllDaySpanChange = onAllDaySpanChange
            )
          }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
            thickness = 1.dp,
            modifier = Modifier.padding(start = 56.dp)
        )
    }
}

internal fun formatFullDate(date: LocalDate): String {
    val monthAbbrev = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$monthAbbrev ${date.day}${ordinalSuffix(date.day)}, ${date.year}"
}

private fun formatSelectedDateTitle(date: LocalDate): String = formatFullDate(date)

private fun formatDayOfWeek(date: LocalDate): String =
    date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

internal fun formatMonthYear(date: LocalDate): String {
    val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
    return "$monthName ${date.year}"
}

internal fun ordinalSuffix(day: Int): String = when {
    day in 11..13 -> "th"
    else -> when (day % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

@Composable
private fun CalendarTimeGrid(
    selectedDate: LocalDate,
    slideDirection: AnimatedContentTransitionScope.SlideDirection,
    events: List<CalendarEvent>,
    categories: List<CalendarCategory>,
    scrollState: ScrollState,
    topBarHeightDp: Dp,
    headerHeight: Dp,
    onSwipePreviousDay: () -> Unit,
    onSwipeNextDay: () -> Unit,
    resizeState: EventResizeState,
    moveTracker: EventMoveTracker,
    onHourClick: (hour: Int) -> Unit,
    onEventClick: (event: CalendarEvent) -> Unit,
    onEventResize: (event: CalendarEvent, newTimeRange: EventTimeRange) -> Unit,
    onEventMove: (move: EventMovePreview) -> Unit,
    modifier: Modifier = Modifier
) {
    var accumulatedDragPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }
    val nowMillis = rememberNowMillis()
    val currentOnEventMove by rememberUpdatedState(onEventMove)
    EventMoveFrameEffect(moveTracker, onHoldAtPreviousEdge = onSwipePreviousDay, onHoldAtNextEdge = onSwipeNextDay)

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { accumulatedDragPx = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragPx += dragAmount
                    },
                    onDragEnd = {
                        if (accumulatedDragPx <= -swipeThresholdPx) {
                            onSwipeNextDay()
                        } else if (accumulatedDragPx >= swipeThresholdPx) {
                            onSwipePreviousDay()
                        }
                        accumulatedDragPx = 0f
                    },
                    onDragCancel = { accumulatedDragPx = 0f }
                )
            }
            .onPlaced { moveTracker.gridCoordinates = it }
            .pointerInput(moveTracker) { followEventMovePointer(moveTracker) { move -> currentOnEventMove(move) } }
    ) {
        AnimatedContent(
            targetState = selectedDate,
            transitionSpec = {
                (slideIntoContainer(slideDirection, tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300))) togetherWith
                        (slideOutOfContainer(slideDirection, tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300)))
            },
            label = "CalendarDayGridTransition",
            modifier = Modifier.fillMaxSize()
        ) { date ->
            DayHourGrid(
                date = date,
                events = events,
                categories = categories,
                nowMillis = nowMillis,
                scrollState = scrollState,
                topBarHeightDp = topBarHeightDp,
                headerHeight = headerHeight,
                resizeState = resizeState,
                moveTracker = moveTracker,
                onHourClick = onHourClick,
                onEventClick = onEventClick,
                onEventResize = onEventResize
            )
        }
        FloatingMovedEventCard(tracker = moveTracker, categories = categories)
    }
}

@Composable
private fun DayHourGrid(
    date: LocalDate,
    events: List<CalendarEvent>,
    categories: List<CalendarCategory>,
    nowMillis: Long,
    scrollState: ScrollState,
    topBarHeightDp: Dp,
    headerHeight: Dp,
    resizeState: EventResizeState,
    moveTracker: EventMoveTracker,
    onHourClick: (hour: Int) -> Unit,
    onEventClick: (event: CalendarEvent) -> Unit,
    onEventResize: (event: CalendarEvent, newTimeRange: EventTimeRange) -> Unit
) {
    val hourHeight = 72.dp
    val hours = remember { 0..23 }
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val bottomPadding = if (isDesktopPlatform) 0.dp else calendarSearchBarBottomPadding()
    val resizeAutoScroll = rememberEventResizeAutoScroll(scrollState, topBarHeightDp + headerHeight, bottomPadding)
    SideEffect {
        moveTracker.autoScroll = resizeAutoScroll
        moveTracker.hourHeightPx = hourHeightPx
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .onPlaced { resizeAutoScroll.viewportCoordinates = it }
            .verticalScroll(scrollState)
            .smoothWheelScroll(scrollState)
            .padding(top = topBarHeightDp + headerHeight, bottom = bottomPadding)
    ) {
        HourLabelColumn(hours = hours, hourHeight = hourHeight)

        Box(modifier = Modifier.fillMaxWidth().height(hourHeight * hours.count())) {
            DayColumnBody(
                date = date,
                hours = hours,
                hourHeight = hourHeight,
                hourHeightPx = hourHeightPx,
                events = events,
                categories = categories,
                nowMillis = nowMillis,
                resizeState = resizeState,
                resizeAutoScroll = resizeAutoScroll,
                moveTracker = moveTracker,
                onHourClick = onHourClick,
                onEventClick = onEventClick,
                onEventResize = onEventResize
            )
        }
    }

        EmberrVerticalScrollbar(
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = topBarHeightDp + headerHeight, bottom = if (isDesktopPlatform) 0.dp else calendarSearchBarBottomPadding())
        )
    }
}

@Composable
private fun HourLabelColumn(hours: IntRange, hourHeight: Dp) {
    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current
    Column(modifier = Modifier.width(56.dp)) {
        hours.forEach { hour ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(hourHeight),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = formatHourLabel(hour, usesTwentyFourHourClock),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private class ActiveEventResize(
    val event: CalendarEvent,
    val edge: EventResizeEdge,
    val startYInColumn: Float,
    var pointerInWindow: Offset
) {
    var hasMoved = false
}

@Composable
private fun DayColumnBody(
    date: LocalDate,
    hours: IntRange,
    hourHeight: Dp,
    hourHeightPx: Float,
    events: List<CalendarEvent>,
    categories: List<CalendarCategory>,
    nowMillis: Long,
    resizeState: EventResizeState,
    resizeAutoScroll: EventResizeAutoScroll,
    moveTracker: EventMoveTracker,
    onHourClick: (hour: Int) -> Unit,
    onEventClick: (event: CalendarEvent) -> Unit,
    onEventResize: (event: CalendarEvent, newTimeRange: EventTimeRange) -> Unit
) {
    var columnCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var activeResize by remember { mutableStateOf<ActiveEventResize?>(null) }
    val haptics = LocalHapticFeedback.current

    fun showResizePreview(resize: ActiveEventResize) {
        val pointerYInColumn = columnCoordinates?.takeIf { it.isAttached }
            ?.windowToLocal(resize.pointerInWindow)?.y ?: return
        val draggedMinutes = (pointerYInColumn - resize.startYInColumn) / hourHeightPx * 60f
        resizeState.preview = EventResizePreview(
            event = resize.event,
            timeRange = resizeEventTimeRange(resize.event.timeRange(), resize.edge, draggedMinutes)
        )
    }

    LaunchedEffect(events) {
        if (resizeState.preview?.event?.dateString == date.toString()) resizeState.preview = null
        val movePreview = moveTracker.preview
        if (movePreview != null && moveTracker.activeMove == null) {
            val hasLandedHere = movePreview.targetDate == date
            val hasLeftHere = movePreview.event.dateString == date.toString() && events.none { it == movePreview.event }
            if (hasLandedHere || hasLeftHere) moveTracker.preview = null
        }
    }

    DisposableEffect(date, columnCoordinates) {
        val coordinates = columnCoordinates
        if (coordinates != null) moveTracker.registerColumn(date, coordinates)
        onDispose { moveTracker.unregisterColumn(date, coordinates) }
    }

    LaunchedEffect(activeResize) {
        val resize = activeResize ?: return@LaunchedEffect
        while (true) {
            withFrameNanos { }
            if (resize.hasMoved && resizeAutoScroll.scrollTowards(resize.pointerInWindow)) showResizePreview(resize)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(hourHeight * hours.count())
            .onPlaced { columnCoordinates = it }
            .pointerInput(hourHeightPx) {
                detectTapGestures { offset ->
                    if (resizeState.eventInResizeMode != null) {
                        resizeState.eventInResizeMode = null
                    } else {
                        val hour = (offset.y / hourHeightPx).toInt().coerceIn(0, 23)
                        onHourClick(hour)
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            hours.forEach { _ ->
                Box(modifier = Modifier.fillMaxWidth().height(hourHeight)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        thickness = 1.dp
                    )
                }
            }
        }

        val horizontalInset = 6.dp
        val chipGap = 4.dp
        val availableWidth = maxWidth - horizontalInset * 2
        val positionedEvents = remember(events) { layoutEventsForColumn(events.filter { it.allDay == null }) }
        val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current

        positionedEvents.forEach { positioned ->
            val event = positioned.event
            key(event.blockId, event.occurrenceDateString) {
                val timeRange = resizeState.preview?.takeIf { event.isSameOccurrenceAs(it.event) }?.timeRange
                    ?: event.timeRange()
                val topOffset = hourHeight * (timeRange.startMinuteOfDay / 60f)
                val chipHeight = maxOf(24.dp, hourHeight * (timeRange.durationMinutes / 60f))
                val category = categories.firstOrNull { it.id == event.categoryId }

                val chipWidth = (availableWidth - chipGap * (positioned.columnCount - 1)) / positioned.columnCount
                val xOffset = horizontalInset + (chipWidth + chipGap) * positioned.columnIndex

                EventChip(
                    text = event.text,
                    timeText = formatEventTimeRange(timeRange, usesTwentyFourHourClock),
                    color = category?.colorHex?.toCategoryColor() ?: MaterialTheme.colorScheme.surface,
                    hasCategory = category != null,
                    isChecked = event.isChecked,
                    height = chipHeight,
                    isInResizeMode = event.isSameOccurrenceAs(resizeState.eventInResizeMode),
                    onResizeStart = { edge, pointerInWindow ->
                        columnCoordinates?.takeIf { it.isAttached }?.windowToLocal(pointerInWindow)?.y?.let { startY ->
                            activeResize = ActiveEventResize(event, edge, startY, pointerInWindow)
                        }
                    },
                    onResizeMove = { pointerInWindow ->
                        activeResize?.let { resize ->
                            resize.pointerInWindow = pointerInWindow
                            resize.hasMoved = true
                            showResizePreview(resize)
                        }
                    },
                    onResizeEnd = {
                        activeResize = null
                        val newTimeRange = resizeState.preview?.takeIf { event.isSameOccurrenceAs(it.event) }?.timeRange
                        if (newTimeRange == null || newTimeRange == event.timeRange()) {
                            resizeState.preview = null
                        } else {
                            onEventResize(event, newTimeRange)
                        }
                    },
                    onResizeCancel = {
                        activeResize = null
                        resizeState.preview = null
                    },
                    onMoveStart = { pointerInWindow, pointerId, cardTopLeftInWindow, cardSize ->
                        columnCoordinates?.takeIf { it.isAttached }?.let { column ->
                            moveTracker.start(
                                event = event,
                                pointerId = pointerId,
                                pointerInWindow = pointerInWindow,
                                cardTopLeftInWindow = cardTopLeftInWindow,
                                cardSize = cardSize,
                                sourceColumn = column,
                                hourHeightPx = hourHeightPx
                            )
                        }
                    },
                    onLongClick = if (isDesktopPlatform) null else {
                        {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            resizeState.eventInResizeMode = event
                        }
                    },
                    onClick = {
                        resizeState.eventInResizeMode = null
                        onEventClick(event)
                    },
                    modifier = Modifier
                        .width(chipWidth)
                        .padding(top = topOffset)
                        .offset(x = xOffset)
                        .alpha(if (event.isSameOccurrenceAs(moveTracker.preview?.event)) MovedEventOriginalAlpha else 1f)
                )
            }
        }

        moveTracker.preview?.takeIf { it.targetDate == date && moveTracker.activeMove == null && !it.becomesAllDay }?.let { preview ->
            val category = categories.firstOrNull { it.id == preview.event.categoryId }
            EventMovePreviewChip(
                text = preview.event.text,
                timeText = formatEventTimeRange(preview.timeRange, usesTwentyFourHourClock),
                color = category?.colorHex?.toCategoryColor() ?: MaterialTheme.colorScheme.surface,
                hasCategory = category != null,
                isChecked = preview.event.isChecked,
                height = maxOf(24.dp, hourHeight * (preview.timeRange.durationMinutes / 60f)),
                modifier = Modifier
                    .width(availableWidth)
                    .padding(top = hourHeight * (preview.timeRange.startMinuteOfDay / 60f))
                    .offset(x = horizontalInset)
            )
        }

        val nowDt = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        if (date == nowDt.date) {
            val nowOffset = hourHeight * nowDt.hour + hourHeight * (nowDt.minute / 60f)
            CurrentTimeLine(modifier = Modifier.fillMaxWidth().padding(top = nowOffset))
        }
    }
}

@Composable
private fun rememberNowMillis(): Long {
    var nowMillis by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000.milliseconds)
            nowMillis = Clock.System.now().toEpochMilliseconds()
        }
    }
    return nowMillis
}

@Composable
private fun CurrentTimeLine(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

private fun formatShortDayLabel(date: LocalDate): String {
    val shortDay = date.dayOfWeek.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
    return "$shortDay ${date.day}"
}

private fun formatSingleLetterDayLabel(date: LocalDate): String {
    val initial = date.dayOfWeek.name.take(1)
    return "$initial ${date.day}"
}

@Composable
private fun MultiDayTimeGrid(
    startDate: LocalDate,
    dayCount: Int,
    slideDirection: AnimatedContentTransitionScope.SlideDirection,
    viewModel: CalendarViewModel,
    categories: List<CalendarCategory>,
    scrollState: ScrollState,
    topBarHeightDp: Dp,
    headerHeight: Dp,
    onSwipePrevious: () -> Unit,
    onSwipeNext: () -> Unit,
    resizeState: EventResizeState,
    moveTracker: EventMoveTracker,
    onHourClick: (date: LocalDate, hour: Int) -> Unit,
    onEventClick: (event: CalendarEvent) -> Unit,
    onEventResize: (event: CalendarEvent, newTimeRange: EventTimeRange) -> Unit,
    onEventMove: (move: EventMovePreview) -> Unit,
    modifier: Modifier = Modifier
) {
    var accumulatedDragPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }
    val nowMillis = rememberNowMillis()
    val currentOnEventMove by rememberUpdatedState(onEventMove)
    EventMoveFrameEffect(moveTracker, onHoldAtPreviousEdge = onSwipePrevious, onHoldAtNextEdge = onSwipeNext)

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(dayCount) {
                detectHorizontalDragGestures(
                    onDragStart = { accumulatedDragPx = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragPx += dragAmount
                    },
                    onDragEnd = {
                        if (accumulatedDragPx <= -swipeThresholdPx) {
                            onSwipeNext()
                        } else if (accumulatedDragPx >= swipeThresholdPx) {
                            onSwipePrevious()
                        }
                        accumulatedDragPx = 0f
                    },
                    onDragCancel = { accumulatedDragPx = 0f }
                )
            }
            .onPlaced { moveTracker.gridCoordinates = it }
            .pointerInput(moveTracker) { followEventMovePointer(moveTracker) { move -> currentOnEventMove(move) } }
    ) {
        AnimatedContent(
            targetState = startDate,
            transitionSpec = {
                (slideIntoContainer(slideDirection, tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300))) togetherWith
                        (slideOutOfContainer(slideDirection, tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300)))
            },
            label = "MultiDayGridTransition",
            modifier = Modifier.fillMaxSize()
        ) { windowStart ->
            MultiDayGridContent(
                windowStart = windowStart,
                dayCount = dayCount,
                viewModel = viewModel,
                categories = categories,
                nowMillis = nowMillis,
                scrollState = scrollState,
                topBarHeightDp = topBarHeightDp,
                headerHeight = headerHeight,
                resizeState = resizeState,
                moveTracker = moveTracker,
                onHourClick = onHourClick,
                onEventClick = onEventClick,
                onEventResize = onEventResize
            )
        }
        FloatingMovedEventCard(tracker = moveTracker, categories = categories)
    }
}

@Composable
private fun MultiDayGridContent(
    windowStart: LocalDate,
    dayCount: Int,
    viewModel: CalendarViewModel,
    categories: List<CalendarCategory>,
    nowMillis: Long,
    scrollState: ScrollState,
    topBarHeightDp: Dp,
    headerHeight: Dp,
    resizeState: EventResizeState,
    moveTracker: EventMoveTracker,
    onHourClick: (date: LocalDate, hour: Int) -> Unit,
    onEventClick: (event: CalendarEvent) -> Unit,
    onEventResize: (event: CalendarEvent, newTimeRange: EventTimeRange) -> Unit
) {
    val firstDayOfWeek = LocalFirstDayOfWeek.current
    val dates = remember(windowStart, dayCount, firstDayOfWeek) {
        val anchor = if (dayCount == 7) windowStart.startOfWeek(firstDayOfWeek) else windowStart
        (0 until dayCount).map { offset -> anchor.plus(offset.toLong(), DateTimeUnit.DAY) }
    }
    val eventsByDate = dates.associateWith { date ->
        val state by remember(date) { viewModel.eventsForDate(date.toString()) }
            .collectAsState(initial = emptyList())
        state
    }

    val hourHeight = 72.dp
    val hours = remember { 0..23 }
    val density = LocalDensity.current
    val hourHeightPx = with(density) { hourHeight.toPx() }
    val bottomPadding = if (isDesktopPlatform) 0.dp else calendarSearchBarBottomPadding()
    val resizeAutoScroll = rememberEventResizeAutoScroll(scrollState, topBarHeightDp + headerHeight, bottomPadding)
    SideEffect {
        moveTracker.autoScroll = resizeAutoScroll
        moveTracker.hourHeightPx = hourHeightPx
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .onPlaced { resizeAutoScroll.viewportCoordinates = it }
            .verticalScroll(scrollState)
            .smoothWheelScroll(scrollState)
            .padding(top = topBarHeightDp + headerHeight, bottom = bottomPadding)
    ) {
        HourLabelColumn(hours = hours, hourHeight = hourHeight)

        dates.forEachIndexed { index, date ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(hourHeight * hours.count())
            ) {
                DayColumnBody(
                    date = date,
                    hours = hours,
                    hourHeight = hourHeight,
                    hourHeightPx = hourHeightPx,
                    events = eventsByDate[date] ?: emptyList(),
                    categories = categories,
                    nowMillis = nowMillis,
                    resizeState = resizeState,
                    resizeAutoScroll = resizeAutoScroll,
                    moveTracker = moveTracker,
                    onHourClick = { hour -> onHourClick(date, hour) },
                    onEventClick = onEventClick,
                    onEventResize = onEventResize
                )
            }
            if (index != dates.lastIndex) {
                VerticalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    modifier = Modifier.height(hourHeight * hours.count())
                )
            }
        }
    }

        EmberrVerticalScrollbar(
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .padding(top = topBarHeightDp + headerHeight, bottom = if (isDesktopPlatform) 0.dp else calendarSearchBarBottomPadding())
        )
    }
}

private fun buildMonthGridDates(anchorMonth: LocalDate, firstDayOfWeek: DayOfWeek): List<LocalDate> {
    val firstOfMonth = LocalDate(anchorMonth.year, anchorMonth.month, 1)
    val leadingBlanks = daysSinceStartOfWeek(firstOfMonth.dayOfWeek, firstDayOfWeek)
    val gridStart = firstOfMonth.startOfWeek(firstDayOfWeek)
    val nextMonthFirst = firstOfMonth.plus(1, DateTimeUnit.MONTH)
    val daysInMonth = firstOfMonth.daysUntil(nextMonthFirst)
    val totalCells = ((leadingBlanks + daysInMonth + 6) / 7) * 7
    return (0 until totalCells).map { offset -> gridStart.plus(offset.toLong(), DateTimeUnit.DAY) }
}

@Composable
private fun MonthView(
    selectedDate: LocalDate,
    slideDirection: AnimatedContentTransitionScope.SlideDirection,
    viewModel: CalendarViewModel,
    categories: List<CalendarCategory>,
    selectedDateEvents: List<CalendarEvent>,
    onSwipePreviousMonth: () -> Unit,
    onSwipeNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isDesktopPlatform) {
        Row(modifier = modifier) {
            MonthGrid(
                selectedDate = selectedDate,
                slideDirection = slideDirection,
                viewModel = viewModel,
                categories = categories,
                onSwipePreviousMonth = onSwipePreviousMonth,
                onSwipeNextMonth = onSwipeNextMonth,
                onDayClick = onDayClick,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
            MonthEventList(
                events = selectedDateEvents,
                categories = categories,
                onEventClick = onEventClick,
                modifier = Modifier.weight(1f).fillMaxHeight()
            )
        }
    } else {
        Column(modifier = modifier) {
            MonthGrid(
                selectedDate = selectedDate,
                slideDirection = slideDirection,
                viewModel = viewModel,
                categories = categories,
                onSwipePreviousMonth = onSwipePreviousMonth,
                onSwipeNextMonth = onSwipeNextMonth,
                onDayClick = onDayClick,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
            MonthEventList(
                events = selectedDateEvents,
                categories = categories,
                onEventClick = onEventClick,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MonthGrid(
    selectedDate: LocalDate,
    slideDirection: AnimatedContentTransitionScope.SlideDirection,
    viewModel: CalendarViewModel,
    categories: List<CalendarCategory>,
    onSwipePreviousMonth: () -> Unit,
    onSwipeNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var accumulatedDragPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 80.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { accumulatedDragPx = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragPx += dragAmount
                    },
                    onDragEnd = {
                        if (accumulatedDragPx <= -swipeThresholdPx) {
                            onSwipeNextMonth()
                        } else if (accumulatedDragPx >= swipeThresholdPx) {
                            onSwipePreviousMonth()
                        }
                        accumulatedDragPx = 0f
                    },
                    onDragCancel = { accumulatedDragPx = 0f }
                )
            }
    ) {
        AnimatedContent(
            targetState = selectedDate,
            transitionSpec = {
                (slideIntoContainer(slideDirection, tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300))) togetherWith
                        (slideOutOfContainer(slideDirection, tween(300, easing = FastOutSlowInEasing)) + fadeOut(tween(300)))
            },
            contentKey = { date -> monthKey(date) },
            label = "MonthGridTransition",
            modifier = Modifier.fillMaxSize()
        ) { date ->
            MonthGridContent(
                selectedDate = date,
                viewModel = viewModel,
                categories = categories,
                onDayClick = onDayClick
            )
        }
    }
}

private fun monthKey(date: LocalDate): String = "${date.year}-${date.month.number}"

@Composable
private fun MonthGridContent(
    selectedDate: LocalDate,
    viewModel: CalendarViewModel,
    categories: List<CalendarCategory>,
    onDayClick: (LocalDate) -> Unit
) {
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
    val firstDayOfWeek = LocalFirstDayOfWeek.current
    val gridDates = remember(selectedDate.year, selectedDate.month, firstDayOfWeek) {
        buildMonthGridDates(selectedDate, firstDayOfWeek)
    }
    val yearMonth = remember(selectedDate.year, selectedDate.month) {
        "${selectedDate.year}-${selectedDate.month.number.toString().padStart(2, '0')}"
    }
    val monthEvents by remember(yearMonth) { viewModel.eventsForMonth(yearMonth) }
        .collectAsState(initial = emptyList())
    val eventsByDate = remember(monthEvents) {
        monthEvents
            .flatMap { event -> event.datesShownOn().map { date -> date.toString() to event } }
            .groupBy({ (dateString, _) -> dateString }, { (_, event) -> event })
    }
    val weekRows = remember(gridDates) { gridDates.chunked(7) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp).padding(bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            daysOfWeekStartingFrom(firstDayOfWeek).forEach { day ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = day.name.take(1),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day == DayOfWeek.SUNDAY) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            weekRows.forEach { week ->
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    week.forEach { date ->
                        MonthDayCell(
                            date = date,
                            isCurrentMonth = date.month == selectedDate.month,
                            isToday = date == today,
                            isSelected = date == selectedDate,
                            events = eventsByDate[date.toString()] ?: emptyList(),
                            categories = categories,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthEventList(
    events: List<CalendarEvent>,
    categories: List<CalendarCategory>,
    onEventClick: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val sortedEvents = remember(events) { events.sortedWith(compareBy({ it.allDay == null }, { it.reminderTimestamp })) }
    val bottomPadding = if (isDesktopPlatform) {
        16.dp
    } else {
        calendarSearchBarBottomPadding() + 4.dp
    }

    Box(modifier = modifier) {
        if (sortedEvents.isEmpty()) {
            Text(
                text = "No events",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Center).padding(bottom = bottomPadding)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().smoothWheelScroll(listState),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = bottomPadding),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(sortedEvents) { event ->
                    MonthEventRow(
                        event = event,
                        category = categories.firstOrNull { it.id == event.categoryId },
                        onClick = { onEventClick(event) }
                    )
                }
            }

            EmberrVerticalScrollbar(
                listState = listState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(bottom = bottomPadding)
            )
        }
    }
}

@Composable
internal fun MonthEventRow(
    event: CalendarEvent,
    category: CalendarCategory?,
    onClick: () -> Unit
) {
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
        Text(
            text = event.text.ifBlank { "Untitled event" },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (event.isChecked) 0.45f else 1f),
            textDecoration = if (event.isChecked) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        )
        Text(
            text = event.timeOfDayLabel(LocalUsesTwentyFourHourClock.current),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun MonthDayCell(
    date: LocalDate,
    isCurrentMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    events: List<CalendarEvent>,
    categories: List<CalendarCategory>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cellShape = RoundedCornerShape(8.dp)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val cellColor = if (isCurrentMonth) surfaceColor else surfaceColor.copy(alpha = 0.5f)
    val eventColors = events.take(3).map { event ->
        categories.firstOrNull { it.id == event.categoryId }?.colorHex?.toCategoryColor()
            ?: MaterialTheme.colorScheme.outline
    }
    val dayNumberColor = if (date.dayOfWeek == DayOfWeek.SUNDAY) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = modifier
            .clip(cellShape)
            .background(cellColor)
            .then(if (eventColors.isNotEmpty()) Modifier.background(eventColors.first().copy(alpha = 0.15f)) else Modifier)
            .then(if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.outline, cellShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .then(
                    if (isToday) Modifier.background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = date.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = when {
                    isToday -> MaterialTheme.colorScheme.onPrimary
                    isCurrentMonth -> dayNumberColor
                    else -> dayNumberColor.copy(alpha = 0.3f)
                }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(top = 4.dp).height(5.dp)
        ) {
            eventColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(color, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun CalendarViewModeSwitcher(
    viewMode: CalendarViewMode,
    onViewModeChange: (CalendarViewMode) -> Unit,
    hazeState: HazeState
) {
    Surface(
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .height(TopHeaderBarButtonSize)
            .emberrBorder(CircleShape)
            .customEmberrShadow(CircleShape, elevation = EmberrShadowElevation.Standard)
            .clip(CircleShape)
            .emberrBlur(hazeState, EmberrBlur.Regular)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            calendarViewModeLabels.forEach { (mode, label) ->
                val isSelected = viewMode == mode
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onViewModeChange(mode) }
                        .padding(horizontal = 14.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        },
                        maxLines = 1
                    )
                }
            }
        }
    }
}

