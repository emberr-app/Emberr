package com.emberr.presentation.shared.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.domain.people.PeopleRepository
import com.emberr.domain.people.personDisplayName
import com.emberr.domain.util.system.showNativeToast
import com.emberr.domain.util.system.isDesktopPlatform
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.check
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.x
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

private val PeoplePickerDesktopWidth = 280.dp
private val PeoplePickerRowShape = RoundedCornerShape(10.dp)
private val PeoplePickerRowSidePadding = if (isDesktopPlatform) 8.dp else 0.dp
private val PeoplePickerAvatarSize = 24.dp
private val PersonPillAvatarSize = 20.dp

data class PersonOption(
    val id: String,
    val name: String,
    val icon: String? = null,
    val photoPath: String? = null
)

fun PersonOption.displayName(): String = personDisplayName(name)

private val LocalPeopleToPick = staticCompositionLocalOf<State<List<PersonOption>?>?> { null }

@Composable
private fun collectPeopleToPick(): State<List<PersonOption>?> {
    val peopleRepository = koinInject<PeopleRepository>()
    return remember(peopleRepository) {
        peopleRepository.observePeople().map { personNotes ->
            personNotes.map { person -> PersonOption(id = person.noteId, name = person.title, icon = person.icon, photoPath = person.coverImagePath) }
        }
    }.collectAsState(initial = null)
}

@Composable
fun ProvidePeopleToPick(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPeopleToPick provides collectPeopleToPick(), content = content)
}

@Composable
fun rememberPeopleToPick(): List<PersonOption>? {
    val providedPeople = LocalPeopleToPick.current
    if (providedPeople != null) return providedPeople.value
    return collectPeopleToPick().value
}

@Composable
fun rememberPersonCreator(): (name: String, onCreated: (personNoteId: String) -> Unit) -> Unit {
    val peopleRepository = koinInject<PeopleRepository>()
    val coroutineScope = rememberCoroutineScope()
    return remember(peopleRepository, coroutineScope) {
        { name, onCreated ->
            coroutineScope.launch {
                try {
                    onCreated(peopleRepository.createPerson(name))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (e: Exception) {
                    e.printStackTrace()
                    showNativeToast("Couldn't create the person")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeoplePicker(
    personNoteIds: List<String>,
    people: List<PersonOption>,
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
                    modifier = Modifier.padding(horizontal = PeoplePickerRowSidePadding, vertical = 10.dp)
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
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp)) {
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
private fun PeoplePickerRow(person: PersonOption, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isDesktopPlatform) Modifier.heightIn(min = 48.dp) else Modifier)
            .clip(PeoplePickerRowShape)
            .clickable(onClick = onClick)
            .padding(horizontal = PeoplePickerRowSidePadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PersonAvatar(personNoteId = person.id, name = person.name, icon = person.icon, photoPath = person.photoPath, size = PeoplePickerAvatarSize)
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
            .padding(horizontal = PeoplePickerRowSidePadding, vertical = 8.dp),
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

@Composable
fun PersonPill(person: PersonOption, onOpen: () -> Unit, onRemove: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .emberrBorder(CircleShape)
            .clickable(onClick = onOpen)
            .padding(start = 3.dp, top = 3.dp, bottom = 3.dp, end = if (onRemove != null) 3.dp else 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PersonAvatar(personNoteId = person.id, name = person.name, icon = person.icon, photoPath = person.photoPath, size = PersonPillAvatarSize)
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
