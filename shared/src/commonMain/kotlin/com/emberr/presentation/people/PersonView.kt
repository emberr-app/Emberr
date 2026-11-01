package com.emberr.presentation.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.domain.people.EventsWithPerson
import com.emberr.domain.people.PeopleRepository
import com.emberr.domain.people.PersonWithDetails
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.domain.util.system.showFeedback
import com.emberr.presentation.LocalImagePicker
import com.emberr.presentation.calendar.EventEditorSheetHost
import com.emberr.presentation.calendar.toCategoryColor
import com.emberr.presentation.home.note.NoteEditorViewModel
import com.emberr.presentation.home.note.NoteScreen
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.PersonAvatar
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.EmberrBlur
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import com.emberr.presentation.shared.components.fullScreenDialogProperties
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.chevron_left
import emberr.shared.generated.resources.ellipsis
import emberr.shared.generated.resources.square_arrow_out_up_right
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.x
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

private const val DESKTOP_DIALOG_WIDTH_FRACTION = 0.55f
private const val DESKTOP_DIALOG_HEIGHT_FRACTION = 0.92f
private const val ANDROID_NAME_SIZE_SCALE = 1.3f
private const val DESKTOP_NAME_SIZE_SCALE = 1.5f
private const val GLOW_FADE_START = 0.45f

private val TopBarButtonSize = 44.dp
private val TopButtonsPadding = 16.dp
private val AndroidAvatarSize = 96.dp
private val DesktopAvatarSize = 108.dp
private val AvatarRingWidth = 4.dp
private val AvatarShadowElevation = 16.dp
private val DesktopDialogMaxWidth = 720.dp
private val DesktopDialogShape = RoundedCornerShape(24.dp)
private val DesktopSidePadding = 28.dp
private val DesktopDetailsColumnWidth = 280.dp
private val EditorBlockSidePadding = if (isDesktopPlatform) 36.dp else 16.dp
private val SectionGap = 24.dp
private val FocusNameAfterSheetOpensDelay = 350.milliseconds

@Composable
fun PersonView(
    personNoteId: String,
    onDismiss: () -> Unit,
    focusNameOnOpen: Boolean = false
) {
    val notesEditor: NoteEditorViewModel = koinViewModel(key = personNoteId)
    val peopleRepository: PeopleRepository = koinInject()
    val noteRepository: NoteRepository = koinInject()

    val personWithDetails by remember(personNoteId) { peopleRepository.observePerson(personNoteId) }.collectAsState(initial = null)
    val eventsWithPerson by remember(personNoteId) { peopleRepository.observeEventsWithPerson(personNoteId) }.collectAsState(initial = null)
    val everyone by remember { peopleRepository.observePeople() }.collectAsState(initial = emptyList())
    val categories by remember { noteRepository.getAllCategories() }.collectAsState(initial = emptyList())
    val savedName by notesEditor.noteTitle.collectAsState()

    var nameDraft by remember(personNoteId) { mutableStateOf<String?>(null) }
    var showsAllPast by remember(personNoteId) { mutableStateOf(false) }
    var openEvent by remember(personNoteId) { mutableStateOf<CalendarTaskEntity?>(null) }
    var hasFocusedNameOnOpen by remember(personNoteId) { mutableStateOf(!focusNameOnOpen) }

    val commitName: () -> Unit = {
        val draft = nameDraft
        if (draft != null) {
            if (draft.trim() != savedName) notesEditor.updateTitle(draft.trim())
            nameDraft = null
        }
    }
    val dismiss: () -> Unit = {
        commitName()
        onDismiss()
    }
    val moveToTrash: () -> Unit = {
        commitName()
        val shownName = personDisplayName(notesEditor.noteTitle.value)
        notesEditor.moveToTrash {
            showFeedback("Moved $shownName to Trash")
            onDismiss()
        }
    }

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val page = PersonPage(
        personNoteId = personNoteId,
        icon = personWithDetails?.person?.icon,
        shownName = nameDraft ?: savedName,
        savedName = savedName,
        personWithDetails = personWithDetails,
        events = eventsWithPerson,
        today = today,
        eventRowLook = PersonEventRowLook(
            namesByPersonNoteId = everyone.associate { person -> person.noteId to personDisplayName(person.title) },
            categoryColorsById = categories.filterNot { it.isDeleted }.associate { category -> category.categoryId to category.colorHex.toCategoryColor() },
            shownPersonNoteId = personNoteId
        ),
        showsAllPast = showsAllPast,
        onShowsAllPastChange = { showsAllPast = it },
        onNameChange = { nameDraft = it },
        onCommitName = commitName,
        shouldFocusName = !hasFocusedNameOnOpen,
        onNameFocused = { hasFocusedNameOnOpen = true },
        onOpenEvent = { event -> openEvent = event },
        onMoveToTrash = moveToTrash
    )

    if (isDesktopPlatform) {
        Dialog(onDismissRequest = dismiss, properties = fullScreenDialogProperties()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = dismiss),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(DESKTOP_DIALOG_WIDTH_FRACTION)
                        .widthIn(max = DesktopDialogMaxWidth)
                        .fillMaxHeight(DESKTOP_DIALOG_HEIGHT_FRACTION)
                        .safeDrawingPadding()
                        .clip(DesktopDialogShape)
                        .background(personPageColor())
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {})
                ) {
                    DesktopPersonLayout(page = page, notesEditor = notesEditor, onBack = dismiss)
                }
            }
        }
    } else {
        EmberrBottomSheet(
            expanded = true,
            onDismiss = dismiss,
            contentHorizontalPadding = 0.dp,
            scrollsContent = false
        ) { closeAnd ->
            val hazeState = remember { HazeState() }
            Box(modifier = Modifier.fillMaxWidth()) {
                PersonNotes(page = page, notesEditor = notesEditor, hazeState = hazeState) {
                    AndroidPersonHeader(page = page)
                }
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(TopButtonsPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PersonMoreButton(
                        personName = personDisplayName(page.savedName),
                        onMoveToTrash = page.onMoveToTrash,
                        hazeState = hazeState
                    )
                    PersonTopBarButton(icon = Res.drawable.x, description = "Close", hazeState = hazeState, onClick = { closeAnd {} })
                }
            }
        }
    }

    EventEditorSheetHost(
        targetBlockId = openEvent?.blockId,
        targetOccurrenceDate = openEvent?.takeIf { it.recurrenceFrequency != null }?.targetDate,
        onDismissTarget = { openEvent = null }
    )
}

private class PersonPage(
    val personNoteId: String,
    val icon: String?,
    val shownName: String,
    val savedName: String,
    val personWithDetails: PersonWithDetails?,
    val events: EventsWithPerson?,
    val today: LocalDate,
    val eventRowLook: PersonEventRowLook,
    val showsAllPast: Boolean,
    val onShowsAllPastChange: (Boolean) -> Unit,
    val onNameChange: (String) -> Unit,
    val onCommitName: () -> Unit,
    val shouldFocusName: Boolean,
    val onNameFocused: () -> Unit,
    val onOpenEvent: (CalendarTaskEntity) -> Unit,
    val onMoveToTrash: () -> Unit
) {
    val firstName: String get() = personFirstName(savedName)
    val metaLine: String get() = personMetaLine(events?.upcoming?.firstOrNull(), events?.past?.firstOrNull(), today)
}

@Composable
private fun PersonNotes(
    page: PersonPage,
    notesEditor: NoteEditorViewModel,
    hazeState: HazeState? = null,
    header: @Composable () -> Unit
) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = personPageColor())) {
        NoteScreen(
            noteId = page.personNoteId,
            onNavigateBack = {},
            onPickImage = LocalImagePicker.current,
            customHeader = { header() },
            emptyNotePlaceholder = "Write anything about ${page.firstName}…",
            extraBlockSidePadding = SectionTitleInset,
            externalHazeState = hazeState,
            viewModel = notesEditor
        )
    }
}

@Composable
private fun AndroidPersonHeader(page: PersonPage) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().personGlow(page.personNoteId)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(TopButtonsPadding + TopBarButtonSize))
                PersonHeroAvatar(page = page, size = AndroidAvatarSize)
                Spacer(modifier = Modifier.height(14.dp))
                PersonNameField(page = page, isCentered = true, sizeScale = ANDROID_NAME_SIZE_SCALE)
                PersonMetaLine(page = page, modifier = Modifier.padding(horizontal = 24.dp, vertical = 3.dp))
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = EditorBlockSidePadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(SectionGap)
        ) {
            PersonDetailsSection(details = page.personWithDetails?.details.orEmpty(), today = page.today)
            PersonEventSections(page = page)
            PersonSectionHeader(title = "Notes")
        }
    }
}

@Composable
private fun DesktopPersonLayout(page: PersonPage, notesEditor: NoteEditorViewModel, onBack: () -> Unit) {
    val hazeState = remember { HazeState() }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.matchParentSize().hazeSource(hazeState).personGlow(page.personNoteId))
            Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp + TopBarButtonSize)) {
                Row(
                    modifier = Modifier.padding(horizontal = DesktopSidePadding - 20.dp).padding(bottom = 30.dp),
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PersonHeroAvatar(page = page, size = DesktopAvatarSize)
                    Column(modifier = Modifier.weight(1f)) {
                        PersonNameField(page = page, isCentered = false, sizeScale = DESKTOP_NAME_SIZE_SCALE)
                        PersonMetaLine(page = page, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PersonTopBarButton(icon = Res.drawable.chevron_left, description = "Back", hazeState = hazeState, onClick = onBack)
                PersonMoreButton(
                    personName = personDisplayName(page.savedName),
                    onMoveToTrash = page.onMoveToTrash,
                    hazeState = hazeState
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .padding(start = DesktopSidePadding)
                    .width(DesktopDetailsColumnWidth)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 6.dp, bottom = DesktopSidePadding)
            ) {
                PersonDetailsSection(details = page.personWithDetails?.details.orEmpty(), today = page.today)
            }
            Box(modifier = Modifier.weight(1f)) {
                PersonNotes(page = page, notesEditor = notesEditor) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = EditorBlockSidePadding).padding(top = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(SectionGap)
                    ) {
                        PersonEventSections(page = page)
                        PersonSectionHeader(title = "Notes")
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonEventSections(page: PersonPage) {
    PersonUpcomingSection(
        upcoming = page.events?.upcoming.orEmpty(),
        firstName = page.firstName,
        look = page.eventRowLook,
        onOpenEvent = page.onOpenEvent
    )
    PersonPastSection(
        past = page.events?.past.orEmpty(),
        showsAll = page.showsAllPast,
        onShowsAllChange = page.onShowsAllPastChange,
        look = page.eventRowLook,
        onOpenEvent = page.onOpenEvent
    )
}

@Composable
private fun PersonHeroAvatar(page: PersonPage, size: Dp) {
    val glow = personGlowColors(page.personNoteId)
    Box(
        modifier = Modifier
            .shadow(AvatarShadowElevation, CircleShape, ambientColor = glow.avatarShadow, spotColor = glow.avatarShadow)
            .background(personPageColor(), CircleShape)
            .padding(AvatarRingWidth)
    ) {
        PersonAvatar(personNoteId = page.personNoteId, name = page.savedName, icon = page.icon, size = size)
    }
}

@Composable
private fun PersonNameField(page: PersonPage, isCentered: Boolean, sizeScale: Float) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val titleStyle = MaterialTheme.typography.titleLarge
    val nameStyle: TextStyle = titleStyle.copy(
        fontSize = titleStyle.fontSize * sizeScale,
        lineHeight = titleStyle.lineHeight * sizeScale,
        fontWeight = FontWeight.Bold,
        textAlign = if (isCentered) TextAlign.Center else TextAlign.Start
    )
    LaunchedEffect(page.shouldFocusName) {
        if (!page.shouldFocusName) return@LaunchedEffect
        delay(FocusNameAfterSheetOpensDelay)
        runCatching { focusRequester.requestFocus() }
        page.onNameFocused()
    }
    val submitName = {
        page.onCommitName()
        focusManager.clearFocus()
    }
    val fieldModifier = Modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)
        .onFocusChanged { focusState -> if (!focusState.isFocused) page.onCommitName() }
    if (isCentered) {
        val placeholderColor = mutedTextColor()
        BasicTextField(
            value = page.shownName,
            onValueChange = page.onNameChange,
            singleLine = true,
            textStyle = nameStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submitName() }),
            modifier = fieldModifier.padding(horizontal = 24.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (page.shownName.isEmpty()) Text(text = "Name", style = nameStyle, color = placeholderColor)
                    innerTextField()
                }
            }
        )
    } else {
        EmberrTextField(
            value = page.shownName,
            onValueChange = page.onNameChange,
            placeholder = "Name",
            textStyle = nameStyle,
            hasBackground = false,
            onSubmit = submitName,
            modifier = fieldModifier
        )
    }
}

@Composable
private fun PersonMetaLine(page: PersonPage, modifier: Modifier) {
    Text(
        text = page.metaLine,
        style = MaterialTheme.typography.labelSmall,
        color = secondaryTextColor(),
        modifier = modifier
    )
}

@Composable
private fun PersonTopBarButton(icon: DrawableResource, description: String, hazeState: HazeState, onClick: () -> Unit) {
    TopBarIconButton(
        icon = painterResource(icon),
        contentDescription = description,
        bgColor = Color.Transparent,
        tint = MaterialTheme.colorScheme.primary,
        hazeState = hazeState,
        hazeStyle = EmberrBlur.Regular,
        size = TopBarButtonSize,
        tooltip = description,
        onClick = onClick
    )
}

@Composable
private fun PersonMoreButton(personName: String, onMoveToTrash: () -> Unit, hazeState: HazeState) {
    var showsMenu by remember { mutableStateOf(false) }
    Box {
        PersonTopBarButton(icon = Res.drawable.ellipsis, description = "More", hazeState = hazeState, onClick = { showsMenu = true })
        PersonActionsMenu(
            personName = personName,
            expanded = showsMenu,
            onDismiss = { showsMenu = false },
            onOpen = null,
            onMoveToTrash = onMoveToTrash
        )
    }
}

@Composable
internal fun PersonActionsMenu(
    personName: String,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpen: (() -> Unit)?,
    onMoveToTrash: () -> Unit
) {
    val trashColor = MaterialTheme.colorScheme.error
    if (isDesktopPlatform) {
        EmberrDesktopMenu(expanded = expanded, onDismissRequest = onDismiss) {
            if (onOpen != null) {
                EmberrDesktopMenuOption(
                    label = "Open",
                    icon = { MenuIcon(icon = Res.drawable.square_arrow_out_up_right, tint = MaterialTheme.colorScheme.onSurface) },
                    onClick = {
                        onDismiss()
                        onOpen()
                    }
                )
            }
            EmberrDesktopMenuOption(
                label = "Move to Trash",
                icon = { MenuIcon(icon = Res.drawable.trash, tint = trashColor) },
                labelColor = trashColor,
                onClick = {
                    onDismiss()
                    onMoveToTrash()
                }
            )
        }
    } else {
        EmberrBottomSheet(expanded = expanded, onDismiss = onDismiss, title = personName) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                if (onOpen != null) {
                    EmberrBottomSheetOption(
                        label = "Open",
                        icon = { MenuIcon(icon = Res.drawable.square_arrow_out_up_right, tint = MaterialTheme.colorScheme.onSurface) },
                        outerHorizontalPadding = 0.dp,
                        innerHorizontalPadding = 0.dp,
                        onClick = { closeAnd(onOpen) }
                    )
                }
                EmberrBottomSheetOption(
                    label = "Move to Trash",
                    icon = { MenuIcon(icon = Res.drawable.trash, tint = trashColor) },
                    labelColor = trashColor,
                    outerHorizontalPadding = 0.dp,
                    innerHorizontalPadding = 0.dp,
                    onClick = { closeAnd(onMoveToTrash) }
                )
                EmberrButtonPrimary(
                    text = "Close",
                    onClick = { closeAnd(onDismiss) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun MenuIcon(icon: DrawableResource, tint: Color) {
    Icon(painter = painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
}

@Composable
private fun Modifier.personGlow(personNoteId: String): Modifier {
    val colors = personGlowColors(personNoteId)
    val pageColor = personPageColor()
    return drawBehind {
        val longestSide = maxOf(size.width, size.height)
        drawRect(Brush.verticalGradient(listOf(colors.wash, pageColor)))
        drawRect(
            Brush.radialGradient(
                colors = listOf(colors.topLeftSpot, colors.topLeftSpot.copy(alpha = 0f)),
                center = Offset(size.width * 0.18f, size.height * 0.12f),
                radius = longestSide * 0.6f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(colors.topRightSpot, colors.topRightSpot.copy(alpha = 0f)),
                center = Offset(size.width * 0.86f, size.height * 0.18f),
                radius = longestSide * 0.5f
            )
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(colors.lowerSpot, colors.lowerSpot.copy(alpha = 0f)),
                center = Offset(size.width * 0.5f, size.height * 0.7f),
                radius = longestSide * 0.65f
            )
        )
        drawRect(
            Brush.verticalGradient(
                0f to pageColor.copy(alpha = 0f),
                GLOW_FADE_START to pageColor.copy(alpha = 0f),
                1f to pageColor
            )
        )
    }
}
