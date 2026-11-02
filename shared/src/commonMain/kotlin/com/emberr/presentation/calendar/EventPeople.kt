package com.emberr.presentation.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.components.MorePeopleAvatar
import com.emberr.presentation.shared.components.PersonAvatar
import com.emberr.presentation.shared.components.PersonOption
import com.emberr.presentation.shared.components.PersonPill

internal val EventChipAvatarSize = 16.dp
private val EventChipAvatarOverlap = 5.dp
private val EventChipAvatarRingWidth = 1.5.dp
private const val MaxAvatarsOnEventChip = 3

internal fun peopleShownOnEvent(personNoteIds: List<String>, peopleById: Map<String, PersonOption>): List<PersonOption> =
    personNoteIds.mapNotNull { personNoteId -> peopleById[personNoteId] }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EventPeopleRow(
    people: List<PersonOption>,
    onRemovePerson: ((personNoteId: String) -> Unit)?,
    onOpenPerson: (personNoteId: String) -> Unit
) {
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
            PersonPill(
                person = person,
                onOpen = { onOpenPerson(person.id) },
                onRemove = onRemovePerson?.let { removePerson -> { removePerson(person.id) } }
            )
        }
    }
}

@Composable
internal fun EventChipAvatars(people: List<PersonOption>, ringColor: Color) {
    val ringModifier = Modifier.border(EventChipAvatarRingWidth, ringColor, CircleShape)
    Row(horizontalArrangement = Arrangement.spacedBy(-EventChipAvatarOverlap)) {
        people.take(MaxAvatarsOnEventChip).forEach { person ->
            PersonAvatar(
                personNoteId = person.id,
                name = person.name,
                icon = person.icon,
                size = EventChipAvatarSize,
                modifier = ringModifier
            )
        }
        if (people.size > MaxAvatarsOnEventChip) {
            MorePeopleAvatar(hiddenCount = people.size - MaxAvatarsOnEventChip, size = EventChipAvatarSize, modifier = ringModifier)
        }
    }
}
