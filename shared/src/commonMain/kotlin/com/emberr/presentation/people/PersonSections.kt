package com.emberr.presentation.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.personNoteIdList
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.IconTooltip
import com.emberr.presentation.shared.components.emberrControlContainerColor
import com.emberr.ui.theme.LocalAppIsDark
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import com.emberr.ui.theme.highlightBackgroundColor
import com.emberr.presentation.shared.components.orHighContrastLine
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.at
import emberr.shared.generated.resources.calendar_date
import emberr.shared.generated.resources.building
import emberr.shared.generated.resources.chevron_right
import emberr.shared.generated.resources.copy
import emberr.shared.generated.resources.address_card
import emberr.shared.generated.resources.link
import emberr.shared.generated.resources.mail
import emberr.shared.generated.resources.message
import emberr.shared.generated.resources.phone
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.square_arrow_out_up_right
import emberr.shared.generated.resources.star_outline
import emberr.shared.generated.resources.tag_user2
import emberr.shared.generated.resources.tags
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

internal const val PAST_EVENTS_SHOWN_BEFORE_SHOW_ALL = 3
private const val PAST_EVENT_ALPHA = 0.6f
private const val DETAIL_HEADING_ALPHA = 0.6f

internal val PersonCardShape = RoundedCornerShape(12.dp)
internal val SectionTitleInset = 6.dp
private val KindTileSize = 32.dp
private val KindIconSize = 18.dp
private val DateTileWidth = 46.dp
private val DateTileShape = RoundedCornerShape(8.dp)
private val CategoryDotSize = 8.dp
private val DetailCardGap = 12.dp
private val DetailHeadingIconSize = 14.dp
private val ChoiceChipShape = RoundedCornerShape(8.dp)
private val ChoiceChipHorizontalPadding = 10.dp
private val ChoiceChipVerticalPadding = 5.dp
private val EventDividerStart = 70.dp

@Composable
internal fun personPageColor(): Color =
    if (LocalAppIsDark.current) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.background

@Composable
internal fun personCardColor(): Color =
    if (LocalAppIsDark.current) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface

@Composable
internal fun mutedTextColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

@Composable
internal fun secondaryTextColor(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)

@Composable
internal fun PersonSectionHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier.padding(start = SectionTitleInset, end = SectionTitleInset, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.alignByBaseline()
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = mutedTextColor(),
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}

@Composable
internal fun PersonCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PersonCardShape)
            .background(personCardColor()),
        content = content
    )
}

@Composable
private fun CardRowDivider(start: Dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = start),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f).orHighContrastLine()
    )
}

@Composable
private fun CardEmptyLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = mutedTextColor(),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(20.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PersonDetailsSection(
    details: List<PersonDetailBlock>,
    today: LocalDate,
    editing: PersonDetailEditing,
    actions: PersonDetailActions
) {
    val missingSuggestedKinds = suggestedDetailKinds.filter { kind -> details.none { it.kind == kind } }
    Column {
        PersonSectionHeader(title = "Details", count = details.size.takeIf { it > 0 })
        Column(verticalArrangement = Arrangement.spacedBy(DetailCardGap)) {
            details.forEach { detail ->
                val anchor = detailAnchor(detail.id)
                Column {
                    DetailHeading(kind = detail.kind)
                    PersonCard {
                        Box {
                            PersonDetailRow(
                                detail = detail,
                                today = today,
                                actions = actions,
                                onClick = { editing.onPopupChange(PersonDetailPopup.Editing(detail.kind, detail, anchor)) }
                            )
                            PersonDetailPopupAt(anchor = anchor, editing = editing)
                        }
                    }
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                missingSuggestedKinds.forEach { kind ->
                    val anchor = suggestionAnchor(kind)
                    Box {
                        DetailPill(
                            text = kind.label,
                            onClick = { editing.onPopupChange(PersonDetailPopup.Editing(kind, null, anchor)) }
                        )
                        PersonDetailPopupAt(anchor = anchor, editing = editing)
                    }
                }
                Box {
                    DetailPill(
                        text = "More details",
                        onClick = { editing.onPopupChange(PersonDetailPopup.ChoosingKind(ADD_DETAIL_ANCHOR)) }
                    )
                    PersonDetailPopupAt(anchor = ADD_DETAIL_ANCHOR, editing = editing)
                }
            }
        }
    }
}

@Composable
private fun DetailPill(text: String, onClick: () -> Unit) {
    PersonChoiceChip(
        text = text,
        isChosen = false,
        icon = painterResource(Res.drawable.plus),
        containerColor = personCardColor(),
        onClick = onClick
    )
}

@Composable
private fun DetailHeading(kind: PersonDetailKind) {
    val headingColor = MaterialTheme.colorScheme.onSurface.copy(alpha = DETAIL_HEADING_ALPHA)
    Row(
        modifier = Modifier.padding(start = SectionTitleInset, end = SectionTitleInset, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = kindIcon(kind),
            contentDescription = null,
            tint = headingColor,
            modifier = Modifier.size(DetailHeadingIconSize)
        )
        Text(
            text = kind.label,
            style = MaterialTheme.typography.labelSmall,
            color = headingColor
        )
    }
}

@Composable
private fun PersonDetailRow(
    detail: PersonDetailBlock,
    today: LocalDate,
    actions: PersonDetailActions,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            detail.label?.takeIf { it.isNotBlank() }?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = mutedTextColor()
                )
            }
            Text(
                text = shownDetailValue(detail, today),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(if (isDesktopPlatform) 4.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (detail.kind == PersonDetailKind.PHONE && !isDesktopPlatform) {
                DetailActionButton(Res.drawable.phone, "Call") { actions.openLink(phoneCallLink(detail.value)) }
                DetailActionButton(Res.drawable.message, "Message") { actions.openLink(phoneMessageLink(detail.value)) }
            }
            if (detail.kind == PersonDetailKind.EMAIL) {
                DetailActionButton(Res.drawable.mail, "Write email") { actions.openLink(emailLink(detail.value)) }
            }
            openLinkFor(detail)?.let { link ->
                DetailActionButton(Res.drawable.square_arrow_out_up_right, "Open") { actions.openLink(link) }
            }
            DetailActionButton(Res.drawable.copy, "Copy") { actions.copy(shownDetailValue(detail, today)) }
        }
    }
}

@Composable
private fun DetailActionButton(icon: DrawableResource, description: String, onClick: () -> Unit) {
    IconTooltip(text = description) {
        Box(
            modifier = Modifier
                .size(if (isDesktopPlatform) 28.dp else 40.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = description,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(if (isDesktopPlatform) 16.dp else 18.dp)
            )
        }
    }
}

@Composable
internal fun PersonChoiceChip(
    text: String,
    isChosen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    iconDescription: String? = null,
    containerColor: Color = emberrControlContainerColor()
) {
    val contentColor = if (isChosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val labelLineHeight = with(LocalDensity.current) { MaterialTheme.typography.labelSmall.lineHeight.toDp() }
    Row(
        modifier = modifier
            .heightIn(min = labelLineHeight + ChoiceChipVerticalPadding * 2)
            .clip(ChoiceChipShape)
            .background(if (isChosen) MaterialTheme.colorScheme.primary else containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = ChoiceChipHorizontalPadding, vertical = ChoiceChipVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = icon,
                contentDescription = iconDescription,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
        }
        if (text.isNotEmpty()) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun KindTile(kind: PersonDetailKind) {
    Box(modifier = Modifier.size(KindTileSize), contentAlignment = Alignment.Center) {
        Icon(
            painter = kindIcon(kind),
            contentDescription = kind.label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(KindIconSize)
        )
    }
}

@Composable
internal fun kindIcon(kind: PersonDetailKind): Painter = painterResource(
    when (kind) {
        PersonDetailKind.PHONE -> Res.drawable.phone
        PersonDetailKind.EMAIL -> Res.drawable.mail
        PersonDetailKind.BIRTHDAY -> Res.drawable.calendar_date
        PersonDetailKind.ADDRESS -> Res.drawable.address_card
        PersonDetailKind.COMPANY -> Res.drawable.building
        PersonDetailKind.ROLE -> Res.drawable.tag_user2
        PersonDetailKind.WEBSITE -> Res.drawable.link
        PersonDetailKind.SOCIAL -> Res.drawable.at
        PersonDetailKind.RELATIONSHIP -> Res.drawable.star_outline
        PersonDetailKind.CUSTOM -> Res.drawable.tags
    }
)

internal class PersonEventRowLook(
    val namesByPersonNoteId: Map<String, String>,
    val categoryColorsById: Map<String, Color>,
    val shownPersonNoteId: String
)

@Composable
internal fun PersonUpcomingSection(
    upcoming: List<CalendarTaskEntity>,
    firstName: String,
    look: PersonEventRowLook,
    onOpenEvent: (CalendarTaskEntity) -> Unit
) {
    Column {
        PersonSectionHeader(title = "Upcoming", count = upcoming.size)
        PersonCard {
            if (upcoming.isEmpty()) CardEmptyLine("Nothing planned with $firstName")
            upcoming.forEachIndexed { index, event ->
                if (index > 0) CardRowDivider(start = EventDividerStart)
                PersonEventRow(
                    event = event,
                    look = look,
                    isSoonest = index == 0,
                    isPast = false,
                    onClick = { onOpenEvent(event) }
                )
            }
        }
    }
}

@Composable
internal fun PersonPastSection(
    past: List<CalendarTaskEntity>,
    showsAll: Boolean,
    onShowsAllChange: (Boolean) -> Unit,
    look: PersonEventRowLook,
    onOpenEvent: (CalendarTaskEntity) -> Unit
) {
    val shownEvents = if (showsAll) past else past.take(PAST_EVENTS_SHOWN_BEFORE_SHOW_ALL)
    Column {
        PersonSectionHeader(title = "Past", count = past.size)
        PersonCard {
            if (past.isEmpty()) CardEmptyLine("No past events")
            shownEvents.forEachIndexed { index, event ->
                if (index > 0) CardRowDivider(start = EventDividerStart)
                PersonEventRow(
                    event = event,
                    look = look,
                    isSoonest = false,
                    isPast = true,
                    onClick = { onOpenEvent(event) }
                )
            }
            if (past.size > PAST_EVENTS_SHOWN_BEFORE_SHOW_ALL) {
                CardRowDivider(start = 0.dp)
                Text(
                    text = if (showsAll) "Show fewer" else "Show all ${past.size}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = secondaryTextColor(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShowsAllChange(!showsAll) }
                        .padding(13.dp)
                )
            }
        }
    }
}

@Composable
private fun PersonEventRow(
    event: CalendarTaskEntity,
    look: PersonEventRowLook,
    isSoonest: Boolean,
    isPast: Boolean,
    onClick: () -> Unit
) {
    val otherPeopleNames = event.personNoteIdList()
        .filter { personNoteId -> personNoteId != look.shownPersonNoteId }
        .mapNotNull { personNoteId -> look.namesByPersonNoteId[personNoteId] }
    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(if (isPast) Modifier.alpha(PAST_EVENT_ALPHA) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        eventDate(event)?.let { date ->
            EventDateTile(date = date, isHighlighted = isSoonest)
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(CategoryDotSize)
                        .clip(CircleShape)
                        .background(event.categoryId?.let { look.categoryColorsById[it] } ?: MaterialTheme.colorScheme.outline)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = event.text.ifBlank { "Untitled event" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val timeLine = eventTimeLine(event, otherPeopleNames, usesTwentyFourHourClock)
            if (timeLine.isNotEmpty()) {
                Text(
                    text = timeLine,
                    style = MaterialTheme.typography.labelSmall,
                    color = secondaryTextColor(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            painter = painterResource(Res.drawable.chevron_right),
            contentDescription = null,
            tint = mutedTextColor(),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun EventDateTile(date: LocalDate, isHighlighted: Boolean) {
    val smallStyle = MaterialTheme.typography.labelSmall.let { style -> style.copy(lineHeight = style.fontSize) }
    val dayStyle = MaterialTheme.typography.titleLarge.let { style -> style.copy(lineHeight = style.fontSize) }
    Column(
        modifier = Modifier
            .width(DateTileWidth)
            .clip(DateTileShape)
            .background(if (isHighlighted) highlightBackgroundColor else personPageColor())
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = shortWeekdayName(date), style = smallStyle, fontWeight = FontWeight.Medium, color = secondaryTextColor())
        Text(text = date.day.toString(), style = dayStyle, color = MaterialTheme.colorScheme.onSurface)
        Text(text = shortMonthName(date), style = smallStyle, fontWeight = FontWeight.Medium, color = secondaryTextColor())
    }
}
