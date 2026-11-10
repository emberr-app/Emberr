package com.emberr.presentation.shared.editor.blockViews.property

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.TextAlignment
import com.emberr.presentation.people.PersonView
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.PeoplePicker
import com.emberr.presentation.shared.components.PersonOption
import com.emberr.presentation.shared.components.PersonPill
import com.emberr.presentation.shared.components.rememberPeopleToPick
import com.emberr.presentation.shared.components.rememberPersonCreator
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.property_add_people
import emberr.shared.generated.resources.property_empty
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val PropertyPeopleHorizontalPadding = 12.dp
private val PropertyPeopleVerticalPadding = 6.dp

internal fun peopleLinkedBy(block: PropertyBlock, people: List<PersonOption>): List<PersonOption> {
    val peopleById = people.associateBy { it.id }
    return block.personNoteIds.mapNotNull { personNoteId -> peopleById[personNoteId] }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PropertyPeopleValue(
    block: PropertyBlock,
    inSelectionMode: Boolean,
    onUpdatePeople: (List<String>) -> Unit,
    runAfterKeyboardCloses: (() -> Unit) -> Unit,
    widthModifier: Modifier,
    pillsOpenPersonPage: Boolean = true,
    alignment: TextAlignment? = null,
    wrapsText: Boolean = true
) {
    val people = rememberPeopleToPick()
    val createPerson = rememberPersonCreator()
    var showsPeoplePicker by remember { mutableStateOf(false) }
    var openPersonNoteId by remember { mutableStateOf<String?>(null) }
    val linkedPeople = people?.let { peopleLinkedBy(block, it) }
    val latestPersonNoteIds by rememberUpdatedState(block.personNoteIds)

    fun openPicker() = runAfterKeyboardCloses { showsPeoplePicker = true }

    val pills = @Composable {
        linkedPeople.orEmpty().forEach { person ->
            PersonPill(
                person = person,
                onOpen = {
                    when {
                        inSelectionMode -> Unit
                        pillsOpenPersonPage -> openPersonNoteId = person.id
                        else -> openPicker()
                    }
                },
                onRemove = null
            )
        }
    }

    Box {
        Box(
            modifier = widthModifier
                .clickable(enabled = !inSelectionMode) { openPicker() }
                .padding(horizontal = PropertyPeopleHorizontalPadding, vertical = PropertyPeopleVerticalPadding),
            contentAlignment = alignment.toBoxAlignment()
        ) {
            when {
                linkedPeople == null -> Unit
                linkedPeople.isEmpty() -> Text(
                    text = stringResource(Res.string.property_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
                !wrapsText -> Box(modifier = Modifier.clipToBounds()) {
                    Row(
                        modifier = Modifier.wrapContentWidth(align = Alignment.Start, unbounded = true),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        pills()
                    }
                }
                else -> FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    pills()
                    if (pillsOpenPersonPage && !inSelectionMode) {
                        Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                            IconTooltip(text = stringResource(Res.string.property_add_people)) {
                                Icon(
                                    painter = painterResource(Res.drawable.plus),
                                    contentDescription = stringResource(Res.string.property_add_people),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable { openPicker() }
                                        .padding(4.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showsPeoplePicker && people != null) {
            PeoplePicker(
                personNoteIds = block.personNoteIds,
                people = people,
                onPeopleChange = onUpdatePeople,
                onCreatePerson = { name ->
                    createPerson(name) { newPersonNoteId -> onUpdatePeople(latestPersonNoteIds + newPersonNoteId) }
                },
                onDismiss = { showsPeoplePicker = false }
            )
        }
    }

    openPersonNoteId?.let { personNoteId ->
        PersonView(personNoteId = personNoteId, onDismiss = { openPersonNoteId = null })
    }
}
