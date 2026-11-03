package com.emberr.presentation.people

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.people.personDisplayName
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.EmberrSearchTextField
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.PersonAvatar
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.topHeaderBarPadding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.address_card
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.search
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

private val ListSidePadding = if (isDesktopPlatform) 32.dp else 16.dp
private val HeaderSidePadding = if (isDesktopPlatform) 22.dp else 16.dp
private val TopEdgeFadeInDistance = 48.dp
private val SearchBarSpace = 96.dp
private val SpaceBelowHeader = 12.dp
private val DesktopListMaxWidth = 760.dp
private val RowAvatarSize = 44.dp
private val RowDividerStart = if (isDesktopPlatform) 72.dp else 70.dp
private val EmptyArtAvatarSize = 48.dp
private val EmptyArtPlusSize = 56.dp
private val DashLength = 6.dp
private val DashGap = 5.dp

private data class OpenedPerson(
    val personNoteId: String,
    val isNew: Boolean,
    val focusesName: Boolean
)

@Composable
fun PeopleScreen(
    onNavigateBack: () -> Unit,
    viewModel: PeopleViewModel = koinViewModel()
) {
    val rows by viewModel.rows.collectAsState()
    var searchText by rememberSaveable { mutableStateOf("") }
    var openedPerson by remember { mutableStateOf<OpenedPerson?>(null) }
    val query = searchText.trim()
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    val createPerson: (String) -> Unit = { name ->
        viewModel.createPerson(name) { personNoteId ->
            searchText = ""
            openedPerson = OpenedPerson(personNoteId = personNoteId, isNew = true, focusesName = name.isBlank())
        }
    }
    val openPerson: (String) -> Unit = { personNoteId ->
        openedPerson = OpenedPerson(personNoteId = personNoteId, isNew = false, focusesName = false)
    }
    val pickPhoneContacts = rememberPhoneContactPicker { contacts ->
        viewModel.importContacts(contacts) { personNoteId ->
            searchText = ""
            openPerson(personNoteId)
        }
    }

    val density = LocalDensity.current
    var topAreaHeightPx by remember { mutableFloatStateOf(0f) }
    val topAreaHeight = with(density) { topAreaHeightPx.toDp() }
    val hazeState = remember { HazeState() }
    val listState = rememberLazyListState()
    val topEdgeFadeInPx = with(density) { TopEdgeFadeInDistance.toPx() }
    val topEdgeFadeAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / topEdgeFadeInPx).coerceIn(0f, 1f)
        }
    }
    val focusManager = LocalFocusManager.current
    val backgroundColor = if (isDesktopPlatform) Color.Transparent else MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize()) {
        val allRows = rows
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .background(backgroundColor)
        ) {
            when {
                allRows == null -> Unit
                allRows.isEmpty() -> PeopleEmptyState(
                    onNewPerson = { createPerson("") },
                    onPickPhoneContacts = pickPhoneContacts,
                    modifier = Modifier.padding(top = topAreaHeight)
                )
                else -> {
                    val matchingRows = if (query.isEmpty()) allRows else allRows.filter { row -> row.person.title.contains(query, ignoreCase = true) }
                    if (matchingRows.isEmpty()) {
                        PeopleNoResults(
                            query = query,
                            onCreate = { createPerson(query) },
                            modifier = Modifier.padding(top = topAreaHeight)
                        )
                    } else {
                        PeopleList(
                            rows = matchingRows,
                            today = today,
                            listState = listState,
                            topPadding = topAreaHeight,
                            onOpen = openPerson,
                            onMoveToTrash = viewModel::moveToTrash
                        )
                    }
                }
            }
        }

        val peopleCount = allRows?.size ?: 0
        val peopleCountBadge: @Composable () -> Unit = {
            Text(
                text = peopleCountLabel(peopleCount),
                style = MaterialTheme.typography.labelSmall,
                color = mutedTextColor(),
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(10f)
                .onGloballyPositioned { coordinates -> topAreaHeightPx = coordinates.size.height.toFloat() }
        ) {
            EmberrTopHeaderBar(
                title = "People",
                titleTrailingIcon = if (isDesktopPlatform && peopleCount > 0) peopleCountBadge else null,
                topEdgeFadeAlpha = topEdgeFadeAlpha,
                hazeState = hazeState,
                hazeStyle = EmberrBlur.Regular,
                contentPadding = topHeaderBarPadding(horizontal = HeaderSidePadding),
                backTooltip = "Back",
                onBackClick = onNavigateBack,
                actions = {
                    if (isDesktopPlatform) {
                        EmberrButtonPrimary(text = "New person", onClick = { createPerson("") })
                    } else {
                        if (pickPhoneContacts != null) {
                            TopBarIconButton(
                                icon = painterResource(Res.drawable.address_card),
                                contentDescription = "Add from contacts",
                                bgColor = Color.Transparent,
                                tint = MaterialTheme.colorScheme.primary,
                                hazeState = hazeState,
                                hazeStyle = EmberrBlur.Regular,
                                tooltip = "Add from contacts",
                                onClick = pickPhoneContacts
                            )
                        }
                        TopBarIconButton(
                            icon = painterResource(Res.drawable.plus),
                            contentDescription = "New person",
                            bgColor = Color.Transparent,
                            tint = MaterialTheme.colorScheme.primary,
                            hazeState = hazeState,
                            hazeStyle = EmberrBlur.Regular,
                            tooltip = "New person",
                            onClick = { createPerson("") }
                        )
                    }
                }
            )
        }

        if (!allRows.isNullOrEmpty()) {
            EmberrSearchTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = "Search people",
                hazeState = hazeState,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onSubmit = { focusManager.clearFocus() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .navigationBarsPadding()
                    .widthIn(max = DesktopListMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = ListSidePadding, vertical = 14.dp)
            )
        }
    }

    openedPerson?.let { opened ->
        PersonView(
            personNoteId = opened.personNoteId,
            focusNameOnOpen = opened.focusesName,
            onDismiss = { notesEditor ->
                openedPerson = null
                if (opened.isNew) viewModel.deletePersonIfLeftEmpty(opened.personNoteId, notesEditor)
            }
        )
    }
}

@Composable
private fun PeopleList(
    rows: List<PersonListRow>,
    today: LocalDate,
    listState: LazyListState,
    topPadding: Dp,
    onOpen: (String) -> Unit,
    onMoveToTrash: (NoteMetadataEntity) -> Unit
) {
    val groups = remember(rows) { rows.groupBy { row -> personGroupLetter(row.person.title) }.toList() }
    val bottomPadding = SearchBarSpace + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(top = topPadding + SpaceBelowHeader, bottom = bottomPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        items(groups, key = { (letter, _) -> letter }) { (letter, groupRows) ->
            Column(
                modifier = Modifier
                    .widthIn(max = DesktopListMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = ListSidePadding)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = mutedTextColor(),
                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(PersonCardShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    groupRows.forEachIndexed { index, row ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = RowDividerStart),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                        PersonListRowItem(
                            row = row,
                            today = today,
                            onOpen = { onOpen(row.person.noteId) },
                            onMoveToTrash = { onMoveToTrash(row.person) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PersonListRowItem(
    row: PersonListRow,
    today: LocalDate,
    onOpen: () -> Unit,
    onMoveToTrash: () -> Unit
) {
    var showsMenu by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(DpOffset.Zero) }
    val gestures = if (isDesktopPlatform) {
        Modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                        val clickPosition = event.changes.first().position
                        menuPosition = DpOffset(clickPosition.x.toDp(), clickPosition.y.toDp())
                        event.changes.forEach { change -> change.consume() }
                        showsMenu = true
                    }
                }
            }
            .clickable(onClick = onOpen)
    } else {
        Modifier.combinedClickable(onClick = onOpen, onLongClick = { showsMenu = true })
    }
    val name = row.person.title.trim()
    val nextDate = row.nextEvent?.let(::eventDate)

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(gestures)
                .padding(horizontal = if (isDesktopPlatform) 16.dp else 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PersonAvatar(personNoteId = row.person.noteId, name = name, icon = row.person.icon, photoPath = row.person.coverImagePath, size = RowAvatarSize)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = personDisplayName(name),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (name.isEmpty()) mutedTextColor() else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val nextEvent = row.nextEvent
                if (nextEvent != null && nextDate != null) {
                    val onSurface = MaterialTheme.colorScheme.onSurface
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = onSurface)) { append("Next") }
                            append(" · ${weekdayMonthDay(nextDate, today)} · ${nextEvent.text.ifBlank { "Untitled event" }}")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryTextColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = "No upcoming events",
                        style = MaterialTheme.typography.labelSmall,
                        color = mutedTextColor(),
                        maxLines = 1
                    )
                }
            }
            if (row.eventCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = eventCountLabel(row.eventCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = mutedTextColor()
                )
            }
        }

        if (isDesktopPlatform) {
            Box(modifier = Modifier.offset(x = menuPosition.x, y = menuPosition.y)) {
                PersonActionsMenu(
                    personName = personDisplayName(name),
                    expanded = showsMenu,
                    onDismiss = { showsMenu = false },
                    onOpen = onOpen,
                    onMoveToTrash = onMoveToTrash
                )
            }
        } else {
            PersonActionsMenu(
                personName = personDisplayName(name),
                expanded = showsMenu,
                onDismiss = { showsMenu = false },
                onOpen = onOpen,
                onMoveToTrash = onMoveToTrash
            )
        }
    }
}

@Composable
private fun PeopleEmptyState(onNewPerson: () -> Unit, onPickPhoneContacts: (() -> Unit)?, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        EmptyStateArt()
        Spacer(modifier = Modifier.height(22.dp))
        Text(
            text = "No people yet",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Add someone you want to remember things about. People you add to an event show up here too.",
            style = MaterialTheme.typography.bodyLarge,
            color = secondaryTextColor(),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        Spacer(modifier = Modifier.height(22.dp))
        EmberrButtonPrimary(text = "New person", onClick = onNewPerson, modifier = Modifier.widthIn(min = 160.dp))
        if (onPickPhoneContacts != null) {
            Spacer(modifier = Modifier.height(10.dp))
            EmberrButtonSecondary(text = "Add from contacts", onClick = onPickPhoneContacts, modifier = Modifier.widthIn(min = 160.dp))
        }
    }
}

@Composable
private fun EmptyStateArt() {
    val pageColor = MaterialTheme.colorScheme.background
    val dashColor = mutedTextColor()
    Box(modifier = Modifier.width(150.dp).height(80.dp)) {
        PersonAvatar(
            personNoteId = "people-empty-state-left",
            name = "A R",
            icon = null,
            size = EmptyArtAvatarSize,
            modifier = Modifier.align(Alignment.CenterStart).offset(x = 6.dp)
        )
        PersonAvatar(
            personNoteId = "people-empty-state-right",
            name = "S K",
            icon = null,
            size = EmptyArtAvatarSize,
            modifier = Modifier.align(Alignment.CenterEnd).offset(x = (-6).dp)
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(EmptyArtPlusSize)
                .clip(CircleShape)
                .background(pageColor)
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    drawRoundRect(
                        color = dashColor,
                        cornerRadius = CornerRadius(size.width / 2),
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(DashLength.toPx(), DashGap.toPx()))
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.plus),
                contentDescription = null,
                tint = secondaryTextColor(),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PeopleNoResults(query: String, onCreate: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 70.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(Res.drawable.search),
            contentDescription = null,
            tint = secondaryTextColor(),
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "No one matches “$query”",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        EmberrButtonSecondary(text = "Create “$query”", onClick = onCreate)
    }
}
