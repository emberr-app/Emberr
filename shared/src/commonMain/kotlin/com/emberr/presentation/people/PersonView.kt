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
import androidx.compose.runtime.withFrameNanos
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
import com.emberr.domain.people.PersonWithDetails
import com.emberr.domain.util.system.isDesktopPlatform
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
import emberr.shared.generated.resources.image
import emberr.shared.generated.resources.square_arrow_out_up_right
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.x
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Clock
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.util.network.openLinkInRunningBrowser
import com.emberr.domain.util.system.showNativeToast
import com.emberr.domain.people.personDisplayName
import com.emberr.presentation.LocalDocumentPicker
import com.emberr.presentation.LocalFileOpener
import com.emberr.presentation.LocalNoteOpener
import com.emberr.presentation.LocalPhotoTaker
import com.emberr.presentation.home.note.CategorizedEmojiPicker
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.editor.hidesEditorToolbarWhileFocused
import androidx.compose.foundation.layout.navigationBarsPadding
import emberr.shared.generated.resources.calendar_add
import emberr.shared.generated.resources.ghost_smile
import emberr.shared.generated.resources.mail
import emberr.shared.generated.resources.message
import emberr.shared.generated.resources.phone
import emberr.shared.generated.resources.user2

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
private val AvatarBadgeSize = 28.dp
private val AndroidEmojiPickerHeight = 450.dp
private val QuickActionMinWidth = 58.dp
private val QuickActionTileSize = 48.dp
private val QuickActionShape = RoundedCornerShape(12.dp)
private val QuickActionIconSize = 20.dp
private val DesktopDialogMaxWidth = 720.dp
private val DesktopDialogShape = RoundedCornerShape(24.dp)
private val DesktopSidePadding = 28.dp
private val DesktopDetailsColumnWidth = 280.dp
private val EditorBlockSidePadding = if (isDesktopPlatform) 36.dp else 16.dp
private val SectionGap = 24.dp

@Composable
fun PersonView(
    personNoteId: String,
    onDismiss: (notesEditor: NoteEditorViewModel) -> Unit,
    focusNameOnOpen: Boolean = false
) {
    val notesEditor: NoteEditorViewModel = koinViewModel(key = personNoteId)
    val personViewModel: PersonViewModel = koinViewModel(key = "person-$personNoteId") { parametersOf(personNoteId) }

    val personWithDetails by personViewModel.person.collectAsState()
    val eventsWithPerson by personViewModel.events.collectAsState()
    val everyone by personViewModel.everyone.collectAsState()
    val categories by personViewModel.categories.collectAsState()
    val savedName by notesEditor.noteTitle.collectAsState()
    val photoPath by notesEditor.coverImagePath.collectAsState()
    val pickImage = LocalImagePicker.current

    var nameDraft by remember(personNoteId) { mutableStateOf<String?>(null) }
    var showsAllPast by remember(personNoteId) { mutableStateOf(false) }
    var openEvent by remember(personNoteId) { mutableStateOf<CalendarTaskEntity?>(null) }
    var hasFocusedNameOnOpen by remember(personNoteId) { mutableStateOf(!focusNameOnOpen) }
    var detailPopup by remember(personNoteId) { mutableStateOf<PersonDetailPopup?>(null) }
    var showsIconPicker by remember(personNoteId) { mutableStateOf(false) }
    var isAddingEvent by remember(personNoteId) { mutableStateOf(false) }
    val noteOpener = LocalNoteOpener.current
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current

    val commitName: () -> Unit = {
        val draft = nameDraft
        if (draft != null) {
            if (draft.trim() != savedName) notesEditor.updateTitle(draft.trim())
            nameDraft = null
        }
    }
    val dismiss: () -> Unit = {
        commitName()
        onDismiss(notesEditor)
    }
    val moveToTrash: () -> Unit = {
        commitName()
        notesEditor.moveToTrash { onDismiss(notesEditor) }
    }

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val details = personWithDetails?.details.orEmpty()
    val detailEditing = PersonDetailEditing(
        popup = detailPopup,
        personName = savedName,
        hasBirthday = details.any { it.kind == PersonDetailKind.BIRTHDAY },
        today = today,
        onPopupChange = { detailPopup = it },
        onBack = {
            val shownPopup = detailPopup
            detailPopup = if (shownPopup is PersonDetailPopup.Editing && shownPopup.isFromPicker) {
                PersonDetailPopup.ChoosingKind(shownPopup.anchor)
            } else {
                null
            }
        },
        onSave = personViewModel::saveDetail,
        onRemove = personViewModel::removeDetail
    )
    val detailActions = remember(uriHandler, clipboardManager) {
        PersonDetailActions(
            openLink = { link -> openPersonLink(link, uriHandler) },
            copy = { text ->
                try {
                    clipboardManager.setText(AnnotatedString(text))
                    showNativeToast("Copied")
                } catch (e: Exception) {
                    e.printStackTrace()
                    showNativeToast("Couldn't copy")
                }
            }
        )
    }
    val page = PersonPage(
        personNoteId = personNoteId,
        icon = personWithDetails?.person?.icon,
        photoPath = photoPath,
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
        onMoveToTrash = moveToTrash,
        detailEditing = detailEditing,
        detailActions = detailActions,
        showsIconPicker = showsIconPicker,
        onShowsIconPickerChange = { showsIconPicker = it },
        onIconChange = { icon ->
            notesEditor.updateIcon(icon)
            if (photoPath != null) notesEditor.removeCoverImage()
        },
        onChangePhoto = { pickImage { path -> notesEditor.handleCoverImagePicked(path) } },
        onRemovePhoto = if (photoPath != null) notesEditor::removeCoverImage else null,
        onNewEvent = { isAddingEvent = true },
        onOpenNote = noteOpener?.let { openNote ->
            { noteId ->
                dismiss()
                openNote(noteId)
            }
        }
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
                    PersonMoreButton(page = page, hazeState = hazeState)
                    PersonTopBarButton(icon = Res.drawable.x, description = "Close", hazeState = hazeState, onClick = { closeAnd {} })
                }
            }
        }
    }

    PersonDetailSheet(editing = detailEditing)
    AndroidPersonIconSheet(page = page)

    EventEditorSheetHost(
        targetBlockId = openEvent?.blockId,
        targetOccurrenceDate = openEvent?.takeIf { it.recurrenceFrequency != null }?.targetDate,
        onDismissTarget = {
            openEvent = null
            isAddingEvent = false
        },
        newEventPersonNoteId = personNoteId.takeIf { isAddingEvent }
    )
}

private fun openPersonLink(link: String, uriHandler: UriHandler) {
    if (openLinkInRunningBrowser(link)) return
    try {
        uriHandler.openUri(link)
    } catch (e: Exception) {
        e.printStackTrace()
        showNativeToast("Couldn't open the link")
    }
}

private class PersonPage(
    val personNoteId: String,
    val icon: String?,
    val photoPath: String?,
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
    val onMoveToTrash: () -> Unit,
    val detailEditing: PersonDetailEditing,
    val detailActions: PersonDetailActions,
    val showsIconPicker: Boolean,
    val onShowsIconPickerChange: (Boolean) -> Unit,
    val onIconChange: (String?) -> Unit,
    val onChangePhoto: () -> Unit,
    val onRemovePhoto: (() -> Unit)?,
    val onNewEvent: () -> Unit,
    val onOpenNote: ((String) -> Unit)?
) {
    val firstName: String get() = personFirstNameOrThem(savedName)
    val metaLine: String get() = personMetaLine(events?.upcoming?.firstOrNull(), events?.past?.firstOrNull(), today)
    val details: List<PersonDetailBlock> get() = personWithDetails?.details.orEmpty()
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
            onTakePhoto = LocalPhotoTaker.current,
            onPickDocument = LocalDocumentPicker.current,
            onOpenFile = LocalFileOpener.current,
            onNavigateToEditor = page.onOpenNote ?: {},
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
                PersonQuickActions(page = page, modifier = Modifier.padding(top = 20.dp))
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = EditorBlockSidePadding, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(SectionGap)
        ) {
            PersonDetailsSection(details = page.details, today = page.today, editing = page.detailEditing, actions = page.detailActions)
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
                    PersonQuickActions(page = page, modifier = Modifier)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PersonTopBarButton(icon = Res.drawable.chevron_left, description = "Back", hazeState = hazeState, onClick = onBack)
                PersonMoreButton(page = page, hazeState = hazeState)
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
                PersonDetailsSection(details = page.details, today = page.today, editing = page.detailEditing, actions = page.detailActions)
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
    Box {
        Box(
            modifier = Modifier
                .shadow(AvatarShadowElevation, CircleShape, ambientColor = glow.avatarShadow, spotColor = glow.avatarShadow)
                .background(personPageColor(), CircleShape)
                .padding(AvatarRingWidth)
                .clip(CircleShape)
                .clickable { page.onShowsIconPickerChange(true) }
        ) {
            PersonAvatar(personNoteId = page.personNoteId, name = page.savedName, icon = page.icon, photoPath = page.photoPath, size = size)
        }
        Box(modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 6.dp)) {
            IconTooltip(text = "Change icon") {
                Box(
                    modifier = Modifier
                        .size(AvatarBadgeSize)
                        .clip(CircleShape)
                        .background(personCardColor())
                        .emberrBorder(CircleShape)
                        .clickable { page.onShowsIconPickerChange(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ghost_smile),
                        contentDescription = "Change icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        if (isDesktopPlatform) {
            EmberrDesktopMenu(expanded = page.showsIconPicker, onDismissRequest = { page.onShowsIconPickerChange(false) }) {
                Box(modifier = Modifier.size(width = 340.dp, height = 380.dp)) {
                    CategorizedEmojiPicker(
                        onEmojiSelected = { emoji ->
                            page.onIconChange(emoji)
                            page.onShowsIconPickerChange(false)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                EmberrDesktopMenuOption(
                    label = "Use initials",
                    onClick = {
                        page.onIconChange(null)
                        page.onShowsIconPickerChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun AndroidPersonIconSheet(page: PersonPage) {
    if (isDesktopPlatform) return
    EmberrBottomSheet(
        expanded = page.showsIconPicker,
        onDismiss = { page.onShowsIconPickerChange(false) },
        title = "Change icon"
    ) { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(AndroidEmojiPickerHeight)) {
                CategorizedEmojiPicker(
                    onEmojiSelected = { emoji -> closeAnd { page.onIconChange(emoji) } },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                EmberrButtonSecondary(
                    text = "Use initials",
                    onClick = { closeAnd { page.onIconChange(null) } },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                EmberrButtonPrimary(
                    text = "Close",
                    onClick = { closeAnd {} },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PersonQuickActions(page: PersonPage, modifier: Modifier) {
    val firstPhone = page.details.firstOrNull { it.kind == PersonDetailKind.PHONE }?.value
    val firstEmail = page.details.firstOrNull { it.kind == PersonDetailKind.EMAIL }?.value
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        if (firstPhone != null && !isDesktopPlatform) {
            QuickActionButton(
                label = "Call",
                icon = Res.drawable.phone,
                onClick = { page.detailActions.openLink(phoneCallLink(firstPhone)) }
            )
            QuickActionButton(
                label = "Message",
                icon = Res.drawable.message,
                onClick = { page.detailActions.openLink(phoneMessageLink(firstPhone)) }
            )
        }
        if (firstEmail != null) {
            QuickActionButton(
                label = "Email",
                icon = Res.drawable.mail,
                onClick = { page.detailActions.openLink(emailLink(firstEmail)) }
            )
        }
        QuickActionButton(
            label = "New event",
            icon = Res.drawable.calendar_add,
            onClick = page.onNewEvent
        )
    }
}

@Composable
private fun QuickActionButton(label: String, icon: DrawableResource, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(min = QuickActionMinWidth)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(QuickActionTileSize)
                .clip(QuickActionShape)
                .background(personCardColor()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(QuickActionIconSize)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false
        )
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
        waitOneFrame()
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
        .hidesEditorToolbarWhileFocused()
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
private fun PersonMoreButton(page: PersonPage, hazeState: HazeState) {
    var showsMenu by remember { mutableStateOf(false) }
    Box {
        PersonTopBarButton(icon = Res.drawable.ellipsis, description = "More", hazeState = hazeState, onClick = { showsMenu = true })
        PersonActionsMenu(
            personName = personDisplayName(page.savedName),
            expanded = showsMenu,
            onDismiss = { showsMenu = false },
            onOpen = null,
            onChangeIcon = { page.onShowsIconPickerChange(true) },
            onChangePhoto = page.onChangePhoto,
            onRemovePhoto = page.onRemovePhoto,
            onMoveToTrash = page.onMoveToTrash
        )
    }
}

@Composable
internal fun PersonActionsMenu(
    personName: String,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onOpen: (() -> Unit)?,
    onMoveToTrash: () -> Unit,
    onChangeIcon: (() -> Unit)? = null,
    onChangePhoto: (() -> Unit)? = null,
    onRemovePhoto: (() -> Unit)? = null
) {
    val trashColor = MaterialTheme.colorScheme.error
    val options = buildList {
        if (onOpen != null) add(PersonMenuOption("Open", Res.drawable.square_arrow_out_up_right, color = null, onClick = onOpen))
        if (onChangeIcon != null) add(PersonMenuOption("Change icon", Res.drawable.ghost_smile, color = null, onClick = onChangeIcon))
        if (onChangePhoto != null) add(PersonMenuOption("Change photo", Res.drawable.image, color = null, onClick = onChangePhoto))
        if (onRemovePhoto != null) add(PersonMenuOption("Remove photo", Res.drawable.user2, color = null, onClick = onRemovePhoto))
        add(PersonMenuOption("Move to Trash", Res.drawable.trash, color = trashColor, onClick = onMoveToTrash))
    }
    if (isDesktopPlatform) {
        EmberrDesktopMenu(expanded = expanded, onDismissRequest = onDismiss) {
            options.forEach { option ->
                EmberrDesktopMenuOption(
                    label = option.label,
                    icon = { MenuIcon(icon = option.icon, tint = option.color ?: MaterialTheme.colorScheme.onSurface) },
                    labelColor = option.color,
                    onClick = {
                        onDismiss()
                        option.onClick()
                    }
                )
            }
        }
    } else {
        EmberrBottomSheet(expanded = expanded, onDismiss = onDismiss, title = personName) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp)) {
                options.forEach { option ->
                    EmberrBottomSheetOption(
                        label = option.label,
                        icon = { MenuIcon(icon = option.icon, tint = option.color ?: MaterialTheme.colorScheme.onSurface) },
                        labelColor = option.color,
                        outerHorizontalPadding = 0.dp,
                        innerHorizontalPadding = 0.dp,
                        onClick = { closeAnd(option.onClick) }
                    )
                }
                EmberrButtonPrimary(
                    text = "Close",
                    onClick = { closeAnd(onDismiss) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        }
    }
}

private class PersonMenuOption(
    val label: String,
    val icon: DrawableResource,
    val color: Color?,
    val onClick: () -> Unit
)

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

private suspend fun waitOneFrame() {
    withFrameNanos { }
}
