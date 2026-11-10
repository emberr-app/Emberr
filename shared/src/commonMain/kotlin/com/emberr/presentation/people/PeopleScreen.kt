package com.emberr.presentation.people

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.settings.SettingsDivider
import com.emberr.presentation.settings.settingsRowSize
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrBlur
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.EmberrExpandingSearchBar
import com.emberr.presentation.shared.components.EmberrSelectionPill
import com.emberr.presentation.shared.components.SelectionPillAction
import com.emberr.presentation.shared.components.emberrBlur
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.KmpBackHandler
import com.emberr.presentation.shared.components.EmberrSearchTextField
import com.emberr.presentation.shared.components.EmberrTopHeaderBar
import com.emberr.presentation.shared.components.PersonAvatar
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.topHeaderBarPadding
import com.emberr.presentation.shared.components.highContrastSegmentBorder
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.phone3
import emberr.shared.generated.resources.check
import emberr.shared.generated.resources.file_down
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.search
import emberr.shared.generated.resources.select_all
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.common_back
import emberr.shared.generated.resources.common_close
import emberr.shared.generated.resources.people_add_people
import emberr.shared.generated.resources.people_clear_selection
import emberr.shared.generated.resources.people_create_named
import emberr.shared.generated.resources.people_create_new_contact
import emberr.shared.generated.resources.people_deselect_all
import emberr.shared.generated.resources.people_empty_message
import emberr.shared.generated.resources.people_empty_title
import emberr.shared.generated.resources.people_import_contacts
import emberr.shared.generated.resources.people_import_from_phone
import emberr.shared.generated.resources.people_import_from_vcf_file
import emberr.shared.generated.resources.people_move_to_trash
import emberr.shared.generated.resources.people_new_person
import emberr.shared.generated.resources.people_next
import emberr.shared.generated.resources.people_no_one_matches
import emberr.shared.generated.resources.people_no_upcoming_events
import emberr.shared.generated.resources.people_search_people
import emberr.shared.generated.resources.people_select_all
import emberr.shared.generated.resources.people_selected
import emberr.shared.generated.resources.people_selected_count
import emberr.shared.generated.resources.people_title
import emberr.shared.generated.resources.people_untitled_event
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

private val ListSidePadding = 16.dp
private val PersonCardCorner = 18.dp
private val HeaderSidePadding = if (isDesktopPlatform) 22.dp else 16.dp
private val TopEdgeFadeInDistance = 48.dp
private val SearchBarSpace = 96.dp
private val SpaceBelowHeader = 12.dp
private val DesktopListMaxWidth = 760.dp
private val RowAvatarSize = 44.dp
private val RowCheckIconSize = 16.dp
private const val SelectedRowTintAlpha = 0.08f
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
    CountPeopleScreenAsOpen()
    LaunchedEffect(contactImportProgress) {
        if (contactImportProgress !is ContactImportProgress.Finished) return@LaunchedEffect
        val result = viewModel.takeFinishedImportResult() ?: return@LaunchedEffect
        val onlyImportedPersonNoteId = result.importedPersonNoteIds.singleOrNull()
        if (onlyImportedPersonNoteId != null && result.copiesSkipped == 0 && result.failures == 0) {
            openImportedPerson(onlyImportedPersonNoteId)
        } else {
            showNativeToast(contactImportMessage(result))
        }
    }
    var isAddPeopleSheetOpen by remember { mutableStateOf(false) }
    val showImportOptions = if (isDesktopPlatform) pickContactFiles else ({ isAddPeopleSheetOpen = true })

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
                    onImportContacts = showImportOptions,
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
                            onMoveToTrash = viewModel::moveToTrash,
                            onMoveSelectedToTrash = viewModel::moveSelectedToTrash
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
                title = if (isSelecting) pluralStringResource(Res.plurals.people_selected_count, selectedCount, selectedCount) else stringResource(Res.string.people_title),
                titleTrailingIcon = if (isDesktopPlatform && peopleCount > 0 && !isSelecting) peopleCountBadge else null,
                topEdgeFadeAlpha = topEdgeFadeAlpha,
                hazeState = hazeState,
                hazeStyle = EmberrBlur.Regular,
                contentPadding = topHeaderBarPadding(horizontal = HeaderSidePadding),
                backTooltip = stringResource(if (isSelecting) Res.string.people_clear_selection else Res.string.common_back),
                onBackClick = if (isSelecting) viewModel::clearSelection else onNavigateBack,
                actions = {
                    PeopleHeaderActions(
                        isSelecting = isSelecting,
                        areAllMatchingSelected = areAllMatchingSelected,
                        hazeState = hazeState,
                        onToggleSelectAll = toggleSelectAll,
                        onImportFromFile = pickContactFiles,
                        onNewPerson = { createPerson("") },
                        onAddPeople = { isAddPeopleSheetOpen = true }
                    )
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
            PeopleSearch(
                searchText = searchText,
                onSearchTextChange = { searchText = it },
                isSearchOpen = isSearchOpen,
                onOpenSearch = { isSearchOpen = true },
                onCloseSearch = closeSearch,
                onSubmit = { focusManager.clearFocus() },
                hazeState = hazeState
            )
        }

        EmberrSelectionPill(
            isVisible = isSelecting && !isDesktopPlatform,
            selectedCount = selectedCount,
            onClearSelection = viewModel::clearSelection,
            actions = listOf(SelectionPillAction(Res.drawable.trash, stringResource(Res.string.people_move_to_trash), viewModel::moveSelectedToTrash)),
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

@Composable
private fun PeopleHeaderActions(
    isSelecting: Boolean,
    areAllMatchingSelected: Boolean,
    hazeState: HazeState,
    onToggleSelectAll: () -> Unit,
    onImportFromFile: () -> Unit,
    onNewPerson: () -> Unit,
    onAddPeople: () -> Unit
) {
    if (isSelecting) {
        val selectAllLabel = stringResource(if (areAllMatchingSelected) Res.string.people_deselect_all else Res.string.people_select_all)
        if (isDesktopPlatform) {
            EmberrButtonSecondary(text = selectAllLabel, onClick = onToggleSelectAll)
        } else {
            TopBarIconButton(
                icon = painterResource(Res.drawable.select_all),
                contentDescription = selectAllLabel,
                bgColor = Color.Transparent,
                tint = MaterialTheme.colorScheme.primary,
                hazeState = hazeState,
                hazeStyle = EmberrBlur.Regular,
                tooltip = selectAllLabel,
                onClick = onToggleSelectAll
            )
        }
    } else {
        var isDesktopAddMenuOpen by remember { mutableStateOf(false) }
        val addPeopleLabel = stringResource(Res.string.people_add_people)
        Box {
            TopBarIconButton(
                icon = painterResource(Res.drawable.plus),
                contentDescription = addPeopleLabel,
                bgColor = Color.Transparent,
                tint = MaterialTheme.colorScheme.primary,
                hazeState = hazeState,
                hazeStyle = EmberrBlur.Regular,
                tooltip = addPeopleLabel,
                onClick = { if (isDesktopPlatform) isDesktopAddMenuOpen = true else onAddPeople() }
            )
            if (isDesktopPlatform) {
                AddPeopleDesktopMenu(
                    expanded = isDesktopAddMenuOpen,
                    onDismiss = { isDesktopAddMenuOpen = false },
                    onCreateNew = onNewPerson,
                    onImportFromFile = onImportFromFile
                )
            }
        }
    }
}

@Composable
private fun AddPeopleDesktopMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onCreateNew: () -> Unit,
    onImportFromFile: () -> Unit
) {
    EmberrDesktopMenu(expanded = expanded, onDismissRequest = onDismiss) {
        AddPeopleDesktopMenuOption(label = stringResource(Res.string.people_create_new_contact), icon = Res.drawable.plus, onClick = { onDismiss(); onCreateNew() })
        AddPeopleDesktopMenuOption(label = stringResource(Res.string.people_import_from_vcf_file), icon = Res.drawable.file_down, onClick = { onDismiss(); onImportFromFile() })
    }
}

@Composable
private fun AddPeopleDesktopMenuOption(label: String, icon: DrawableResource, onClick: () -> Unit) {
    EmberrDesktopMenuOption(
        label = label,
        icon = { Icon(painter = painterResource(icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)) },
        onClick = onClick
    )
}

@Composable
private fun BoxScope.PeopleSearch(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    isSearchOpen: Boolean,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onSubmit: () -> Unit,
    hazeState: HazeState
) {
    if (isDesktopPlatform) {
        EmberrSearchTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            placeholder = stringResource(Res.string.people_search_people),
            hazeState = hazeState,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onSubmit = onSubmit,
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
            placeholder = stringResource(Res.string.people_search_people),
            query = searchText,
            onQueryChange = onSearchTextChange,
            onOpenClick = onOpenSearch,
            onCloseClick = onCloseSearch,
            hazeState = hazeState,
            modifier = Modifier.align(Alignment.BottomCenter).zIndex(20f).padding(end = if (isSearchOpen) 0.dp else 4.dp)
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
    onMoveToTrash: (NoteMetadataEntity) -> Unit,
    onMoveSelectedToTrash: () -> Unit
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
                            .highContrastSegmentBorder(
                                cornerRadius = PersonCardCorner,
                                isFirst = item.isFirstInLetter,
                                isLast = item.isLastInLetter
                            )
                    ) {
                        if (!item.isFirstInLetter) {
                            SettingsDivider()
                        }
                        PersonListRowItem(
                            row = item.row,
                            today = today,
                            isSelected = item.row.person.noteId in selectedPersonNoteIds,
                            isSelecting = isSelecting,
                            onOpen = { onOpen(item.row.person.noteId) },
                            onToggleSelection = { onToggleSelection(item.row.person.noteId) },
                            onMoveToTrash = { onMoveToTrash(item.row.person) },
                            onMoveSelectedToTrash = onMoveSelectedToTrash
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

@Composable
private fun PersonListRowItem(
    row: PersonListRow,
    today: LocalDate,
    isSelected: Boolean,
    isSelecting: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
    onMoveToTrash: () -> Unit,
    onMoveSelectedToTrash: () -> Unit
) {
    var showsMenu by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(DpOffset.Zero) }
    val name = row.person.title.trim()
    val shownName = shownPersonName(name)
    val nextDate = row.nextEvent?.let(::eventDate)

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = SelectedRowTintAlpha) else Color.Transparent)
                .personRowGestures(
                    isSelecting = isSelecting,
                    onOpen = onOpen,
                    onToggleSelection = onToggleSelection,
                    onRightClick = { position ->
                        menuPosition = position
                        showsMenu = true
                    },
                    onLongClick = { showsMenu = true }
                )
                .settingsRowSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectableAvatar(person = row.person, name = name, isSelected = isSelected, onToggleSelection = onToggleSelection)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shownName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (name.isEmpty()) mutedTextColor() else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val nextEvent = row.nextEvent
                if (nextEvent != null && nextDate != null) {
                    val onSurface = MaterialTheme.colorScheme.onSurface
                    val nextLabel = stringResource(Res.string.people_next)
                    val eventTitle = nextEvent.text.ifBlank { stringResource(Res.string.people_untitled_event) }
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = onSurface)) { append(nextLabel) }
                            append(" · ${weekdayMonthDay(nextDate, today)} · $eventTitle")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = secondaryTextColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.people_no_upcoming_events),
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
                if (isSelecting) {
                    SelectedPeopleDesktopMenu(
                        expanded = showsMenu,
                        onDismiss = { showsMenu = false },
                        onMoveSelectedToTrash = onMoveSelectedToTrash
                    )
                } else {
                    PersonActionsMenu(
                        personName = shownName,
                        expanded = showsMenu,
                        onDismiss = { showsMenu = false },
                        onOpen = onOpen,
                        onMoveToTrash = onMoveToTrash
                    )
                }
            }
        } else {
            PersonActionsMenu(
                personName = shownName,
                expanded = showsMenu,
                onDismiss = { showsMenu = false },
                onOpen = onOpen,
                onMoveToTrash = onMoveToTrash
            )
        }
    }
}

@Composable
private fun SelectedPeopleDesktopMenu(expanded: Boolean, onDismiss: () -> Unit, onMoveSelectedToTrash: () -> Unit) {
    val trashColor = MaterialTheme.colorScheme.error
    EmberrDesktopMenu(expanded = expanded, onDismissRequest = onDismiss) {
        EmberrDesktopMenuOption(
            label = stringResource(Res.string.people_move_to_trash),
            icon = { Icon(painter = painterResource(Res.drawable.trash), contentDescription = null, tint = trashColor, modifier = Modifier.size(20.dp)) },
            labelColor = trashColor,
            onClick = {
                onDismiss()
                onMoveSelectedToTrash()
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.personRowGestures(
    isSelecting: Boolean,
    onOpen: () -> Unit,
    onToggleSelection: () -> Unit,
    onRightClick: (DpOffset) -> Unit,
    onLongClick: () -> Unit
): Modifier = if (isDesktopPlatform) {
    pointerInput(Unit) {
        awaitEachGesture {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                val clickPosition = event.changes.first().position
                event.changes.forEach { change -> change.consume() }
                onRightClick(DpOffset(clickPosition.x.toDp(), clickPosition.y.toDp()))
            }
        }
    }
        .clickable(onClick = if (isSelecting) onToggleSelection else onOpen)
} else if (isSelecting) {
    clickable(onClick = onToggleSelection)
} else {
    combinedClickable(onClick = onOpen, onLongClick = onLongClick)
}

@Composable
private fun SelectableAvatar(
    person: NoteMetadataEntity,
    name: String,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
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
                    contentDescription = stringResource(Res.string.people_selected),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(RowCheckIconSize)
                )
            }
        } else {
            PersonAvatar(personNoteId = person.noteId, name = name, icon = person.icon, photoPath = person.coverImagePath, size = RowAvatarSize)
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
            text = stringResource(Res.string.people_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.people_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = secondaryTextColor(),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp)
        )
        Spacer(modifier = Modifier.height(22.dp))
        EmberrButtonPrimary(text = stringResource(Res.string.people_new_person), onClick = onNewPerson, modifier = Modifier.widthIn(min = 160.dp))
        Spacer(modifier = Modifier.height(10.dp))
        EmberrButtonSecondary(text = stringResource(Res.string.people_import_contacts), onClick = onImportContacts, modifier = Modifier.widthIn(min = 160.dp))
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
    EmberrBottomSheet(expanded = expanded, onDismiss = onDismiss, title = stringResource(Res.string.people_add_people)) { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp)) {
            AddPeopleOption(label = stringResource(Res.string.people_create_new_contact), icon = Res.drawable.plus, onClick = { closeAnd(onCreateNew) })
            if (onImportFromPhone != null) {
                AddPeopleOption(label = stringResource(Res.string.people_import_from_phone), icon = Res.drawable.phone3, onClick = { closeAnd(onImportFromPhone) })
            }
            AddPeopleOption(label = stringResource(Res.string.people_import_from_vcf_file), icon = Res.drawable.file_down, onClick = { closeAnd(onImportFromFile) })
            EmberrButtonPrimary(
                text = stringResource(Res.string.common_close),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
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
            text = stringResource(Res.string.people_no_one_matches, query),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        EmberrButtonSecondary(text = stringResource(Res.string.people_create_named, query), onClick = onCreate)
    }
}
