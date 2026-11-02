package com.emberr.presentation.calendar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.emberr.domain.model.RecurrenceEditScope
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

// Reusable host for opening the same EventEditorSheet used by CalendarScreen from anywhere a
// checkbox's reminder can be tapped (Daily/Note screens' three-dot menu) - avoids re-wiring the
// sheet's state + recurrence scope chooser in every screen that needs it.
@Composable
fun EventEditorSheetHost(
    targetBlockId: String?,
    targetOccurrenceDate: String? = null,
    onDismissTarget: () -> Unit,
    newEventPersonNoteId: String? = null,
    calendarViewModel: CalendarViewModel = koinViewModel()
) {
    var eventEditorState by remember { mutableStateOf<EventEditorState?>(null) }
    var pendingScopeAction by remember { mutableStateOf<((RecurrenceEditScope) -> Unit)?>(null) }

    // Collected continuously (not a one-shot LaunchedEffect + Flow.first()) so there's no window
    // where a recomposition can observe targetBlockId having changed before the lookup's first
    // emission has landed - collectAsState just always reflects whatever the flow has emitted so
    // far, recomputing from scratch whenever the key changes.
    val resolvedEvent: CalendarEvent? by remember(targetBlockId, targetOccurrenceDate) {
        if (targetBlockId == null) flowOf(null) else calendarViewModel.eventForBlock(targetBlockId, targetOccurrenceDate)
    }.collectAsState(initial = null)

    LaunchedEffect(targetBlockId, resolvedEvent?.blockId) {
        if (targetBlockId == null) {
            eventEditorState = null
            return@LaunchedEffect
        }
        // Only (re)initialize the editable snapshot when a genuinely different event just
        // resolved - once the sheet is open for a given block, further emissions from the same
        // underlying flow (e.g. a background sync) must not clobber in-progress local edits.
        if (eventEditorState?.original?.blockId == targetBlockId) return@LaunchedEffect
        val event = resolvedEvent ?: return@LaunchedEffect
        val dt = Instant.fromEpochMilliseconds(event.reminderTimestamp).toLocalDateTime(TimeZone.currentSystemDefault())
        eventEditorState = EventEditorState(
            original = event,
            name = event.text,
            date = dt.date,
            hour = dt.hour,
            minute = dt.minute,
            categoryId = event.categoryId,
            durationMinutes = event.durationMinutes,
            url = event.url.orEmpty(),
            description = event.description.orEmpty(),
            recurrenceRule = event.recurrenceRule,
            allDay = event.allDay,
            personNoteIds = event.personNoteIds
        )
    }

    LaunchedEffect(newEventPersonNoteId) {
        val personNoteId = newEventPersonNoteId ?: return@LaunchedEffect
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        eventEditorState = EventEditorState(
            original = null,
            name = "",
            date = now.date,
            hour = now.hour,
            minute = 0,
            categoryId = null,
            personNoteIds = listOf(personNoteId)
        )
    }

    val dismiss: () -> Unit = {
        eventEditorState = null
        onDismissTarget()
    }

    SavingEventEditorSheet(
        state = eventEditorState,
        onStateChange = { newState -> eventEditorState = newState },
        onChooseScope = { runWithScope -> pendingScopeAction = runWithScope },
        onDismiss = dismiss,
        calendarViewModel = calendarViewModel
    )

    pendingScopeAction?.let { runWithScope ->
        RecurrenceScopeChooser(
            onDismiss = { pendingScopeAction = null },
            onScopeSelected = { scope ->
                pendingScopeAction = null
                runWithScope(scope)
            }
        )
    }
}

@Composable
fun SavingEventEditorSheet(
    state: EventEditorState?,
    onStateChange: (EventEditorState?) -> Unit,
    onChooseScope: (runWithScope: (RecurrenceEditScope) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    calendarViewModel: CalendarViewModel,
    sourceNoteTitle: String? = null,
    onOpenSourceNote: (() -> Unit)? = null
) {
    val categories by calendarViewModel.categories.collectAsState()
    val people by calendarViewModel.people.collectAsState()
    val latestState by rememberUpdatedState(state)
    val latestOnStateChange by rememberUpdatedState(onStateChange)
    val changeState: (EventEditorState.() -> EventEditorState) -> Unit = { update ->
        latestOnStateChange(latestState?.update())
    }

    EventEditorSheet(
        state = state,
        categories = categories,
        people = people,
        onNameChange = { name -> changeState { copy(name = name) } },
        onDateChange = { date -> changeState { copy(date = date) } },
        onTimeChange = { hour, minute -> changeState { copy(hour = hour, minute = minute) } },
        onDurationChange = { minutes -> changeState { copy(durationMinutes = minutes) } },
        onCategoryChange = { categoryId -> changeState { copy(categoryId = categoryId) } },
        onUrlChange = { url -> changeState { copy(url = url) } },
        onDescriptionChange = { description -> changeState { copy(description = description) } },
        onRecurrenceChange = { rule -> changeState { copy(recurrenceRule = rule) } },
        onAllDayChange = { allDay -> changeState { copy(allDay = allDay) } },
        onPeopleChange = { personNoteIds -> changeState { copy(personNoteIds = personNoteIds) } },
        onCreatePerson = { name ->
            calendarViewModel.createPerson(name) { personNoteId ->
                changeState { copy(personNoteIds = personNoteIds + personNoteId) }
            }
        },
        onEditClick = {
            if (state?.original?.recurrenceRule != null) {
                onChooseScope { scope -> changeState { copy(isEditing = true, editScope = scope) } }
            } else {
                changeState { copy(isEditing = true) }
            }
        },
        onCancelEdit = { onStateChange(state?.original?.toEditorState()) },
        onSave = {
            state?.let { savedState ->
                calendarViewModel.saveEvent(
                    original = savedState.original,
                    dateString = savedState.date.toString(),
                    timestamp = savedState.toEpochMillis(),
                    name = savedState.name,
                    categoryId = savedState.categoryId,
                    durationMinutes = savedState.durationMinutes,
                    url = savedState.url,
                    description = savedState.description,
                    allDay = savedState.allDay,
                    personNoteIds = savedState.personNoteIds,
                    recurrenceRule = savedState.recurrenceRule,
                    editScope = savedState.editScope
                )
            }
            val viewState = state?.viewAfterSaving()
            if (viewState != null) onStateChange(viewState) else onDismiss()
        },
        onDelete = state?.original?.let { original ->
            {
                if (original.recurrenceRule != null) {
                    onChooseScope { scope ->
                        calendarViewModel.deleteEvent(original, scope)
                        onDismiss()
                    }
                } else {
                    calendarViewModel.deleteEvent(original)
                    onDismiss()
                }
            }
        },
        onDismiss = onDismiss,
        sourceNoteTitle = sourceNoteTitle,
        onOpenSourceNote = onOpenSourceNote
    )
}
