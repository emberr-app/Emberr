package com.emberr.presentation.calendar

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.MorePeopleAvatar
import com.emberr.presentation.shared.components.NoRippleIndicationNodeFactory
import com.emberr.presentation.shared.components.PersonAvatar
import com.emberr.presentation.shared.components.emberrBorder
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.check
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.x
import org.jetbrains.compose.resources.painterResource

private val PeoplePickerDesktopWidth = 280.dp
private val PeoplePickerRowShape = RoundedCornerShape(10.dp)
private val PeoplePickerAvatarSize = 24.dp
private val EventPersonPillAvatarSize = 20.dp
internal val EventChipAvatarSize = 16.dp
private val EventChipAvatarOverlap = 5.dp
private val EventChipAvatarRingWidth = 1.5.dp
private const val MaxAvatarsOnEventChip = 3

internal fun EventPerson.displayName(): String = name.trim().ifEmpty { "Unnamed" }

internal fun peopleShownOnEvent(personNoteIds: List<String>, peopleById: Map<String, EventPerson>): List<EventPerson> =
    personNoteIds.mapNotNull { personNoteId -> peopleById[personNoteId] }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PeoplePicker(
    personNoteIds: List<String>,
    people: List<EventPerson>,
    onPeopleChange: (List<String>) -> Unit,
    onCreatePerson: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val query = searchText.trim()
    val matchingPeople = people.filter { person -> query.isEmpty() || person.name.contains(query, ignoreCase = true) }
    val canCreatePerson = query.isNotEmpty() && people.none { person -> person.name.trim().equals(query, ignoreCase = true) }

    fun togglePerson(personNoteId: String) {
        onPeopleChange(if (personNoteId in personNoteIds) personNoteIds - personNoteId else personNoteIds + personNoteId)
        searchText = ""
    }

    fun createPersonFromSearch() {
        onCreatePerson(query)
        searchText = ""
    }

    fun submitSearch() {
        when {
            canCreatePerson -> createPersonFromSearch()
            matchingPeople.isNotEmpty() -> togglePerson(matchingPeople.first().id)
        }
    }

    val pickerContent = @Composable { searchFieldModifier: Modifier ->
        Column(modifier = Modifier.fillMaxWidth()) {
            EmberrTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = "Search or add a person",
                modifier = searchFieldModifier.fillMaxWidth(),
                onSubmit = { submitSearch() }
            )
            Spacer(modifier = Modifier.height(8.dp))

            matchingPeople.forEach { person ->
                PeoplePickerRow(
                    person = person,
                    isSelected = person.id in personNoteIds,
                    onClick = { togglePerson(person.id) }
                )
            }

            if (canCreatePerson) {
                CreatePersonRow(name = query, onClick = { createPersonFromSearch() })
            }

            if (matchingPeople.isEmpty() && !canCreatePerson) {
                Text(
                    text = "Type a name to add a person",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                )
            }
        }
    }

    if (isDesktopPlatform) {
        EmberrDesktopMenu(
            expanded = true,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(PeoplePickerDesktopWidth)
        ) {
            val searchFocusRequester = remember { FocusRequester() }
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                pickerContent(Modifier.focusRequester(searchFocusRequester))
            }
            LaunchedEffect(Unit) {
                runCatching { searchFocusRequester.requestFocus() }
            }
        }
        return
    }

    EmberrBottomSheet(expanded = true, onDismiss = onDismiss, title = "Add people") { closeAnd ->
        CompositionLocalProvider(
            LocalIndication provides NoRippleIndicationNodeFactory,
            LocalRippleConfiguration provides null
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                pickerContent(Modifier)
                EmberrButtonPrimary(
                    text = "Done",
                    onClick = { closeAnd(onDismiss) },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun PeoplePickerRow(person: EventPerson, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isDesktopPlatform) Modifier.heightIn(min = 48.dp) else Modifier)
            .clip(PeoplePickerRowShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PersonAvatar(name = person.name, size = PeoplePickerAvatarSize)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = person.displayName(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Icon(
                painter = painterResource(Res.drawable.check),
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp).size(16.dp)
            )
        }
    }
}

@Composable
private fun CreatePersonRow(name: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isDesktopPlatform) Modifier.heightIn(min = 48.dp) else Modifier)
            .clip(PeoplePickerRowShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(PeoplePickerAvatarSize), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(Res.drawable.plus),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Create “$name”",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EventPeopleRow(people: List<EventPerson>, onRemovePerson: ((personNoteId: String) -> Unit)?) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "With",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 2.dp)
        )
        people.forEach { person ->
            EventPersonPill(
                person = person,
                onRemove = onRemovePerson?.let { removePerson -> { removePerson(person.id) } }
            )
        }
    }
}

@Composable
private fun EventPersonPill(person: EventPerson, onRemove: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .emberrBorder(CircleShape)
            .padding(start = 3.dp, top = 3.dp, bottom = 3.dp, end = if (onRemove != null) 3.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PersonAvatar(name = person.name, size = EventPersonPillAvatarSize)
        Text(
            text = person.displayName(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (onRemove != null) {
            IconTooltip(text = "Remove") {
                Icon(
                    painter = painterResource(Res.drawable.x),
                    contentDescription = "Remove ${person.displayName()}",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onRemove)
                        .padding(3.dp)
                        .size(14.dp)
                )
            }
        }
    }
}

@Composable
internal fun EventChipAvatars(people: List<EventPerson>, ringColor: Color) {
    val ringModifier = Modifier.border(EventChipAvatarRingWidth, ringColor, CircleShape)
    Row(horizontalArrangement = Arrangement.spacedBy(-EventChipAvatarOverlap)) {
        people.take(MaxAvatarsOnEventChip).forEach { person ->
            PersonAvatar(name = person.name, size = EventChipAvatarSize, modifier = ringModifier)
        }
        if (people.size > MaxAvatarsOnEventChip) {
            MorePeopleAvatar(hiddenCount = people.size - MaxAvatarsOnEventChip, size = EventChipAvatarSize, modifier = ringModifier)
        }
    }
}
