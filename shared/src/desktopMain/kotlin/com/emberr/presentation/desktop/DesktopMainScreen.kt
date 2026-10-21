package com.emberr.presentation.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.domain.model.AllDaySpan
import com.emberr.domain.model.CalloutType
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.TextAlignment
import com.emberr.presentation.LocalCanvasFullScreenOverlay
import com.emberr.presentation.shared.editor.blockViews.database.DatabaseBlockEditor
import com.emberr.presentation.shared.editor.EditorActions
import com.emberr.presentation.shared.editor.markdown.MarkdownShortcut
import com.emberr.presentation.settings.SettingsScreen
import com.emberr.presentation.settings.SettingsSubPage
import com.emberr.presentation.shared.UserSettings
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrPillShadowAmbientColor
import com.emberr.presentation.shared.components.EmberrPillShadowSpotColor
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.EmberrUpdatePeek
import com.emberr.presentation.shared.components.EmberrVerticalScrollbar
import com.emberr.presentation.shared.canvas.CanvasScreen
import com.emberr.presentation.shared.components.smoothWheelScroll
import com.emberr.presentation.sync.SyncViewModel
import com.emberr.domain.util.eventbus.WidgetCalendarEventBus
import com.emberr.domain.util.system.isDebugBuild
import com.emberr.presentation.calendar.CalendarEvent
import com.emberr.presentation.calendar.CalendarEventSearchResult
import com.emberr.presentation.calendar.CalendarScreen
import com.emberr.presentation.calendar.matchingDetail
import com.emberr.presentation.calendar.searchResultDateLabel
import com.emberr.presentation.calendar.toCategoryColor
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import com.emberr.presentation.daily.CollapsedWeekStrip
import com.emberr.presentation.daily.DailyEditorPane
import com.emberr.presentation.daily.DailyEditorViewModel
import com.emberr.presentation.daily.DailyTimelineDialog
import com.emberr.presentation.home.DesktopSortMenu
import com.emberr.presentation.home.DropInsertPosition
import com.emberr.presentation.home.HomeViewModel
import com.emberr.presentation.home.HomeItem
import com.emberr.presentation.home.HomeItemKey
import com.emberr.presentation.home.TemplatesDesktopMenu
import com.emberr.presentation.home.ROOT_TREE_GUIDE_LINES
import com.emberr.presentation.home.TreeRowFadeInSpec
import com.emberr.presentation.home.TreeRowFadeOutSpec
import com.emberr.presentation.home.TreeRowPlacementSpec
import com.emberr.presentation.home.TreeSelectionMenu
import com.emberr.presentation.home.buildTreeGuideLines
import com.emberr.presentation.home.flattenFolderTree
import com.emberr.presentation.home.rowsBetween
import com.emberr.presentation.home.treeSelectionMenu
import com.emberr.presentation.home.note.NoteScreen
import com.emberr.presentation.home.overview.OverviewSection
import com.emberr.presentation.home.overview.bookmarks.BookmarksScreen
import com.emberr.presentation.home.overview.documents.DocumentsScreen
import com.emberr.presentation.home.overview.images.ImagesScreen
import com.emberr.presentation.home.overview.tasks.TasksScreen
import com.emberr.domain.model.NoteSearchResult
import com.emberr.domain.model.PropertyDateRange
import com.emberr.presentation.search.SearchViewModel
import com.emberr.presentation.search.canBeFavoritedOrTrashed
import com.emberr.presentation.search.canBeRenamed
import com.emberr.presentation.search.dailyDateToOpen
import com.emberr.presentation.search.isPinnedBlocks
import com.emberr.presentation.search.favoriteOptionLabel
import com.emberr.presentation.search.defaultHighlightStyle
import com.emberr.presentation.search.highlightMatches
import com.emberr.presentation.trash.TrashScreen
import com.emberr.presentation.properties.PropertiesScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import com.emberr.domain.reminders.ReminderClickBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.IconTooltipPosition
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.EmberrShadowElevation
import com.emberr.presentation.shared.components.TopBarIconButtonGroup
import com.emberr.presentation.shared.components.customEmberrShadow
import com.emberr.presentation.shared.components.TopBarIconButtonItem
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isBackPressed
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.emberr.domain.reminders.ReminderTargetResolver
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.LocalEmberrBlurSource
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.emberrBorder
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.arrow_up_down
import emberr.shared.generated.resources.astroid
import emberr.shared.generated.resources.calendar
import emberr.shared.generated.resources.sidebar
import emberr.shared.generated.resources.ellipsis
import emberr.shared.generated.resources.history2
import emberr.shared.generated.resources.pen_square
import emberr.shared.generated.resources.folder_plus
import emberr.shared.generated.resources.refresh_cw
import emberr.shared.generated.resources.search
import emberr.shared.generated.resources.x
import org.jetbrains.compose.resources.painterResource
import java.awt.Cursor

private val PANEL_PADDING = 16.dp
private val PANEL_TOP_MARGIN = 12.dp
private val DesktopPanelShape = RoundedCornerShape(18.dp)

// Right-panel state. Replaces Home's boolean flags.
sealed interface DetailPane {
    data class Daily(val date: LocalDate) : DetailPane
    data object Empty : DetailPane
    data class Note(val noteId: String) : DetailPane
    data object Settings : DetailPane
    data object SelfHostSetup : DetailPane
    data object EmberrCloud : DetailPane
    data object About : DetailPane
    data object ImportNotes : DetailPane
    data object OpenSourceLicenses : DetailPane
    data object Trash : DetailPane
    data object Properties : DetailPane
    data object Reminders : DetailPane
    data object Bookmarks : DetailPane
    data object Images : DetailPane
    data object Documents : DetailPane
    data object Calendar : DetailPane
}

private fun DetailPane?.settingsSubPage(): SettingsSubPage? = when (this) {
    DetailPane.SelfHostSetup -> SettingsSubPage.SELF_HOST
    DetailPane.EmberrCloud -> if (isDebugBuild) SettingsSubPage.EMBERR_CLOUD else null
    DetailPane.ImportNotes -> SettingsSubPage.IMPORT
    DetailPane.About -> SettingsSubPage.ABOUT
    DetailPane.OpenSourceLicenses -> SettingsSubPage.OPEN_SOURCE_LICENSES
    else -> null
}

private fun SettingsSubPage?.toDetailPane(): DetailPane = when (this) {
    null -> DetailPane.Settings
    SettingsSubPage.SELF_HOST -> DetailPane.SelfHostSetup
    SettingsSubPage.EMBERR_CLOUD -> if (isDebugBuild) DetailPane.EmberrCloud else DetailPane.Settings
    SettingsSubPage.IMPORT -> DetailPane.ImportNotes
    SettingsSubPage.ABOUT -> DetailPane.About
    SettingsSubPage.OPEN_SOURCE_LICENSES -> DetailPane.OpenSourceLicenses
}

internal fun DetailPane.encode(): String = when (this) {
    is DetailPane.Daily -> "DAILY:${date}"
    DetailPane.Empty -> "PANEL:EMPTY"
    is DetailPane.Note -> "NOTE:${noteId}"
    DetailPane.Settings -> "PANEL:SETTINGS"
    DetailPane.SelfHostSetup -> "PANEL:SELFHOST"
    DetailPane.EmberrCloud -> "PANEL:EMBERR_CLOUD"
    DetailPane.About -> "PANEL:ABOUT"
    DetailPane.ImportNotes -> "PANEL:IMPORT_NOTES"
    DetailPane.OpenSourceLicenses -> "PANEL:LICENSES"
    DetailPane.Trash -> "PANEL:TRASH"
    DetailPane.Properties -> "PANEL:PROPERTIES"
    DetailPane.Reminders -> "PANEL:REMINDERS"
    DetailPane.Bookmarks -> "PANEL:BOOKMARKS"
    DetailPane.Images -> "PANEL:IMAGES"
    DetailPane.Documents -> "PANEL:DOCUMENTS"
    DetailPane.Calendar -> "PANEL:CALENDAR"
}

internal fun decodeDetailPane(raw: String, today: LocalDate): DetailPane = when {
    raw.startsWith("DAILY:") -> runCatching { DetailPane.Daily(LocalDate.parse(raw.removePrefix("DAILY:"))) }
        .getOrElse { DetailPane.Daily(today) }
    raw.startsWith("NOTE:") -> DetailPane.Note(raw.removePrefix("NOTE:"))
    raw == "PANEL:EMPTY" -> DetailPane.Empty
    raw == "PANEL:SETTINGS" -> DetailPane.Settings
    raw == "PANEL:SELFHOST" -> DetailPane.SelfHostSetup
    raw == "PANEL:EMBERR_CLOUD" -> if (isDebugBuild) DetailPane.EmberrCloud else DetailPane.Settings
    raw == "PANEL:ABOUT" -> DetailPane.About
    raw == "PANEL:IMPORT_NOTES" -> DetailPane.ImportNotes
    raw == "PANEL:LICENSES" -> DetailPane.OpenSourceLicenses
    raw == "PANEL:TRASH" -> DetailPane.Trash
    raw == "PANEL:PROPERTIES" -> DetailPane.Properties
    raw == "PANEL:REMINDERS" -> DetailPane.Reminders
    raw == "PANEL:BOOKMARKS" -> DetailPane.Bookmarks
    raw == "PANEL:IMAGES" -> DetailPane.Images
    raw == "PANEL:DOCUMENTS" -> DetailPane.Documents
    raw == "PANEL:CALENDAR" -> DetailPane.Calendar
    else -> DetailPane.Daily(today)
}

private fun OverviewSection.detailPane(): DetailPane = when (this) {
    OverviewSection.TASKS -> DetailPane.Reminders
    OverviewSection.BOOKMARKS -> DetailPane.Bookmarks
    OverviewSection.IMAGES -> DetailPane.Images
    OverviewSection.DOCUMENTS -> DetailPane.Documents
}

@Composable
private fun Modifier.noRippleClickable(interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }, onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )

private const val SEARCH_EMPTY_MESSAGE_DELAY_MS = 350L
private val SidebarSearchResultShape = RoundedCornerShape(10.dp)
private const val COLLAPSED_EVENT_RESULT_COUNT = 3

@Composable
private fun SidebarSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    shouldRequestFocus: Boolean,
    onFocusRequestHandled: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(shouldRequestFocus) {
        if (shouldRequestFocus) {
            runCatching { focusRequester.requestFocus() }
            onFocusRequestHandled()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .customEmberrShadow(CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.search),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onPreviewKeyEvent { keyEvent ->
                    val isEscapePressed = keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape
                    if (isEscapePressed && query.isNotEmpty()) {
                        onQueryChange("")
                        true
                    } else {
                        false
                    }
                },
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search anything",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (query.isNotEmpty()) {
            val clearInteractionSource = remember { MutableInteractionSource() }
            IconTooltip(text = "Clear search") {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .noRippleClickable(clearInteractionSource) { onQueryChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.x),
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarSearchResults(
    query: String,
    results: List<NoteSearchResult>,
    eventResults: List<CalendarEventSearchResult>,
    onResultClick: (NoteSearchResult) -> Unit,
    onEventClick: (CalendarEvent) -> Unit,
    openInSplitViewActionFor: (NoteSearchResult) -> (() -> Unit)?,
    onRenameNote: (noteId: String, newTitle: String) -> Unit,
    onSetNoteFavorite: (noteId: String, isFavorite: Boolean) -> Unit,
    onTrashNote: (noteId: String) -> Unit,
    dragState: DesktopListDragState,
    rowHeightPx: Float,
    onDropOutsideList: (payload: String, cursorInWindow: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val resultsListState = rememberLazyListState()
    var hasSettledOnNoResults by remember { mutableStateOf(false) }
    var areAllEventsShown by remember(query) { mutableStateOf(false) }
    val shownEventResults = if (areAllEventsShown) eventResults else eventResults.take(COLLAPSED_EVENT_RESULT_COUNT)
    val isShowAllEventsVisible = shownEventResults.size < eventResults.size
    val rowCountBeforeNotes = if (eventResults.isEmpty()) {
        0
    } else {
        1 + shownEventResults.size + (if (isShowAllEventsVisible) 1 else 0) + (if (results.isNotEmpty()) 1 else 0)
    }

    LaunchedEffect(query, results, eventResults) {
        hasSettledOnNoResults = false
        if (results.isEmpty() && eventResults.isEmpty()) {
            delay(SEARCH_EMPTY_MESSAGE_DELAY_MS)
            hasSettledOnNoResults = true
        }
    }

    LaunchedEffect(query) {
        if (resultsListState.firstVisibleItemIndex > 0) resultsListState.scrollToItem(0)
    }

    Box(modifier = modifier) {
        if (results.isEmpty() && eventResults.isEmpty()) {
            if (hasSettledOnNoResults) {
                Text(
                    text = "No notes or events match \"$query\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 20.dp, vertical = 28.dp)
                )
            }
        } else {
            LazyColumn(
                state = resultsListState,
                modifier = Modifier
                    .fillMaxSize()
                    .desktopListDragTracker(
                        dragState = dragState,
                        listState = resultsListState,
                        rowKeys = List(rowCountBeforeNotes) { null } +
                            results.map { result -> HomeItemKey.forNote(result.note.noteId) },
                        payloadForKey = { key -> key?.let { "$DRAG_PREFIX_NOTE${HomeItemKey.noteIdOf(it)}" } },
                        isDropTarget = { _, _ -> false },
                        onDrop = { _, _, _ -> },
                        onDropOutsideList = onDropOutsideList,
                        rowHeightPx = rowHeightPx
                    )
                    .smoothWheelScroll(resultsListState),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 16.dp)
            ) {
                if (eventResults.isNotEmpty()) {
                    item(key = "events_header") { SidebarSearchSectionTitle("Events") }
                    itemsIndexed(
                        shownEventResults,
                        key = { _, eventResult -> "event_${eventResult.event.blockId}_${eventResult.event.dateString}" }
                    ) { index, eventResult ->
                        Column {
                            SidebarEventSearchResultRow(
                                result = eventResult,
                                query = query,
                                onClick = { onEventClick(eventResult.event) }
                            )
                            if (index < shownEventResults.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                    if (isShowAllEventsVisible) {
                        item(key = "events_show_all") {
                            Text(
                                text = "Show all ${eventResults.size} events",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .noRippleClickable { areAllEventsShown = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (results.isNotEmpty()) {
                        item(key = "notes_header") {
                            SidebarSearchSectionTitle("Notes", modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                }
                itemsIndexed(results, key = { _, result -> result.note.noteId }) { index, result ->
                    Column {
                        SidebarSearchResultRow(
                            result = result,
                            query = query,
                            onClick = { onResultClick(result) },
                            onOpenInSplitView = openInSplitViewActionFor(result),
                            onRename = { newTitle -> onRenameNote(result.note.noteId, newTitle) },
                            onSetFavorite = { isFavorite -> onSetNoteFavorite(result.note.noteId, isFavorite) },
                            onDelete = { onTrashNote(result.note.noteId) }
                        )
                        if (index < results.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            )
                        }
                    }
                }
            }
            EmberrVerticalScrollbar(
                listState = resultsListState,
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SidebarSearchSectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        modifier = modifier.padding(start = 10.dp, bottom = 4.dp)
    )
}

@Composable
private fun SidebarEventSearchResultRow(
    result: CalendarEventSearchResult,
    query: String,
    onClick: () -> Unit
) {
    val event = result.event
    val trimmedQuery = query.trim()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val rowBackground by animateColorAsState(
        sidebarRowBackground(isActive = false, isSelected = false, isHovered = isHovered),
        tween(180, easing = FastOutSlowInEasing),
        label = "event_search_result_bg"
    )
    val highlightStyle = defaultHighlightStyle(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    val matchingDetail = event.matchingDetail(trimmedQuery)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SidebarSearchResultShape)
            .background(rowBackground)
            .noRippleClickable(interactionSource, onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        result.category?.colorHex?.toCategoryColor() ?: MaterialTheme.colorScheme.outline,
                        CircleShape
                    )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = highlightMatches(event.text.ifBlank { "Untitled event" }, trimmedQuery, highlightStyle),
                style = sidebarRowTextStyle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        matchingDetail?.let { detail ->
            Text(
                text = highlightMatches(detail, trimmedQuery, highlightStyle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = event.searchResultDateLabel(LocalUsesTwentyFourHourClock.current),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
            if (event.recurrenceRule != null) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    painter = painterResource(Res.drawable.refresh_cw),
                    contentDescription = "Repeats",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun SidebarSearchResultRow(
    result: NoteSearchResult,
    query: String,
    onClick: () -> Unit,
    onOpenInSplitView: (() -> Unit)?,
    onRename: (String) -> Unit,
    onSetFavorite: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val hasContextMenu = onOpenInSplitView != null || result.canBeRenamed || result.canBeFavoritedOrTrashed
    var showContextMenu by remember { mutableStateOf(false) }
    var showRenamePopup by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val rowBackground by animateColorAsState(
        sidebarRowBackground(isActive = false, isSelected = false, isHovered = isHovered),
        tween(180, easing = FastOutSlowInEasing),
        label = "search_result_bg"
    )
    val highlightStyle = defaultHighlightStyle(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    val dailyDateString = result.note.dateString?.takeIf { result.note.isDaily }
    val titleText = if (result.isPinnedBlocks) "Pinned" else dailyDateString ?: result.note.title.ifBlank { "Untitled" }
    val snippetText = result.matchedText.takeIf { it.isNotBlank() && it != result.note.title }

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onPointerEvent(PointerEventType.Press) { event ->
                    if (!hasContextMenu || !event.buttons.isSecondaryPressed) return@onPointerEvent
                    val pressPosition = event.changes.first().position
                    contextMenuOffset = with(density) { DpOffset(pressPosition.x.toDp(), pressPosition.y.toDp()) }
                    showContextMenu = true
                }
                .clip(SidebarSearchResultShape)
                .background(rowBackground)
                .noRippleClickable(interactionSource, onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = highlightMatches(titleText, query, highlightStyle),
                    style = sidebarRowTextStyle,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (dailyDateString != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Daily",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
            if (snippetText != null) {
                Text(
                    text = highlightMatches(snippetText, query, highlightStyle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            result.parentTitle?.let { parentTitle ->
                Text(
                    text = "in $parentTitle",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (hasContextMenu) {
            Box(modifier = Modifier.offset(x = contextMenuOffset.x, y = contextMenuOffset.y)) {
                EmberrDesktopMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false },
                    offset = DpOffset.Zero
                ) {
                    SidebarMenuItems {
                        if (onOpenInSplitView != null) {
                            SidebarMenuItem("Open in split view", Icons.Default.VerticalSplit) {
                                showContextMenu = false
                                onOpenInSplitView()
                            }
                        }
                        if (result.canBeRenamed) {
                            SidebarMenuItem("Rename", Icons.Default.Edit) {
                                showContextMenu = false
                                showRenamePopup = true
                            }
                        }
                        if (result.canBeFavoritedOrTrashed) {
                            SidebarMenuItem(result.favoriteOptionLabel, Icons.Default.Star) {
                                showContextMenu = false
                                onSetFavorite(!result.note.isFavorite)
                            }
                            SidebarMenuItem("Delete", Icons.Default.Delete, isDestructive = true) {
                                showContextMenu = false
                                onDelete()
                            }
                        }
                    }
                }
                EmberrDesktopMenu(
                    expanded = showRenamePopup,
                    onDismissRequest = { showRenamePopup = false },
                    modifier = Modifier.width(280.dp)
                ) {
                    DesktopNamePopup(
                        title = "Rename Note",
                        initialValue = result.note.title,
                        confirmLabel = "Save",
                        onConfirm = { newTitle ->
                            onRename(newTitle)
                            showRenamePopup = false
                        },
                        onDismiss = { showRenamePopup = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewRow(
    icon: Painter,
    title: String,
    subtitle: String,
    isSelected: Boolean = false,
    iconSize: Dp = 22.dp,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val rowBackground by animateColorAsState(
        sidebarRowBackground(isActive = isSelected, isSelected = false, isHovered = isHovered),
        tween(180, easing = FastOutSlowInEasing),
        label = "overview_bg_$title"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(rowBackground)
            .noRippleClickable(interactionSource, onClick)
            .heightIn(min = 40.dp)
            .padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected || isHovered) 0.9f else 0.55f),
                modifier = Modifier.size(iconSize)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = sidebarRowTextStyle,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSelected) 1f else 0.82f),
            modifier = Modifier.weight(1f)
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun SidebarGroupSeparator() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    )
}

private const val FAVORITE_ROW_PREFIX = "sb_fav_"

private val MIN_PANEL_WIDTH = 240.dp
private val MAX_PANEL_WIDTH = 520.dp
private val MIN_RAG_PANEL_WIDTH = 320.dp
private val MAX_RAG_PANEL_WIDTH = 640.dp
private val DEFAULT_RAG_PANEL_WIDTH = 400.dp

private const val NEARBY_DAILY_PREFETCH_RADIUS = 3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun DesktopMainScreen(
    homeViewModel: HomeViewModel = koinViewModel(),
    dailyViewModel: DailyEditorViewModel = koinViewModel(),
    syncViewModel: SyncViewModel = koinViewModel(),
    spaceViewModel: com.emberr.presentation.space.SpaceViewModel = koinViewModel(),
    searchViewModel: SearchViewModel = koinViewModel(),
    reminderTargetResolver: ReminderTargetResolver = koinInject(),
    settingsManager: SettingsManager = koinInject(),
    noteRepository: com.emberr.domain.repository.NoteRepository = koinInject(),
    isSidebarVisible: Boolean = true,
    sidebarWidth: Dp = 340.dp,
    onToggleSidebar: () -> Unit = {},
    onSelectionModeChange: (Boolean) -> Unit = {},
    onPickImage: (onPathSelected: (String) -> Unit) -> Unit = {},
    onTakePhoto: (onPathSelected: (String) -> Unit) -> Unit = {},
    onPickDocument: (onPathSelected: (String) -> Unit) -> Unit = {},
    onOpenFile: (filePath: String, mimeType: String) -> Unit = { _, _ -> },
    onExportMarkdown: (fileName: String, content: String) -> Unit = { _, _ -> },
    onExportPdf: (fileName: String, title: String, blocks: List<NoteBlock>) -> Unit = { _, _, _ -> },
    onExportBackup: (spaceIds: Set<String>) -> Unit = {},
    onAiIconTap: () -> Unit = {},
    isAiChatVisible: Boolean = false,
    ragViewModel: com.emberr.presentation.ai.RagViewModel? = null,
    onDismissAiChat: () -> Unit = {},
    isUpdatePeekDue: Boolean = false,
) {
    val hazeState = remember { HazeState() }
    var isUpdatePeekShowing by remember { mutableStateOf(false) }
    val savedWidth by settingsManager.desktopSidebarWidthFlow.collectAsState(initial = sidebarWidth.value)
    val isAiDisabled by settingsManager.aiFeaturesDisabledFlow.collectAsState(
        initial = settingsManager.isAiFeaturesDisabled()
    )
    val isDailyNotesEnabled by settingsManager.dailyNotesEnabledFlow.collectAsState(
        initial = settingsManager.isDailyNotesEnabled()
    )
    var panelWidth by remember { mutableStateOf(sidebarWidth) }
    var hasLoadedWidth by remember { mutableStateOf(false) }
    var ragPanelWidth by remember { mutableStateOf(DEFAULT_RAG_PANEL_WIDTH) }

    LaunchedEffect(savedWidth) {
        if (!hasLoadedWidth) {
            panelWidth = savedWidth.dp
            hasLoadedWidth = true
        }
    }
    val today = remember { Clock.System.todayIn(TimeZone.currentSystemDefault()) }

    // Home data
    val isLoading by homeViewModel.isLoading.collectAsState()
    val favoriteNotes by homeViewModel.favoriteNotes.collectAsState()
    val recentNotes by homeViewModel.recentNotes.collectAsState()
    val selectedNoteIds by homeViewModel.selectedNoteIds.collectAsState()
    val selectedFolderIds by homeViewModel.selectedFolderIds.collectAsState()
    val foldersByParent by homeViewModel.foldersByParent.collectAsState()
    val notesByFolder by homeViewModel.notesByFolder.collectAsState()
    val expandedFolderIds by homeViewModel.expandedFolderIds.collectAsState()
    val currentSortType by homeViewModel.sortType.collectAsState()
    val currentSortOrder by homeViewModel.sortOrder.collectAsState()
    val remindersCount by homeViewModel.remindersCount.collectAsState()
    val bookmarksCount by homeViewModel.bookmarksCount.collectAsState()
    val imagesCount by homeViewModel.imagesCount.collectAsState()
    val documentsCount by homeViewModel.documentsCount.collectAsState()
    val visibleOverviewSections by homeViewModel.visibleOverviewSections.collectAsState()
    val templates by homeViewModel.filteredTemplates.collectAsState()
    val templateSearchQuery by homeViewModel.templateSearchQuery.collectAsState()

    // Daily data (for strip + sheets)
    val selectedDate by dailyViewModel.selectedDate.collectAsState()

    LaunchedEffect(selectedDate) {
        val nearbyDateStrings = (-NEARBY_DAILY_PREFETCH_RADIUS..NEARBY_DAILY_PREFETCH_RADIUS).map { dayOffset ->
            selectedDate.plus(dayOffset, DateTimeUnit.DAY).toString()
        }
        dailyViewModel.evictPreviewCache(nearbyDateStrings.toSet())
        nearbyDateStrings.forEach { dateString -> dailyViewModel.prefetchDateIfNeeded(dateString) }
    }

    val isSelectionMode = selectedNoteIds.isNotEmpty() || selectedFolderIds.isNotEmpty()

    // Right panel state
    val lastOpenedState by settingsManager.lastOpenedDesktopStateFlow.collectAsState(initial = "")
    val spaces by spaceViewModel.spaces.collectAsState()
    val activeSpaceId by spaceViewModel.activeSpaceId.collectAsState()

    val splitState = rememberDesktopSplitState()
    var detail by splitState::focusedPane
    var hasRestored by remember { mutableStateOf(false) }

    LaunchedEffect(lastOpenedState, isLoading) {
        if (!hasRestored && !isLoading) {
            fun restorablePane(decoded: DetailPane): DetailPane {
                val decodedOverviewSection = OverviewSection.entries.firstOrNull { section -> section.detailPane() == decoded }
                return when {
                    decoded is DetailPane.Daily && !isDailyNotesEnabled -> DetailPane.Empty
                    decoded is DetailPane.Empty && isDailyNotesEnabled -> DetailPane.Daily(today)
                    decodedOverviewSection != null && decodedOverviewSection !in visibleOverviewSections ->
                        if (isDailyNotesEnabled) DetailPane.Daily(today) else DetailPane.Empty
                    else -> decoded
                }
            }

            val savedSplit = decodeSavedSplitLayout(lastOpenedState, today)
            val restorableSplitPanes = savedSplit?.panes.orEmpty().filter { pane -> restorablePane(pane) == pane }
            if (savedSplit != null && restorableSplitPanes.size >= 2) {
                splitState.restoreSplit(savedSplit.keepingOnly(restorableSplitPanes))
            } else {
                val decoded = when {
                    savedSplit != null -> restorableSplitPanes.firstOrNull() ?: savedSplit.focusedPane
                    lastOpenedState.isBlank() -> DetailPane.Daily(today)
                    else -> decodeDetailPane(lastOpenedState, today)
                }
                detail = restorablePane(decoded)
            }
            splitState.panes
                .filterIsInstance<DetailPane.Daily>()
                .firstOrNull()
                ?.let { dailyPane -> dailyViewModel.selectDate(dailyPane.date) }
            hasRestored = true
        }
    }

    val saveOpenedState: () -> Unit = {
        splitState.encodeOpenedState()?.let { openedState -> settingsManager.saveLastOpenedDesktopState(openedState) }
    }

    LaunchedEffect(splitState.panes, splitState.focusedIndex) {
        saveOpenedState()
    }

    LaunchedEffect(isDailyNotesEnabled) {
        if (!isDailyNotesEnabled && detail is DetailPane.Daily) detail = DetailPane.Empty
        if (!isDailyNotesEnabled) splitState.closeUnfocusedPanesWhere { pane -> pane is DetailPane.Daily }
    }

    // Menus / popups
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showAddNotePopup by remember { mutableStateOf(false) }
    var showAddFolderPopup by remember { mutableStateOf(false) }
    var addFolderInput by remember { mutableStateOf("") }

    var isFavoritesExpanded by remember { mutableStateOf(true) }
    var isNotesExpanded by remember { mutableStateOf(true) }
    var isRecentsExpanded by remember { mutableStateOf(true) }
    var isPeeking by remember { mutableStateOf(false) }

    // Sheets
    var showTimelineDialog by remember { mutableStateOf(false) }
    val timelineDays by dailyViewModel.timelineDays.collectAsState()
    val isTimelineLoading by dailyViewModel.isTimelineLoading.collectAsState()
    // Sidebar search
    val searchQuery by searchViewModel.query.collectAsState()
    val searchResults by searchViewModel.results.collectAsState()
    val eventSearchResults by searchViewModel.eventResults.collectAsState()
    val isSearchActive = searchQuery.isNotBlank()
    val searchFieldFocusRequester = remember { FocusRequester() }
    var shouldFocusSearchField by remember { mutableStateOf(false) }
    val isSidebarVisibleNow by rememberUpdatedState(isSidebarVisible)
    LaunchedEffect(Unit) {
        DesktopSearchShortcutBus.requests.collect {
            if (!isSidebarVisibleNow) onToggleSidebar()
            shouldFocusSearchField = true
        }
    }
    fun goBackFromSettingsSubPage() {
        val openSubPage = detail.settingsSubPage() ?: return
        detail = openSubPage.parentPage.toDetailPane()
    }
    LaunchedEffect(Unit) {
        DesktopEscapeKeyBus.presses.collect { goBackFromSettingsSubPage() }
    }

    // Sync
    val snackbarHostState = remember { SnackbarHostState() }
    val syncState by syncViewModel.syncStatus.collectAsState()

    LaunchedEffect(isSidebarVisible) {
        if (isSidebarVisible) isPeeking = false
    }

    val openDaily: (LocalDate) -> Unit = { date ->
        dailyViewModel.selectDate(date)
        detail = DetailPane.Daily(date)
        isPeeking = false
    }
    val openNote: (String) -> Unit = { id ->
        val notePane = DetailPane.Note(id)
        if (splitState.isSplit && splitState.canAddPane(notePane)) splitState.addPane(notePane, shouldFocusNewPane = true)
        else detail = notePane
        isPeeking = false
    }
    val openSubNote: (String) -> Unit = { id ->
        splitState.exitSplitAndShow(DetailPane.Note(id))
        isPeeking = false
    }
    val closeDetailPane: () -> Unit = {
        if (splitState.isSplit) splitState.closePane(splitState.focusedIndex)
        else detail = if (isDailyNotesEnabled) DetailPane.Daily(selectedDate) else DetailPane.Empty
    }

    LaunchedEffect(syncState) {
        if (syncState != "Idle" && syncState != "Syncing...") {
            snackbarHostState.showSnackbar(message = syncState)
            syncViewModel.resetSyncStatus()
        }
    }

    LaunchedEffect(isSelectionMode) { onSelectionModeChange(isSelectionMode) }

    val handleCreateNote = { kind: NoteKind ->
        homeViewModel.createNewNote(title = "", forceHomeFolder = false, kind = kind) { newId -> openNote(newId) }
    }
    val handleCreateFolder = { name: String -> homeViewModel.createNewFolder(name) }

    val handleTemplateClick = { templateId: String ->
        homeViewModel.createNoteFromTemplate(templateId) { newId -> openNote(newId) }
    }
    // Opens the template's own note directly (no clone) - the editor shows an "Editing
    // Template" pill for any note with isTemplate = true, so no separate mode is needed here.
    val handleEditTemplate = { templateId: String -> openNote(templateId) }
    val handleCreateNewTemplate = {
        homeViewModel.saveAsTemplate(title = "", content = NoteContent(blocks = emptyList())) { newId -> openNote(newId) }
    }

    var selectionAnchorKey by remember { mutableStateOf<String?>(null) }

    // Drag
    val dragState = rememberDesktopListDragState()
    var screenOriginInWindow by remember { mutableStateOf(Offset.Zero) }
    val splitDropTarget by remember {
        derivedStateOf {
            val isDraggingNoteOverContent = dragState.dragging &&
                    dragState.isOutsideList &&
                    dragState.payload?.startsWith(DRAG_PREFIX_NOTE) == true
            if (isDraggingNoteOverContent) splitState.dropTargetAt(dragState.cursorInWindow) else null
        }
    }
    val sidebarListState = rememberLazyListState()
    val hasOverviewRows = visibleOverviewSections.isNotEmpty()
    DisposableEffect(hasOverviewRows) {
        val isSidebarAtTop = sidebarListState.firstVisibleItemIndex == 0 &&
                sidebarListState.firstVisibleItemScrollOffset == 0
        if (isSidebarAtTop) sidebarListState.requestScrollToItem(0)
        onDispose {}
    }
    val density = LocalDensity.current
    val rowHeightPx = with(density) { SIDEBAR_ROW_HEIGHT.toPx() }

    var previousSpaceId by remember { mutableStateOf(activeSpaceId) }

    var isOpeningReminderTarget by remember { mutableStateOf(false) }

    LaunchedEffect(activeSpaceId) {
        if (previousSpaceId == activeSpaceId) return@LaunchedEffect
        previousSpaceId = activeSpaceId

        val keepCurrentPane = isOpeningReminderTarget
        isOpeningReminderTarget = false

        splitState.closeUnfocusedPanesWhere { pane -> pane is DetailPane.Note }
        if (detail is DetailPane.Note && !keepCurrentPane) {
            if (isDailyNotesEnabled) {
                dailyViewModel.selectDate(today)
                detail = DetailPane.Daily(today)
            } else {
                detail = DetailPane.Empty
            }
        }
        sidebarListState.scrollToItem(0)
    }

    LaunchedEffect(Unit) {
        ReminderClickBus.pendingBlockId.filterNotNull().collect { blockId ->
            val target = withContext(Dispatchers.IO) { reminderTargetResolver.resolve(blockId) }
            ReminderClickBus.consumePendingBlockId()
            if (target == null) return@collect

            if (target.spaceId != activeSpaceId && target.spaceId != ACCOUNT_SPACE_ID) {
                isOpeningReminderTarget = true
                spaceViewModel.openSpace(target.spaceId)
            }

            val targetDate = target.dateString?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            if (target.isDaily && targetDate != null) {
                dailyViewModel.selectDate(targetDate)
                detail = DetailPane.Daily(targetDate)
            } else {
                detail = DetailPane.Note(target.noteId)
            }
            isPeeking = false
        }
    }

    fun openInSplitViewActionFor(pane: DetailPane, isPartOfSelection: Boolean = false): (() -> Unit)? {
        if (isPartOfSelection || !splitState.canAddPane(pane)) return null
        return {
            if (pane is DetailPane.Daily) dailyViewModel.selectDate(pane.date)
            splitState.addPane(pane, shouldFocusNewPane = false)
            isPeeking = false
        }
    }

    fun paneForSearchResult(result: NoteSearchResult): DetailPane {
        val dailyDateString = result.dailyDateToOpen
        return if (dailyDateString != null) DetailPane.Daily(LocalDate.parse(dailyDateString)) else DetailPane.Note(result.note.noteId)
    }

    val dropNoteOnContent: (String, Offset) -> Unit = { payload, cursorInWindow ->
        val dropTarget = splitState.dropTargetAt(cursorInWindow)
        if (payload.startsWith(DRAG_PREFIX_NOTE) && dropTarget != null) {
            val droppedNoteId = payload.removePrefix(DRAG_PREFIX_NOTE)
            val droppedPane = searchResults.firstOrNull { result -> result.note.noteId == droppedNoteId }
                ?.let { result -> paneForSearchResult(result) }
                ?: DetailPane.Note(droppedNoteId)
            splitState.dropPane(droppedPane, dropTarget)
            if (droppedPane is DetailPane.Daily && splitState.isShowing(droppedPane)) dailyViewModel.selectDate(droppedPane.date)
            isPeeking = false
        }
    }

    val settingsMenuSlot = @Composable {
        UserSettings(
            expanded = showSettingsMenu,
            onDismiss = { showSettingsMenu = false },
            onNavigateToSettings = { showSettingsMenu = false; detail = DetailPane.Settings },
            onNavigateToProperties = { showSettingsMenu = false; detail = DetailPane.Properties },
            onNavigateToTrash = { showSettingsMenu = false; detail = DetailPane.Trash }
        )
    }

    // LEFT PANEL
    val leftPanel = @Composable { startPadding: Dp, endPadding: Dp ->
        val treeRows = if (isNotesExpanded) flattenFolderTree(
            null, 0, foldersByParent, notesByFolder, expandedFolderIds,
            sortType = currentSortType,
            sortOrder = currentSortOrder
        ) else emptyList()

        val treeGuideLines = remember(treeRows) { treeRows.buildTreeGuideLines() }

        val favoriteNoteIds = remember(favoriteNotes) { favoriteNotes.map { it.noteId }.toSet() }

        val favoriteRows = remember(favoriteNotes) { favoriteNotes.map { HomeItem.Note(it) } }

        fun handleRowClick(
            rowsInSection: List<HomeItem>,
            rowKey: String,
            modifiers: SidebarClickModifiers,
            openRow: () -> Unit
        ) {
            val anchorKeyInSection =
                selectionAnchorKey?.takeIf { anchor -> rowsInSection.any { it.key == anchor } } ?: rowKey
            val rowsBetweenAnchorAndClick =
                if (modifiers.extendSelection) rowsInSection.rowsBetween(anchorKeyInSection, rowKey)
                else emptyList()

            when {
                modifiers.extendSelection && rowsBetweenAnchorAndClick.isNotEmpty() -> {
                    homeViewModel.addToSelection(
                        noteIds = rowsBetweenAnchorAndClick.filterIsInstance<HomeItem.Note>().map { it.note.noteId },
                        folderIds = rowsBetweenAnchorAndClick.filterIsInstance<HomeItem.Folder>().map { it.folder.folderId }
                    )
                }

                modifiers.addToSelection -> {
                    selectionAnchorKey = rowKey
                    if (HomeItemKey.isFolder(rowKey)) homeViewModel.toggleFolderSelection(HomeItemKey.folderIdOf(rowKey))
                    else homeViewModel.toggleNoteSelection(HomeItemKey.noteIdOf(rowKey))
                }

                else -> {
                    selectionAnchorKey = rowKey
                    if (isSelectionMode) homeViewModel.clearSelection()
                    openRow()
                }
            }
        }

        val menuForRow = { rowKey: String ->
            val isRowSelected = when {
                HomeItemKey.isFolder(rowKey) -> selectedFolderIds.contains(HomeItemKey.folderIdOf(rowKey))
                else -> selectedNoteIds.contains(HomeItemKey.noteIdOf(rowKey))
            }
            val targetNoteIds = when {
                isRowSelected -> selectedNoteIds
                HomeItemKey.isNote(rowKey) -> setOf(HomeItemKey.noteIdOf(rowKey))
                else -> emptySet<String>()
            }
            val targetFolderIds = when {
                isRowSelected -> selectedFolderIds
                HomeItemKey.isFolder(rowKey) -> setOf(HomeItemKey.folderIdOf(rowKey))
                else -> emptySet<String>()
            }
            TreeRowMenuTarget(
                noteIds = targetNoteIds,
                folderIds = targetFolderIds,
                usesSelection = isRowSelected,
                menu = treeSelectionMenu(targetNoteIds, targetFolderIds) { noteId -> noteId in favoriteNoteIds }
            )
        }

        val rowKeys: List<String?> = buildList {
            if (visibleOverviewSections.isNotEmpty()) add(null)
            add(null) // Space header

            if (favoriteNotes.isNotEmpty()) {
                add(null) // Favorites header
                if (isFavoritesExpanded) favoriteNotes.forEach { add("$FAVORITE_ROW_PREFIX${it.noteId}") }
            }

            add(null) // Notes header

            if (isNotesExpanded && treeRows.isEmpty()) add(null)
            if (isNotesExpanded) treeRows.forEach { add(it.key) }

            if (recentNotes.isNotEmpty()) {
                add(null) // Recents header
                if (isRecentsExpanded) recentNotes.forEach { add("sb_recent_${it.noteId}") }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(start = startPadding, end = endPadding)) {

                // top: icon row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconTooltip(text = "Hide sidebar") {
                        IconButton(onClick = onToggleSidebar) {
                            Icon(painterResource(Res.drawable.sidebar), "Collapse sidebar", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (isDailyNotesEnabled) {
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            TopBarIconButton(
                                icon = painterResource(Res.drawable.history2),
                                contentDescription = "Open timeline",
                                bgColor = MaterialTheme.colorScheme.background,
                                tint = MaterialTheme.colorScheme.primary,
                                tooltip = "Timeline",
                                onClick = {
                                    dailyViewModel.loadTimeline()
                                    showTimelineDialog = true
                                }
                            )
                        }
                    }
                    Box {
                        TopBarIconButtonGroup(
                            bgColor = MaterialTheme.colorScheme.background,
                            tint = MaterialTheme.colorScheme.primary,
                            items = listOf(
                                TopBarIconButtonItem(
                                    icon = painterResource(Res.drawable.calendar),
                                    contentDescription = "Calendar",
                                    onClick = { detail = DetailPane.Calendar; isPeeking = false },
                                    tooltip = "Calendar"
                                ),
                                TopBarIconButtonItem(
                                    icon = painterResource(Res.drawable.ellipsis),
                                    contentDescription = "Settings",
                                    onClick = { showSettingsMenu = true },
                                    tooltip = "More settings"
                                )
                            )
                        )
                        settingsMenuSlot()
                    }
                }

                SidebarSearchBar(
                    query = searchQuery,
                    onQueryChange = searchViewModel::onQueryChange,
                    focusRequester = searchFieldFocusRequester,
                    shouldRequestFocus = shouldFocusSearchField,
                    onFocusRequestHandled = { shouldFocusSearchField = false },
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 14.dp)
                )

                if (isSearchActive) {
                    SidebarSearchResults(
                        query = searchQuery,
                        results = searchResults,
                        eventResults = eventSearchResults,
                        onResultClick = { result ->
                            if (result.note.isDaily) result.dailyDateToOpen?.let { openDaily(LocalDate.parse(it)) }
                            else openNote(result.note.noteId)
                        },
                        onEventClick = { event ->
                            WidgetCalendarEventBus.requestEvent(event.blockId, event.dateString)
                            detail = DetailPane.Calendar
                            isPeeking = false
                        },
                        openInSplitViewActionFor = { result -> openInSplitViewActionFor(paneForSearchResult(result)) },
                        onRenameNote = { noteId, newTitle ->
                            homeViewModel.renameNote(noteId, newTitle, onDone = searchViewModel::refreshResults)
                        },
                        onSetNoteFavorite = { noteId, isFavorite ->
                            homeViewModel.setNotesFavorite(setOf(noteId), isFavorite, onDone = searchViewModel::refreshResults)
                        },
                        onTrashNote = { noteId -> homeViewModel.trashNote(noteId, onDone = searchViewModel::refreshResults) },
                        dragState = dragState,
                        rowHeightPx = rowHeightPx,
                        onDropOutsideList = dropNoteOnContent,
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    )
                } else {
                    // calendar strip
                    if (isDailyNotesEnabled) {
                        CollapsedWeekStrip(
                            selectedDate = selectedDate,
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 20.dp, bottom = 12.dp),
                            onDateSelected = { openDaily(it) }
                        )
                    } else {
                        Spacer(Modifier.height(12.dp))
                    }

                    // Scrolling: overview rows + favorites + notes tree + recents
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(hazeState)
                                .desktopListDragTracker(
                                    dragState = dragState,
                                    listState = sidebarListState,
                                    rowKeys = rowKeys,
                                    rowHeightPx = rowHeightPx,
                                    payloadForKey = { key ->
                                        when {
                                            key == null -> null
                                            HomeItemKey.isFolder(key) -> "$DRAG_PREFIX_FOLDER${HomeItemKey.folderIdOf(key)}"
                                            HomeItemKey.isNote(key) -> "$DRAG_PREFIX_NOTE${HomeItemKey.noteIdOf(key)}"
                                            key.startsWith(FAVORITE_ROW_PREFIX) -> "$DRAG_PREFIX_NOTE${key.removePrefix(FAVORITE_ROW_PREFIX)}"
                                            key.startsWith("sb_recent_") -> "$DRAG_PREFIX_NOTE${key.removePrefix("sb_recent_")}"
                                            else -> null
                                        }
                                    },
                                    isDropTarget = { key, payload ->
                                        if (key == null) false
                                        else when {
                                            key.startsWith(FAVORITE_ROW_PREFIX) ->
                                                payload.startsWith(DRAG_PREFIX_NOTE) &&
                                                        payload.removePrefix(DRAG_PREFIX_NOTE) in favoriteNoteIds
                                            key.startsWith("sb_recent_") -> false
                                            HomeItemKey.isFolder(key) &&
                                                    payload == "$DRAG_PREFIX_FOLDER${HomeItemKey.folderIdOf(key)}" -> false
                                            else -> true
                                        }
                                    },
                                    onDrop = { payload, targetKey, insertBefore ->
                                        when {
                                            targetKey.startsWith(FAVORITE_ROW_PREFIX) &&
                                                    payload.startsWith(DRAG_PREFIX_NOTE) -> {
                                                homeViewModel.reorderFavoriteNotes(
                                                    draggedNoteId = payload.removePrefix(DRAG_PREFIX_NOTE),
                                                    targetNoteId = targetKey.removePrefix(FAVORITE_ROW_PREFIX),
                                                    insertBefore = insertBefore,
                                                    orderedNoteIds = favoriteNotes.map { it.noteId }
                                                )
                                            }

                                            !insertBefore && HomeItemKey.isFolder(targetKey) &&
                                                    dragState.dropPosition == DropInsertPosition.INTO -> {
                                                val folderId = HomeItemKey.folderIdOf(targetKey)
                                                when {
                                                    payload.startsWith(DRAG_PREFIX_NOTE) -> homeViewModel.moveNote(payload.removePrefix(DRAG_PREFIX_NOTE), folderId)
                                                    payload.startsWith(DRAG_PREFIX_FOLDER) -> homeViewModel.moveFolder(payload.removePrefix(DRAG_PREFIX_FOLDER), folderId)
                                                }
                                            }
                                            else -> {
                                                val draggedKey = when {
                                                    payload.startsWith(DRAG_PREFIX_NOTE) -> HomeItemKey.forNote(payload.removePrefix(DRAG_PREFIX_NOTE))
                                                    payload.startsWith(DRAG_PREFIX_FOLDER) -> HomeItemKey.forFolder(payload.removePrefix(DRAG_PREFIX_FOLDER))
                                                    else -> return@desktopListDragTracker
                                                }
                                                homeViewModel.reorderItems(
                                                    draggedKey = draggedKey,
                                                    targetKey = targetKey,
                                                    insertBefore = insertBefore,
                                                    orderedKeys = treeRows.map { it.key }
                                                )
                                            }
                                        }
                                    },
                                    onDropOutsideList = dropNoteOnContent
                                )
                        ) {
                            LazyColumn(
                                state = sidebarListState,
                                modifier = Modifier.fillMaxSize().smoothWheelScroll(sidebarListState),
                                contentPadding = PaddingValues(bottom = 80.dp)
                            ) {
                                if (visibleOverviewSections.isNotEmpty()) {
                                    item(key = "sb_overview") {
                                        Column(
                                            modifier = Modifier
                                                .animateItem(
                                                    fadeInSpec = TreeRowFadeInSpec,
                                                    fadeOutSpec = TreeRowFadeOutSpec,
                                                    placementSpec = null
                                                )
                                                .padding(top = 4.dp, bottom = 8.dp)
                                        ) {
                                            visibleOverviewSections.forEach { section ->
                                                val sectionPane = section.detailPane()
                                                val subtitle = when (section) {
                                                    OverviewSection.TASKS -> "$remindersCount left"
                                                    OverviewSection.BOOKMARKS -> "$bookmarksCount saved"
                                                    OverviewSection.IMAGES -> "$imagesCount saved"
                                                    OverviewSection.DOCUMENTS -> "$documentsCount attached"
                                                }
                                                OverviewRow(
                                                    painterResource(section.icon),
                                                    section.title,
                                                    subtitle,
                                                    isSelected = splitState.isShowing(sectionPane)
                                                ) { detail = sectionPane; isPeeking = false }
                                            }
                                            SidebarGroupSeparator()
                                        }
                                    }
                                }

                                item(key = "sb_space_header") {
                                    SidebarSpaceHeader(
                                        displayName = spaces.firstOrNull { it.spaceId == activeSpaceId }?.displayName.orEmpty(),
                                        canDelete = spaces.size > 1,
                                        onRename = { name -> spaceViewModel.renameSpace(activeSpaceId, name) },
                                        onDelete = { spaceViewModel.deleteSpace(activeSpaceId) },
                                        modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = TreeRowPlacementSpec)
                                    )
                                }

                                if (favoriteNotes.isNotEmpty()) {
                                    item(key = "sb_favorites_header") {
                                        SidebarSectionHeader(
                                            "Favorites",
                                            isFavoritesExpanded,
                                            { isFavoritesExpanded = !isFavoritesExpanded },
                                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = TreeRowPlacementSpec)
                                        )
                                    }
                                    if (isFavoritesExpanded) {
                                        items(favoriteNotes, key = { "$FAVORITE_ROW_PREFIX${it.noteId}" }) { note ->
                                            val favoriteRowKey = HomeItemKey.forNote(note.noteId)
                                            val rowMenuTarget = menuForRow(favoriteRowKey)
                                            SidebarNoteRow(
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = TreeRowFadeInSpec,
                                                    fadeOutSpec = TreeRowFadeOutSpec,
                                                    placementSpec = TreeRowPlacementSpec
                                                ),
                                                note = note, level = 0,
                                                isActive = splitState.isShowing(DetailPane.Note(note.noteId)),
                                                isSelected = selectedNoteIds.contains(note.noteId),
                                                dragState = dragState,
                                                menu = rowMenuTarget.menu,
                                                onClick = { modifiers ->
                                                    handleRowClick(favoriteRows, favoriteRowKey, modifiers) { openNote(note.noteId) }
                                                },
                                                onOpenInSplitView = openInSplitViewActionFor(DetailPane.Note(note.noteId), rowMenuTarget.usesSelection),
                                                onToggleFavorite = {
                                                    homeViewModel.setNotesFavorite(rowMenuTarget.noteIds, rowMenuTarget.menu.makeFavorite)
                                                },
                                                onRename = { newTitle -> homeViewModel.renameNote(note.noteId, newTitle) },
                                                onDelete = {
                                                    if (rowMenuTarget.usesSelection) homeViewModel.deleteSelectedItems()
                                                    else homeViewModel.trashNote(note.noteId)
                                                },
                                                rowKey = "$FAVORITE_ROW_PREFIX${note.noteId}"
                                            )
                                        }
                                    }
                                }

                                item(key = "sb_notes_header") {
                                    SidebarSectionHeader(
                                        title = "Notes", isExpanded = isNotesExpanded,
                                        onToggle = { isNotesExpanded = !isNotesExpanded },
                                        modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = TreeRowPlacementSpec),
                                        trailing = {
                                            Box {
                                                TopBarIconButtonGroup(
                                                    bgColor = MaterialTheme.colorScheme.background,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    hazeState = hazeState,
                                                    hazeStyle = EmberrBlur.Regular,
                                                    iconSize = 20.dp,
                                                    shadowSpotColor = EmberrPillShadowSpotColor,
                                                    shadowAmbientColor = EmberrPillShadowAmbientColor,
                                                    items = listOf(
                                                        TopBarIconButtonItem(
                                                            icon = painterResource(Res.drawable.arrow_up_down),
                                                            contentDescription = "Sort",
                                                            onClick = { showSortMenu = true },
                                                            tooltip = "Sort notes"
                                                        ),
                                                        TopBarIconButtonItem(
                                                            icon = painterResource(Res.drawable.folder_plus),
                                                            contentDescription = "New folder",
                                                            onClick = { addFolderInput = ""; showAddFolderPopup = true },
                                                            tooltip = "New folder"
                                                        ),
                                                        TopBarIconButtonItem(
                                                            icon = painterResource(Res.drawable.pen_square),
                                                            contentDescription = "New note",
                                                            onClick = { showAddNotePopup = true },
                                                            tooltip = "New note"
                                                        )
                                                    )
                                                )
                                                EmberrDesktopMenu(
                                                    expanded = showSortMenu,
                                                    onDismissRequest = { showSortMenu = false }) {
                                                    DesktopSortMenu(
                                                        currentSortType = currentSortType,
                                                        currentSortOrder = currentSortOrder,
                                                        onDismiss = { showSortMenu = false },
                                                        onSortChanged = { type, order ->
                                                            homeViewModel.updateSort(
                                                                type,
                                                                order
                                                            ); showSortMenu = false
                                                        })
                                                }
                                                DesktopNewNoteMenu(
                                                    expanded = showAddNotePopup,
                                                    onDismissRequest = { showAddNotePopup = false },
                                                    onCreateNote = handleCreateNote,
                                                    onTemplatesOpened = { homeViewModel.onTemplatesMenuOpened() },
                                                    templatesMenu = { isExpanded, onDismiss ->
                                                        TemplatesDesktopMenu(
                                                            expanded = isExpanded,
                                                            templates = templates,
                                                            searchQuery = templateSearchQuery,
                                                            onSearchQueryChange = { homeViewModel.updateTemplateSearchQuery(it) },
                                                            onDismissRequest = onDismiss,
                                                            onTemplateClick = { id -> onDismiss(); handleTemplateClick(id) },
                                                            onEditTemplate = { id -> onDismiss(); handleEditTemplate(id) },
                                                            onDeleteTemplate = { id -> homeViewModel.deleteTemplate(id) },
                                                            onCreateNewTemplate = { onDismiss(); handleCreateNewTemplate() }
                                                        )
                                                    }
                                                )
                                                EmberrDesktopMenu(
                                                    expanded = showAddFolderPopup,
                                                    onDismissRequest = { showAddFolderPopup = false },
                                                    modifier = Modifier.width(280.dp)
                                                ) {
                                                    Column(
                                                        Modifier.padding(
                                                            horizontal = 16.dp,
                                                            vertical = 12.dp
                                                        )
                                                    ) {
                                                        Text(
                                                            "New Folder",
                                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.padding(bottom = 18.dp)
                                                        )
                                                        EmberrTextField(
                                                            value = addFolderInput,
                                                            onValueChange = { addFolderInput = it },
                                                            placeholder = "e.g. Personal, Work...",
                                                            modifier = Modifier.fillMaxWidth(),
                                                            onSubmit = {
                                                                if (addFolderInput.isNotBlank()) {
                                                                    handleCreateFolder(addFolderInput.trim())
                                                                    showAddFolderPopup = false
                                                                }
                                                            }
                                                        )
                                                        Row(
                                                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                                            horizontalArrangement = Arrangement.spacedBy(
                                                                8.dp
                                                            )
                                                        ) {
                                                            EmberrButtonSecondary(
                                                                text = "Cancel",
                                                                onClick = {
                                                                    showAddFolderPopup = false
                                                                },
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                            EmberrButtonPrimary(
                                                                text = "Create",
                                                                onClick = {
                                                                    if (addFolderInput.isNotBlank()) {
                                                                        handleCreateFolder(
                                                                            addFolderInput.trim()
                                                                        ); showAddFolderPopup = false
                                                                    }
                                                                },
                                                                modifier = Modifier.weight(1f)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }

                                if (isNotesExpanded && treeRows.isEmpty()) {
                                    item(key = "sidebar_empty_notes") {
                                        SidebarEmptyNotesHint(
                                            onCreateNote = { showAddNotePopup = true },
                                            onCreateFolder = { addFolderInput = ""; showAddFolderPopup = true }
                                        )
                                    }
                                }

                                if (isNotesExpanded) {
                                    itemsIndexed(treeRows, key = { _, row -> row.key }) { index, row ->
                                        val rowMenuTarget = menuForRow(row.key)
                                        when (row) {
                                            is HomeItem.Folder -> SidebarFolderRow(
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = TreeRowFadeInSpec,
                                                    fadeOutSpec = TreeRowFadeOutSpec,
                                                    placementSpec = TreeRowPlacementSpec
                                                ),
                                                folder = row.folder,
                                                level = row.level,
                                                guideLines = treeGuideLines.getOrElse(index) { ROOT_TREE_GUIDE_LINES },
                                                isExpanded = expandedFolderIds.contains(row.folder.folderId),
                                                isSelected = selectedFolderIds.contains(row.folder.folderId),
                                                dragState = dragState,
                                                menu = rowMenuTarget.menu,
                                                onClick = { modifiers ->
                                                    handleRowClick(treeRows, row.key, modifiers) {
                                                        homeViewModel.toggleFolderExpansion(row.folder.folderId)
                                                    }
                                                },
                                                onAddNote = { kind -> homeViewModel.createNoteInParent(row.folder.folderId, autoExpand = true, kind = kind) { newId -> openNote(newId) } },
                                                onOpenTemplates = { homeViewModel.onTemplatesMenuOpened() },
                                                templatesMenu = { isExpanded, onDismiss ->
                                                    TemplatesDesktopMenu(
                                                        expanded = isExpanded,
                                                        templates = templates,
                                                        searchQuery = templateSearchQuery,
                                                        onSearchQueryChange = { homeViewModel.updateTemplateSearchQuery(it) },
                                                        onDismissRequest = onDismiss,
                                                        onTemplateClick = { id ->
                                                            onDismiss()
                                                            homeViewModel.createNoteFromTemplate(id, parentFolderId = row.folder.folderId, autoExpand = true) { newId -> openNote(newId) }
                                                        },
                                                        onEditTemplate = { id -> onDismiss(); handleEditTemplate(id) },
                                                        onDeleteTemplate = { id -> homeViewModel.deleteTemplate(id) },
                                                        onCreateNewTemplate = { onDismiss(); handleCreateNewTemplate() }
                                                    )
                                                },
                                                onAddSubfolder = { name -> homeViewModel.createFolderInParent(row.folder.folderId, name = name, autoExpand = true) },
                                                onRename = { newName -> homeViewModel.renameFolder(row.folder.folderId, newName) },
                                                onDelete = {
                                                    if (rowMenuTarget.usesSelection) homeViewModel.deleteSelectedItems()
                                                    else homeViewModel.trashFolder(row.folder.folderId)
                                                }
                                            )
                                            is HomeItem.Note -> SidebarNoteRow(
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = TreeRowFadeInSpec,
                                                    fadeOutSpec = TreeRowFadeOutSpec,
                                                    placementSpec = TreeRowPlacementSpec
                                                ),
                                                note = row.note,
                                                level = row.level,
                                                guideLines = treeGuideLines.getOrElse(index) { ROOT_TREE_GUIDE_LINES },
                                                isActive = splitState.isShowing(DetailPane.Note(row.note.noteId)),
                                                isSelected = selectedNoteIds.contains(row.note.noteId),
                                                dragState = dragState,
                                                menu = rowMenuTarget.menu,
                                                onClick = { modifiers ->
                                                    handleRowClick(treeRows, row.key, modifiers) { openNote(row.note.noteId) }
                                                },
                                                onOpenInSplitView = openInSplitViewActionFor(DetailPane.Note(row.note.noteId), rowMenuTarget.usesSelection),
                                                onToggleFavorite = {
                                                    homeViewModel.setNotesFavorite(rowMenuTarget.noteIds, rowMenuTarget.menu.makeFavorite)
                                                },
                                                onRename = { newTitle -> homeViewModel.renameNote(row.note.noteId, newTitle) },
                                                onDelete = {
                                                    if (rowMenuTarget.usesSelection) homeViewModel.deleteSelectedItems()
                                                    else homeViewModel.trashNote(row.note.noteId)
                                                }
                                            )
                                        }
                                    }
                                }

                                if (recentNotes.isNotEmpty()) {
                                    item(key = "sb_recents_header") {
                                        SidebarSectionHeader(
                                            "Recents",
                                            isRecentsExpanded,
                                            { isRecentsExpanded = !isRecentsExpanded },
                                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null, placementSpec = TreeRowPlacementSpec)
                                        )
                                    }
                                    if (isRecentsExpanded) {
                                        items(recentNotes, key = { "sb_recent_${it.noteId}" }) { note ->
                                            SidebarNoteRow(
                                                modifier = Modifier.animateItem(
                                                    fadeInSpec = TreeRowFadeInSpec,
                                                    fadeOutSpec = TreeRowFadeOutSpec,
                                                    placementSpec = TreeRowPlacementSpec
                                                ),
                                                note = note, level = 0,
                                                isActive = splitState.isShowing(DetailPane.Note(note.noteId)),
                                                isSelected = false,
                                                dragState = dragState,
                                                onClick = { openNote(note.noteId) },
                                                onOpenInSplitView = openInSplitViewActionFor(DetailPane.Note(note.noteId)),
                                                onRename = { newTitle -> homeViewModel.renameNote(note.noteId, newTitle) },
                                                onDelete = { homeViewModel.trashNote(note.noteId) },
                                                rowKey = "sb_recent_${note.noteId}"
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        val hasScrolledList by remember { derivedStateOf { sidebarListState.canScrollBackward } }
                        val topEdgeAlpha by animateFloatAsState(
                            if (hasScrolledList) 0.09f else 0f,
                            tween(180, easing = FastOutSlowInEasing),
                            label = "sidebar_top_edge"
                        )
                        HorizontalDivider(
                            modifier = Modifier.align(Alignment.TopCenter),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = topEdgeAlpha)
                        )

                        EmberrVerticalScrollbar(
                            listState = sidebarListState,
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp)
                        )
                    }
                }

                DesktopSpaceBar(
                    spaces = spaces,
                    activeSpaceId = activeSpaceId,
                    onOpenSpace = { spaceId -> spaceViewModel.openSpace(spaceId) },
                    onCreateSpace = { name -> spaceViewModel.createSpaceAndOpenIt(name) },
                    onReorderSpaces = { orderedIds -> spaceViewModel.reorderSpaces(orderedIds) }
                )
            }

            // floating AI assistant button, mirrors mobile's EmberrBottomBar circles
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = endPadding + 16.dp, bottom = DESKTOP_SPACE_BAR_HEIGHT + 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isAiDisabled) {
                    TopBarIconButton(
                        icon = painterResource(Res.drawable.astroid),
                        contentDescription = "Ask AI",
                        bgColor = MaterialTheme.colorScheme.background,
                        tint = MaterialTheme.colorScheme.primary,
                        tooltip = "Ask AI",
                        tooltipPosition = IconTooltipPosition.Above,
                        onClick = onAiIconTap
                    )
                }
            }
        }
    }

    // RIGHT PANEL
    val detailPaneContent = @Composable { d: DetailPane? ->
        when (d) {
            null -> Box(Modifier.fillMaxSize())
            DetailPane.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Select a note from the sidebar",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
            is DetailPane.Daily -> Box(Modifier.fillMaxSize()) {
                DailyEditorPane(
                    viewModel = dailyViewModel,
                    hazeState = hazeState,
                    isSidebarVisible = isSidebarVisible,
                    onPickImage = onPickImage,
                    onTakePhoto = onTakePhoto,
                    onPickDocument = onPickDocument,
                    onOpenFile = onOpenFile,
                    onNavigateToEditor = openSubNote,
                    onExportMarkdown = onExportMarkdown,
                    onExportPdf = onExportPdf,
                    onSelectionModeChange = onSelectionModeChange
                )
            }
            is DetailPane.Note -> key(d.noteId) {
                val noteKind by produceState<NoteKind?>(initialValue = null) {
                    value = noteRepository.getNoteById(d.noteId)?.kind ?: NoteKind.NOTE
                }
                when (noteKind) {
                    NoteKind.CANVAS -> CanvasScreen(
                        noteId = d.noteId,
                        showBackButton = isSidebarVisible,
                        onNavigateBack = closeDetailPane
                    )
                    NoteKind.NOTE, NoteKind.DATABASE -> NoteScreen(
                        noteId = d.noteId,
                        onNavigateBack = closeDetailPane,
                        showBackButton = isSidebarVisible,
                        onSelectionModeChange = onSelectionModeChange,
                        onPickImage = onPickImage, onTakePhoto = onTakePhoto, onPickDocument = onPickDocument,
                        onOpenFile = onOpenFile, onExportMarkdown = onExportMarkdown, onExportPdf = onExportPdf,
                        onNavigateToEditor = openSubNote,
                        desktopTopMargin = 0.dp,
                        hideWordCount = isUpdatePeekShowing
                    )
                    null -> Box(Modifier.fillMaxSize())
                }
            }
            DetailPane.Settings,
            DetailPane.SelfHostSetup,
            DetailPane.EmberrCloud,
            DetailPane.About,
            DetailPane.ImportNotes,
            DetailPane.OpenSourceLicenses -> key("settings") {
                Box(
                    Modifier
                        .fillMaxSize()
                        .onPointerEvent(PointerEventType.Press) { event ->
                            if (event.buttons.isBackPressed) goBackFromSettingsSubPage()
                        }
                ) {
                    SettingsScreen(
                        onNavigateBack = closeDetailPane,
                        onExportReady = onExportBackup,
                        onNavigateToSelfHostSetup = { detail = DetailPane.SelfHostSetup },
                        onNavigateToEmberrCloud = { if (isDebugBuild) detail = DetailPane.EmberrCloud },
                        onNavigateToAbout = { detail = DetailPane.About },
                        onNavigateToImportNotes = { detail = DetailPane.ImportNotes },
                        desktopSubPage = d.settingsSubPage(),
                        onDesktopSubPageChange = { page -> detail = page.toDetailPane() },
                        showBackButton = isSidebarVisible,
                        syncViewModel = syncViewModel
                    )
                }
            }
            DetailPane.Trash -> key("trash") {
                Box(Modifier.fillMaxSize()) {
                    TrashScreen(onNavigateBack = closeDetailPane)
                }
            }
            DetailPane.Properties -> key("properties") {
                Box(Modifier.fillMaxSize()) {
                    PropertiesScreen(onNavigateBack = closeDetailPane)
                }
            }
            DetailPane.Reminders -> key("reminders") {
                Box(Modifier.fillMaxSize()) {
                    TasksScreen(onNavigateBack = closeDetailPane, onOpenFile = onOpenFile, onNavigateToEditor = { openNote(it) })
                }
            }
            DetailPane.Images -> key("images") {
                Box(Modifier.fillMaxSize()) {
                    ImagesScreen(onNavigateBack = closeDetailPane, onTriggerImagePicker = { onPickImage { } })
                }
            }
            DetailPane.Documents -> key("documents") {
                Box(Modifier.fillMaxSize()) {
                    DocumentsScreen(onNavigateBack = closeDetailPane, onTriggerDocumentPicker = { onPickDocument { } }, onOpenFile = onOpenFile)
                }
            }
            DetailPane.Bookmarks -> key("bookmarks") {
                Box(Modifier.fillMaxSize()) {
                    BookmarksScreen(onNavigateBack = closeDetailPane)
                }
            }
            DetailPane.Calendar -> key("calendar") {
                Box(Modifier.fillMaxSize()) {
                    CalendarScreen(
                        onNavigateBack = closeDetailPane,
                        showBackButton = isSidebarVisible,
                        onOpenSourceNote = { event -> ReminderClickBus.requestOpen(event.blockId) }
                    )
                }
            }
        }
    }

    val rightPanel = @Composable {
        Box(Modifier.fillMaxSize()) {
            DesktopSplitLayout(
                splitState = splitState,
                hazeState = hazeState,
                dropTarget = splitDropTarget,
                onDividerDragEnd = saveOpenedState,
                paneContent = detailPaneContent
            )
        }
    }

    CompositionLocalProvider(LocalEmberrBlurSource provides hazeState) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background, contentWindowInsets = WindowInsets(0)) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .consumeWindowInsets(paddingValues)
                    .onGloballyPositioned { coordinates -> screenOriginInWindow = coordinates.positionInWindow() }
            ) {
                Row(modifier = Modifier.fillMaxSize()) {

                    AnimatedVisibility(
                        visible = isSidebarVisible,
                        enter = expandHorizontally(expandFrom = Alignment.Start, animationSpec = tween(280, easing = FastOutSlowInEasing)),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.Start, animationSpec = tween(280, easing = FastOutSlowInEasing))
                    ) {
                        Box(
                            modifier = Modifier
                                .width(panelWidth)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            leftPanel(5.dp, 5.dp)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        if (!isSidebarVisible) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 26.dp, top = PANEL_TOP_MARGIN + 4.dp)
                                    .zIndex(10f)
                            ) {
                                TopBarIconButton(
                                    icon = painterResource(Res.drawable.sidebar),
                                    contentDescription = "Expand sidebar",
                                    bgColor = Color.Transparent,
                                    tint = MaterialTheme.colorScheme.primary,
                                    hazeState = hazeState,
                                    hazeStyle = EmberrBlur.Regular,
                                    tooltip = "Show sidebar",
                                    onClick = onToggleSidebar
                                )
                            }
                        }
                        var canvasFullScreenContent by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
                        CompositionLocalProvider(LocalCanvasFullScreenOverlay provides { content -> canvasFullScreenContent = content }) {
                            rightPanel()
                        }
                        canvasFullScreenContent?.let { content ->
                            Box(Modifier.zIndex(11f)) { content() }
                        }
                        EmberrUpdatePeek(
                            isDue = isUpdatePeekDue,
                            canShow = true,
                            hazeState = hazeState,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 32.dp),
                            onPeekingChange = { isPeeking -> isUpdatePeekShowing = isPeeking }
                        )
                    }

                    val isRagPanelVisible = isAiChatVisible && ragViewModel != null

                    AnimatedVisibility(
                        visible = isRagPanelVisible,
                        enter = fadeIn(tween(280)),
                        exit = fadeOut(tween(280))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(PANEL_PADDING)
                                .background(Color.Transparent)
                                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { change, dragAmount ->
                                        change.consume()
                                        val deltaDp = with(density) { dragAmount.toDp() }
                                        ragPanelWidth = (ragPanelWidth - deltaDp).coerceIn(MIN_RAG_PANEL_WIDTH, MAX_RAG_PANEL_WIDTH)
                                    }
                                }
                        )
                    }

                    AnimatedVisibility(
                        visible = isRagPanelVisible,
                        enter = expandHorizontally(expandFrom = Alignment.End, animationSpec = tween(280, easing = FastOutSlowInEasing)),
                        exit = shrinkHorizontally(shrinkTowards = Alignment.End, animationSpec = tween(280, easing = FastOutSlowInEasing))
                    ) {
                        Box(
                            modifier = Modifier
                                .width(ragPanelWidth)
                                .fillMaxHeight()
                                .emberrBorder(RectangleShape)
                        ) {
                            if (ragViewModel != null) {
                                com.emberr.presentation.ai.AiChatPanel(
                                    onDismiss = onDismissAiChat,
                                    viewModel = ragViewModel,
                                    modifier = Modifier.fillMaxSize(),
                                    onPickDocument = onPickDocument
                                )
                            }
                        }
                    }
                }

                if (isSidebarVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = panelWidth - PANEL_PADDING / 2)
                            .fillMaxHeight()
                            .width(PANEL_PADDING)
                            .zIndex(22f)
                            .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                            .pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        settingsManager.saveDesktopSidebarWidth(panelWidth.value)
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    val deltaDp = with(density) { dragAmount.toDp() }
                                    panelWidth = (panelWidth + deltaDp).coerceIn(MIN_PANEL_WIDTH, MAX_PANEL_WIDTH)
                                }
                            }
                    )
                }

                // Hover-to-peek: thin left-edge trigger (only when collapsed)
                if (!isSidebarVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .fillMaxHeight()
                            .width(16.dp)
                            .zIndex(20f)
                            .onPointerEvent(PointerEventType.Enter) { isPeeking = true }
                    )
                }

                // Hover-to-peek: invisible boundary to detect when cursor leaves the panel area
                if (!isSidebarVisible && isPeeking) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = panelWidth + PANEL_PADDING)
                            .zIndex(24f)
                            .onPointerEvent(PointerEventType.Enter) { isPeeking = false }
                    )
                }

                // Hover-to-peek: floating sidebar overlay
                AnimatedVisibility(
                    visible = !isSidebarVisible && isPeeking,
                    enter = slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { -it },
                    exit = slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { -it },
                    modifier = Modifier.align(Alignment.TopStart).zIndex(25f)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(start = PANEL_PADDING, top = PANEL_TOP_MARGIN, bottom = PANEL_TOP_MARGIN)
                            .width(panelWidth)
                            .fillMaxHeight()
                            .clip(DesktopPanelShape)
                            .emberrBlur(
                                hazeState,
                                EmberrBlur.Thick.copy(
                                    backgroundColor = MaterialTheme.colorScheme.surface,
                                    tints = listOf(HazeTint(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))),
                                    fallbackTint = HazeTint(MaterialTheme.colorScheme.surface)
                                )
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { }
                    ) {
                        leftPanel(5.dp, 5.dp)
                    }
                }

                DesktopListDragChip(
                    dragState = dragState,
                    containerOriginInWindow = screenOriginInWindow,
                    labelForPayload = { payload ->
                        when {
                            payload.startsWith(DRAG_PREFIX_NOTE) -> {
                                val id = payload.removePrefix(DRAG_PREFIX_NOTE)
                                (notesByFolder.values.flatten() + favoriteNotes + recentNotes + searchResults.map { it.note })
                                    .find { it.noteId == id }?.title?.ifEmpty { "Untitled" } ?: "Note"
                            }
                            payload.startsWith(DRAG_PREFIX_FOLDER) -> {
                                val id = payload.removePrefix(DRAG_PREFIX_FOLDER)
                                foldersByParent.values.flatten().find { it.folderId == id }?.name ?: "Folder"
                            }
                            else -> ""
                        }
                    }
                )

                // Sheets
                if (showTimelineDialog) {
                    // Mirrors DailyEditorPane's own EditorActions (same viewModel, same picker
                    // callbacks) so the timeline dialog's real block views - table edits,
                    // audio playback, file opening, linked-note lookups - work against the same
                    // daily editor state. Note-link navigation closes the dialog first since it's
                    // opening a different note, not just jumping within this same day's timeline.
                    val timelineEditorActions = remember(dailyViewModel, onOpenFile) {
                        object : EditorActions {
                            override fun onClearSlashQuery() = dailyViewModel.clearActiveSlashQuery()
                            override fun onClearFocusRequest() = dailyViewModel.clearFocusRequest()
                            override fun onUpdateText(id: String, text: String) = dailyViewModel.updateBlockText(id, text)
                            override fun onToggleCheckbox(id: String, checked: Boolean) = dailyViewModel.toggleCheckbox(id, checked)
                            override fun onToggleExpand(id: String) = dailyViewModel.toggleFoldedState(id)
                            override fun onFocusBlock(id: String) = dailyViewModel.setFocusedBlock(id)
                            override fun onChangeBlockType(type: String) = dailyViewModel.changeFocusedBlockType(type)
                            override fun onToggleFormat(format: String) = dailyViewModel.toggleFormat(format)
                            override fun onAdjustIndentation(increase: Boolean) = dailyViewModel.adjustIndentation(increase)
                            override fun onSetBlockAlignment(alignment: TextAlignment) = dailyViewModel.setFocusedBlockAlignment(alignment)
                            override fun onEnterPressed(id: String, before: String, after: String) = dailyViewModel.handleEnter(id, before, after)
                            override fun onShiftEnterPressed(id: String, before: String, after: String) = dailyViewModel.handleShiftEnter(id, before, after)
                            override fun onBackspaceOnEmpty(id: String) = dailyViewModel.handleBackspaceOnEmpty(id)
                            override fun onBackspaceAtStartOfText(id: String) = dailyViewModel.handleBackspaceAtStartOfText(id)
                            override fun onDeleteAtEndOfText(id: String) = dailyViewModel.handleDeleteAtEndOfText(id)
                            override fun onMarkdownShortcut(blockId: String, literalText: String, shortcut: MarkdownShortcut) =
                                dailyViewModel.applyMarkdownShortcut(blockId, literalText, shortcut)
                            override fun onToggleSelection(id: String) = dailyViewModel.toggleSelection(id)
                            override fun onUpdateReminder(id: String, timestamp: Long?, allDay: AllDaySpan?) = dailyViewModel.updateReminder(id, timestamp, allDay)
                            override fun onUrlSubmit(id: String, url: String) = dailyViewModel.handleUrlSubmit(id, url)
                            override fun onImagePicked(id: String, uri: String) = dailyViewModel.handleImagePicked(id, uri)
                            override fun onDocumentPicked(id: String, uri: String) = dailyViewModel.handleDocumentPicked(id, uri)
                            override fun onAddBlankBlock() = dailyViewModel.addBlankBlockBelowFocused()
                            override fun onInsertMediaBlock(type: String) = dailyViewModel.insertNewMediaBlock(type)
                            override fun onOutsideTap() {}
                            override fun onVoiceRecorded(id: String, filePath: String, duration: Int) = dailyViewModel.handleVoiceRecorded(id, filePath, duration)
                            override fun onRemoveVoice(id: String) = dailyViewModel.handleRemoveVoice(id)
                            override fun onDeleteBlock(id: String) = dailyViewModel.deleteBlock(id)
                            override fun onImageLinkSubmitted(blockId: String, address: String) = dailyViewModel.handleImageLinkSubmitted(blockId, address)
                            override val imageLinkFetches get() = dailyViewModel.imageLinkFetches
                            override fun onRequestImagePicker(blockId: String) {
                                onPickImage { path -> dailyViewModel.handleImagePicked(blockId, path) }
                            }
                            override fun onRequestCamera(blockId: String) {
                                onTakePhoto { path -> dailyViewModel.handleImagePicked(blockId, path) }
                            }
                            override fun onRequestDocumentPicker(blockId: String) {
                                onPickDocument { path -> dailyViewModel.handleDocumentPicked(blockId, path) }
                            }
                            override fun onOpenFile(filePath: String, mimeType: String) {
                                onOpenFile(filePath, mimeType)
                            }
                            override fun onStartRecording() = dailyViewModel.startHardwareRecording()
                            override fun onStopRecording(blockId: String, cancel: Boolean) = dailyViewModel.stopHardwareRecording(blockId, cancel)
                            override fun onPlayAudio(filePath: String, onComplete: () -> Unit) = dailyViewModel.playAudio(filePath, onComplete)
                            override fun onStopAudio() = dailyViewModel.stopAudio()
                            override fun onTogglePin() = dailyViewModel.togglePinSelectedBlocks()
                            override fun onUpdateTable(id: String, rows: List<List<String>>) = dailyViewModel.updateTable(id, rows)
                            override fun onUpdateTableStyle(
                                id: String,
                                cellStyles: Map<String, com.emberr.domain.model.TableCellStyle>,
                                rowStyles: Map<String, com.emberr.domain.model.TableCellStyle>,
                                columnStyles: Map<String, com.emberr.domain.model.TableCellStyle>
                            ) = dailyViewModel.updateTableStyle(id, cellStyles, rowStyles, columnStyles)
                            override fun onAddBlockAbove(id: String) = dailyViewModel.addBlockAbove(id)
                            override fun onAddBlockBelow(id: String) = dailyViewModel.addBlockBelow(id)
                            override fun onNoteLinkClick(noteId: String) {
                                showTimelineDialog = false
                                dailyViewModel.clearTimeline()
                                openNote(noteId)
                            }
                            override fun onCreateLinkedNote(title: String): String = dailyViewModel.createLinkedNote(title)
                            override suspend fun getNoteMetadata(noteId: String) = dailyViewModel.getNoteMetadata(noteId)
                            override fun onUpdateLinkedNoteOptions(id: String, showIcon: Boolean, showCoverImage: Boolean) =
                                dailyViewModel.updateLinkedNoteOptions(id, showIcon, showCoverImage)
                            override fun onUpdateCalloutStyle(id: String, calloutType: CalloutType, isFoldable: Boolean) =
                                dailyViewModel.updateCalloutStyle(id, calloutType, isFoldable)
                            override fun onUpdatePropertyText(id: String, text: String) = dailyViewModel.updatePropertyText(id, text)
                            override fun onUpdatePropertyDate(id: String, range: PropertyDateRange) = dailyViewModel.updatePropertyDate(id, range)
                            override fun onUpdatePropertyTags(id: String, tags: List<String>) = dailyViewModel.updatePropertyTags(id, tags)
                            override fun onUpdatePropertyChecked(id: String, isChecked: Boolean) = dailyViewModel.updatePropertyChecked(id, isChecked)
                            override val databaseBlockEditor: DatabaseBlockEditor get() = dailyViewModel.databaseBlockEditor
                        }
                    }

                    DailyTimelineDialog(
                        days = timelineDays,
                        isLoading = isTimelineLoading,
                        anchorDate = selectedDate,
                        today = today,
                        editorActions = timelineEditorActions,
                        onDismiss = {
                            showTimelineDialog = false
                            dailyViewModel.clearTimeline()
                        },
                        onBlockClick = { date, blockId ->
                            showTimelineDialog = false
                            dailyViewModel.clearTimeline()
                            openDaily(date)
                            dailyViewModel.openTimelineBlock(date, blockId)
                        }
                    )
                }

                SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 66.dp)) { data ->
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shadowElevation = EmberrShadowElevation.Standard,
                        modifier = Modifier.padding(horizontal = 24.dp).wrapContentWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sync",
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = data.visuals.message,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class TreeRowMenuTarget(
    val noteIds: Set<String>,
    val folderIds: Set<String>,
    val usesSelection: Boolean,
    val menu: TreeSelectionMenu
)
