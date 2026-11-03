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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.emberr.domain.people.ContactImportProgress
import com.emberr.domain.people.personDisplayName
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrExpandingSearchBar
import com.emberr.presentation.shared.components.EmberrSelectionPill
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.KmpBackHandler
import com.emberr.presentation.shared.components.EmberrSearchTextField
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.PersonAvatar
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.topHeaderBarPadding
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.phone3
import emberr.shared.generated.resources.check
import emberr.shared.generated.resources.file_down
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.search
import emberr.shared.generated.resources.select_all
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

private val ListSidePadding = if (isDesktopPlatform) 32.dp else 16.dp
private val PersonCardCorner = 18.dp
private val HeaderSidePadding = if (isDesktopPlatform) 22.dp else 16.dp
private val TopEdgeFadeInDistance = 48.dp
private val SearchBarSpace = 96.dp
private val SpaceBelowHeader = 12.dp
private val DesktopListMaxWidth = 760.dp
private val RowAvatarSize = 44.dp
private val RowCheckIconSize = 16.dp
private const val SelectedRowTintAlpha = 0.08f
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
    val selectedPersonNoteIds by viewModel.selectedPersonNoteIds.collectAsState()
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
    val openImportedPerson: (String) -> Unit = { personNoteId ->
        searchText = ""
        openPerson(personNoteId)
    }
    val pickPhoneContacts = rememberPhoneContactPicker { contacts -> viewModel.importContacts(contacts) }
    val pickContactFiles = rememberContactFilePicker { fileContents -> viewModel.importContactFiles(fileContents) }
    val contactImportProgress by viewModel.contactImportProgress.collectAsState()
    LaunchedEffect(contactImportProgress) {
        val result = (contactImportProgress as? ContactImportProgress.Finished)?.result ?: return@LaunchedEffect
        val onlyImportedPersonNoteId = result.importedPersonNoteIds.singleOrNull()
        if (onlyImportedPersonNoteId != null && result.copiesSkipped == 0 && result.failures == 0) {
            openImportedPerson(onlyImportedPersonNoteId)
        } else {
            showNativeToast(contactImportMessage(result))
        }
        viewModel.clearFinishedImport()
    }
    var isAddPeopleSheetOpen by remember { mutableStateOf(false) }
    val importContacts = if (isDesktopPlatform) pickContactFiles else ({ isAddPeopleSheetOpen = true })

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
    val allRows = rows
    val matchingRows = when {
        allRows == null -> emptyList()
        query.isEmpty() -> allRows
        else -> allRows.filter { row -> row.person.title.contains(query, ignoreCase = true) }
    }
    val selectedCount = allRows.orEmpty().count { row -> row.person.noteId in selectedPersonNoteIds }
    val isSelecting = selectedCount > 0
    val matchingPersonNoteIds = matchingRows.map { row -> row.person.noteId }.toSet()
    val areAllMatchingSelected = matchingPersonNoteIds.isNotEmpty() && selectedPersonNoteIds.containsAll(matchingPersonNoteIds)
    val toggleSelectAll = {
        viewModel.selectPeople(if (areAllMatchingSelected) emptySet() else selectedPersonNoteIds + matchingPersonNoteIds)
    }

    var isSearchOpen by rememberSaveable { mutableStateOf(false) }
    val closeSearch: () -> Unit = {
        focusManager.clearFocus()
        searchText = ""
        isSearchOpen = false
    }

    KmpBackHandler(enabled = isSelecting) { viewModel.clearSelection() }
    KmpBackHandler(enabled = isSearchOpen && !isSelecting, onBack = closeSearch)

    Box(modifier = Modifier.fillMaxSize()) {
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
                    onImportContacts = importContacts,
                    modifier = Modifier.padding(top = topAreaHeight)
                )
                else -> {
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
                            selectedPersonNoteIds = selectedPersonNoteIds,
                            isSelecting = isSelecting,
                            onOpen = openPerson,
                            onToggleSelection = viewModel::togglePersonSelection,
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
                title = if (isSelecting) "$selectedCount selected" else "People",
                titleTrailingIcon = if (isDesktopPlatform && peopleCount > 0 && !isSelecting) peopleCountBadge else null,
                topEdgeFadeAlpha = topEdgeFadeAlpha,
                hazeState = hazeState,
                hazeStyle = EmberrBlur.Regular,
                contentPadding = topHeaderBarPadding(horizontal = HeaderSidePadding),
                backTooltip = if (isSelecting) "Clear selection" else "Back",
                onBackClick = if (isSelecting) viewModel::clearSelection else onNavigateBack,
                actions = {
                    if (isSelecting) {
                        val selectAllLabel = if (areAllMatchingSelected) "Deselect all" else "Select all"
                        if (isDesktopPlatform) {
                            EmberrButtonSecondary(text = selectAllLabel, onClick = toggleSelectAll)
                        } else {
                            TopBarIconButton(
                                icon = painterResource(Res.drawable.select_all),
                                contentDescription = selectAllLabel,
                                bgColor = Color.Transparent,
                                tint = MaterialTheme.colorScheme.primary,
                                hazeState = hazeState,
                                hazeStyle = EmberrBlur.Regular,
                                tooltip = selectAllLabel,
                                onClick = toggleSelectAll
                            )
                        }
                    } else if (isDesktopPlatform) {
                        TopBarIconButton(
                            icon = painterResource(Res.drawable.file_down),
                            contentDescription = "Import from .vcf file",
                            bgColor = Color.Transparent,
                            tint = MaterialTheme.colorScheme.primary,
                            hazeState = hazeState,
                            hazeStyle = EmberrBlur.Regular,
                            tooltip = "Import from .vcf file",
                            onClick = pickContactFiles
                        )
                        EmberrButtonPrimary(text = "New person", onClick = { createPerson("") })
                    } else {
                        TopBarIconButton(
                            icon = painterResource(Res.drawable.plus),
                            contentDescription = "Add people",
                            bgColor = Color.Transparent,
                            tint = MaterialTheme.colorScheme.primary,
                            hazeState = hazeState,
                            hazeStyle = EmberrBlur.Regular,
                            tooltip = "Add people",
                            onClick = { isAddPeopleSheetOpen = true }
                        )
                    }
                }
            )
        }

        (contactImportProgress as? ContactImportProgress.Importing)?.let { importing ->
            ImportingContactsPill(
                progress = importing,
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.TopCenter).zIndex(11f).padding(top = topAreaHeight + 4.dp)
            )
        }

        if (!allRows.isNullOrEmpty() && !isSelecting) {
            if (isDesktopPlatform) {
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
            } else {
                EmberrExpandingSearchBar(
                    isOpen = isSearchOpen,
                    placeholder = "Search people",
                    query = searchText,
                    onQueryChange = { searchText = it },
                    onOpenClick = { isSearchOpen = true },
                    onCloseClick = closeSearch,
                    hazeState = hazeState,
                    modifier = Modifier.align(Alignment.BottomCenter).zIndex(20f)
                )
            }
        }

        EmberrSelectionPill(
            isVisible = isSelecting,
            selectedCount = selectedCount,
            onClearSelection = viewModel::clearSelection,
            onDelete = viewModel::moveSelectedToTrash,
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    AddPeopleSheet(
        expanded = isAddPeopleSheetOpen,
        onDismiss = { isAddPeopleSheetOpen = false },
        onCreateNew = { createPerson("") },
        onImportFromPhone = pickPhoneContacts,
        onImportFromFile = pickContactFiles
    )

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

private sealed interface PeopleListItem {
    val letter: String
}

private data class LetterHeaderItem(override val letter: String) : PeopleListItem

private data class PersonItem(
    override val letter: String,
    val row: PersonListRow,
    val isFirstInLetter: Boolean,
    val isLastInLetter: Boolean
) : PeopleListItem

private fun peopleListItems(rows: List<PersonListRow>): List<PeopleListItem> =
    rows.groupBy { row -> personGroupLetter(row.person.title) }.flatMap { (letter, letterRows) ->
        listOf(LetterHeaderItem(letter)) + letterRows.mapIndexed { index, row ->
            PersonItem(letter = letter, row = row, isFirstInLetter = index == 0, isLastInLetter = index == letterRows.lastIndex)
        }
    }

@Composable
private fun PeopleList(
    rows: List<PersonListRow>,
    today: LocalDate,
    listState: LazyListState,
    topPadding: Dp,
    selectedPersonNoteIds: Set<String>,
    isSelecting: Boolean,
    onOpen: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onMoveToTrash: (NoteMetadataEntity) -> Unit
) {
    val listItems = remember(rows) { peopleListItems(rows) }
    val itemLetters = remember(listItems) { listItems.map { item -> item.letter } }
    val bottomPadding = SearchBarSpace + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = topPadding + SpaceBelowHeader, bottom = bottomPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = listItems,
                key = { item ->
                    when (item) {
                        is LetterHeaderItem -> "letter-${item.letter}"
                        is PersonItem -> item.row.person.noteId
                    }
                },
                contentType = { item -> item is LetterHeaderItem }
            ) { item ->
                val itemModifier = Modifier
                    .widthIn(max = DesktopListMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = ListSidePadding)
                when (item) {
                    is LetterHeaderItem -> Text(
                        text = item.letter,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = mutedTextColor(),
                        modifier = itemModifier.padding(top = 16.dp).padding(start = 8.dp, bottom = 6.dp)
                    )
                    is PersonItem -> Column(
                        modifier = itemModifier
                            .clip(personRowShape(item))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        if (!item.isFirstInLetter) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = RowDividerStart),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        }
                        PersonListRowItem(
                            row = item.row,
                            today = today,
                            isSelected = item.row.person.noteId in selectedPersonNoteIds,
                            isSelecting = isSelecting,
                            onOpen = { onOpen(item.row.person.noteId) },
                            onToggleSelection = { onToggleSelection(item.row.person.noteId) },
                            onMoveToTrash = { onMoveToTrash(item.row.person) }
                        )
                    }
                }
            }
        }
        PeopleFastScroller(
            listState = listState,
            itemLetters = itemLetters,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = topPadding + SpaceBelowHeader, bottom = bottomPadding)
        )
    }
}

private fun personRowShape(item: PersonItem): RoundedCornerShape {
    val topCorner = if (item.isFirstInLetter) PersonCardCorner else 0.dp
    val bottomCorner = if (item.isLastInLetter) PersonCardCorner else 0.dp
    return RoundedCornerShape(topStart = topCorner, topEnd = topCorner, bottomEnd = bottomCorner, bottomStart = bottomCorner)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PersonListRowItem(
    row: PersonListRow,
    today: LocalDate,
    isSelected: Boolean,
    isSelecting: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
    onMoveToTrash: () -> Unit
) {
    var showsMenu by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(DpOffset.Zero) }
    val gestures = if (isSelecting) {
        Modifier.clickable(onClick = onToggleSelection)
    } else if (isDesktopPlatform) {
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
                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = SelectedRowTintAlpha) else Color.Transparent)
                .then(gestures)
                .padding(horizontal = if (isDesktopPlatform) 16.dp else 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(RowAvatarSize)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleSelection)
            ) {
                if (isSelected) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.check),
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(RowCheckIconSize)
                        )
                    }
                } else {
                    PersonAvatar(personNoteId = row.person.noteId, name = name, icon = row.person.icon, photoPath = row.person.coverImagePath, size = RowAvatarSize)
                }
            }
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
private fun PeopleEmptyState(onNewPerson: () -> Unit, onImportContacts: () -> Unit, modifier: Modifier) {
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
        Spacer(modifier = Modifier.height(10.dp))
        EmberrButtonSecondary(text = "Import contacts", onClick = onImportContacts, modifier = Modifier.widthIn(min = 160.dp))
    }
}

@Composable
private fun ImportingContactsPill(progress: ContactImportProgress.Importing, hazeState: HazeState, modifier: Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .emberrBorder(CircleShape)
            .clip(CircleShape)
            .emberrBlur(hazeState, EmberrBlur.Regular)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
        Text(
            text = importingContactsLabel(progress),
            style = MaterialTheme.typography.labelSmall,
            color = secondaryTextColor()
        )
    }
}

@Composable
private fun AddPeopleSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
    onImportFromPhone: (() -> Unit)?,
    onImportFromFile: () -> Unit
) {
    EmberrBottomSheet(expanded = expanded, onDismiss = onDismiss, title = "Add people") { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp)) {
            AddPeopleOption(label = "Create new contact", icon = Res.drawable.plus, onClick = { closeAnd(onCreateNew) })
            if (onImportFromPhone != null) {
                AddPeopleOption(label = "Import from phone", icon = Res.drawable.phone3, onClick = { closeAnd(onImportFromPhone) })
            }
            AddPeopleOption(label = "Import from .vcf file", icon = Res.drawable.file_down, onClick = { closeAnd(onImportFromFile) })
        }
    }
}

@Composable
private fun AddPeopleOption(label: String, icon: DrawableResource, onClick: () -> Unit) {
    EmberrBottomSheetOption(
        label = label,
        icon = { Icon(painter = painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)) },
        outerHorizontalPadding = 0.dp,
        innerHorizontalPadding = 0.dp,
        onClick = onClick
    )
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
