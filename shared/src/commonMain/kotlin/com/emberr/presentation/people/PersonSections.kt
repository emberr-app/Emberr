package com.emberr.presentation.people

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.data.local.room.entity.CalendarTaskEntity
import com.emberr.data.local.room.entity.personNoteIdList
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.people.readPersonBirthday
import com.emberr.ui.theme.LocalAppIsDark
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import com.emberr.ui.theme.highlightBackgroundColor
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.at
import emberr.shared.generated.resources.calendar_date
import emberr.shared.generated.resources.category2
import emberr.shared.generated.resources.chevron_right
import emberr.shared.generated.resources.house
import emberr.shared.generated.resources.link
import emberr.shared.generated.resources.mail
import emberr.shared.generated.resources.pen
import emberr.shared.generated.resources.phone
import emberr.shared.generated.resources.star
import emberr.shared.generated.resources.tags
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource

internal const val PAST_EVENTS_SHOWN_BEFORE_SHOW_ALL = 3
private const val PAST_EVENT_ALPHA = 0.6f
private const val KIND_TILE_GRADIENT_END_STOP = 0.7f

internal val PersonCardShape = RoundedCornerShape(12.dp)
internal val SectionTitleInset = 6.dp
private val KindTileSize = 32.dp
private val KindTileShape = RoundedCornerShape(8.dp)
private val KindTileIconSize = 17.dp
private val DateTileWidth = 46.dp
private val DateTileShape = RoundedCornerShape(8.dp)
private val CategoryDotSize = 8.dp
private val DetailDividerStart = 58.dp
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
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
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

@Composable
internal fun PersonDetailsSection(details: List<PersonDetailBlock>, today: LocalDate) {
    Column {
        PersonSectionHeader(title = "Details", count = details.size.takeIf { it > 0 })
        PersonCard {
            if (details.isEmpty()) CardEmptyLine("No details yet")
            details.forEachIndexed { index, detail ->
                if (index > 0) CardRowDivider(start = DetailDividerStart)
                PersonDetailRow(detail = detail, today = today)
            }
        }
    }
}

@Composable
private fun PersonDetailRow(detail: PersonDetailBlock, today: LocalDate) {
    val shownValue = if (detail.kind == PersonDetailKind.BIRTHDAY) {
        readPersonBirthday(detail.value)?.let { birthday -> birthdayText(birthday, today) } ?: detail.value
    } else {
        detail.value
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KindTile(kind = detail.kind)
        Spacer(modifier = Modifier.width(12.dp))
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
                text = shownValue,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun KindTile(kind: PersonDetailKind) {
    val colors = kindTileColors(kind)
    Box(
        modifier = Modifier
            .size(KindTileSize)
            .clip(KindTileShape)
            .background(
                Brush.verticalGradient(
                    0f to colors.gradientStart,
                    KIND_TILE_GRADIENT_END_STOP to colors.gradientEnd
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = kindIcon(kind),
            contentDescription = kind.label,
            tint = colors.icon,
            modifier = Modifier.size(KindTileIconSize)
        )
    }
}

@Composable
private fun kindIcon(kind: PersonDetailKind): Painter = painterResource(
    when (kind) {
        PersonDetailKind.PHONE -> Res.drawable.phone
        PersonDetailKind.EMAIL -> Res.drawable.mail
        PersonDetailKind.BIRTHDAY -> Res.drawable.calendar_date
        PersonDetailKind.ADDRESS -> Res.drawable.house
        PersonDetailKind.COMPANY -> Res.drawable.category2
        PersonDetailKind.ROLE -> Res.drawable.pen
        PersonDetailKind.WEBSITE -> Res.drawable.link
        PersonDetailKind.SOCIAL -> Res.drawable.at
        PersonDetailKind.RELATIONSHIP -> Res.drawable.star
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
        eventDate(event)?.let { date -> EventDateTile(date = date, isHighlighted = isSoonest) }
        Spacer(modifier = Modifier.width(14.dp))
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
