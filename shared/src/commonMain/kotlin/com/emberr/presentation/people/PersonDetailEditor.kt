package com.emberr.presentation.people

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.emberrBorderWidth
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrBottomSheetAction
import com.emberr.presentation.shared.components.EmberrBottomSheetOption
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrDesktopMenuOption
import com.emberr.presentation.shared.components.EmberrDesktopSubmenu
import com.emberr.presentation.shared.components.EmberrShadowElevation
import com.emberr.presentation.shared.components.EmberrTextField
import com.emberr.presentation.shared.components.TopBarIconButton
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.emberrBorderColor
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.trash
import emberr.shared.generated.resources.common_cancel
import emberr.shared.generated.resources.common_close
import emberr.shared.generated.resources.common_save
import emberr.shared.generated.resources.people_add_detail
import emberr.shared.generated.resources.people_already_added
import emberr.shared.generated.resources.people_birthday_date_not_in_year
import emberr.shared.generated.resources.people_birthday_day
import emberr.shared.generated.resources.people_birthday_month
import emberr.shared.generated.resources.people_birthday_optional
import emberr.shared.generated.resources.people_birthday_year
import emberr.shared.generated.resources.people_birthday_year_example
import emberr.shared.generated.resources.people_birthday_year_range
import emberr.shared.generated.resources.people_cannot_be_empty
import emberr.shared.generated.resources.people_custom_label
import emberr.shared.generated.resources.people_custom_label_placeholder
import emberr.shared.generated.resources.people_label_optional
import emberr.shared.generated.resources.people_remove
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal const val ADD_DETAIL_ANCHOR = "add-detail"
private const val DAYS_SHOWN_BEFORE_CHOSEN_DAY = 3
private const val HEADING_ALPHA = 0.6f

private val DesktopEditorWidth = 280.dp
private val MonthGridColumns = if (isDesktopPlatform) 4 else 6
private val KindMenuMinWidth = 220.dp
private val KindMenuMaxWidth = 300.dp
private val KindMenuIconSize = 18.dp
private val DayChipWidth = 40.dp
private val DayChipGap = 6.dp
private val FieldShape = RoundedCornerShape(12.dp)
private val SheetRowInset = 8.dp
private val SheetSidePadding = 20.dp

internal fun detailAnchor(detailId: String): String = "detail-$detailId"

internal fun suggestionAnchor(kind: PersonDetailKind): String = "suggestion-${kind.name}"

internal sealed interface PersonDetailPopup {
    val anchor: String

    data class ChoosingKind(override val anchor: String) : PersonDetailPopup

    data class Editing(
        val kind: PersonDetailKind,
        val detail: PersonDetailBlock?,
        override val anchor: String,
        val isFromPicker: Boolean = false
    ) : PersonDetailPopup
}

internal class PersonDetailEditing(
    val popup: PersonDetailPopup?,
    val personName: String,
    val hasBirthday: Boolean,
    val today: LocalDate,
    val onPopupChange: (PersonDetailPopup?) -> Unit,
    val onBack: () -> Unit,
    val onSave: (kind: PersonDetailKind, editedDetail: PersonDetailBlock?, label: String?, value: String) -> Unit,
    val onRemove: (PersonDetailBlock) -> Unit
)

internal class PersonDetailActions(
    val openLink: (String) -> Unit,
    val copy: (String) -> Unit
)

@Composable
private fun editorTitle(popup: PersonDetailPopup.Editing): String =
    if (popup.detail == null) popup.kind.addDetailTitle() else popup.kind.editDetailTitle()

@Composable
internal fun PersonDetailPopupAt(anchor: String, editing: PersonDetailEditing) {
    if (!isDesktopPlatform) return
    val popup = editing.popup?.takeIf { it.anchor == anchor }
    val closeAll = { editing.onPopupChange(null) }
    val finishAndCloseAll: (() -> Unit) -> Unit = { action ->
        action()
        closeAll()
    }
    if (anchor == ADD_DETAIL_ANCHOR) {
        EmberrDesktopMenu(
            expanded = popup is PersonDetailPopup.ChoosingKind,
            onDismissRequest = closeAll,
            modifier = Modifier.widthIn(min = KindMenuMinWidth, max = KindMenuMaxWidth)
        ) {
            PersonDetailKind.entries.forEach { kind ->
                if (kind == PersonDetailKind.BIRTHDAY && editing.hasBirthday) {
                    EmberrDesktopMenuOption(
                        label = kind.displayName(),
                        subtitle = stringResource(Res.string.people_already_added),
                        labelColor = MaterialTheme.colorScheme.outline,
                        icon = { KindMenuIcon(kind) },
                        onClick = {}
                    )
                } else {
                    EmberrDesktopSubmenu(
                        menuWidthEstimate = DesktopEditorWidth,
                        anchor = { openEditor, _ ->
                            EmberrDesktopMenuOption(label = kind.displayName(), icon = { KindMenuIcon(kind) }, onClick = openEditor)
                        }
                    ) { isEditorOpen, closeEditor ->
                        EmberrDesktopMenu(expanded = isEditorOpen, onDismissRequest = closeEditor) {
                            Column(modifier = Modifier.width(DesktopEditorWidth)) {
                                DetailEditorContent(
                                    popup = PersonDetailPopup.Editing(kind, null, anchor),
                                    editing = editing,
                                    finish = finishAndCloseAll,
                                    cancel = closeEditor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    EmberrDesktopMenu(expanded = popup is PersonDetailPopup.Editing, onDismissRequest = closeAll) {
        if (popup is PersonDetailPopup.Editing) {
            Column(modifier = Modifier.width(DesktopEditorWidth)) {
                DetailEditorContent(popup = popup, editing = editing, finish = finishAndCloseAll, cancel = closeAll)
            }
        }
    }
}

@Composable
internal fun PersonDetailSheet(editing: PersonDetailEditing) {
    if (isDesktopPlatform) return
    val popup = editing.popup
    val pickerAnchor = when (popup) {
        is PersonDetailPopup.ChoosingKind -> popup.anchor
        is PersonDetailPopup.Editing -> popup.anchor.takeIf { popup.isFromPicker }
        null -> null
    }
    EmberrBottomSheet(
        expanded = pickerAnchor != null,
        onDismiss = { editing.onPopupChange(null) },
        title = stringResource(Res.string.people_add_detail),
        contentHorizontalPadding = 0.dp
    ) { closeAnd ->
        Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp)) {
            PersonDetailKind.entries.forEach { kind ->
                val isTaken = kind == PersonDetailKind.BIRTHDAY && editing.hasBirthday
                EmberrBottomSheetOption(
                    label = kind.displayName(),
                    subtitle = if (isTaken) stringResource(Res.string.people_already_added) else null,
                    labelColor = if (isTaken) MaterialTheme.colorScheme.outline else null,
                    icon = { KindMenuIcon(kind) },
                    outerHorizontalPadding = SheetRowInset,
                    onClick = {
                        if (!isTaken && pickerAnchor != null) {
                            editing.onPopupChange(PersonDetailPopup.Editing(kind, null, pickerAnchor, isFromPicker = true))
                        }
                    }
                )
            }
            EmberrButtonPrimary(
                text = stringResource(Res.string.common_close),
                onClick = { closeAnd {} },
                modifier = Modifier.fillMaxWidth().padding(start = SheetSidePadding, end = SheetSidePadding, top = 12.dp)
            )
        }
    }
    if (popup is PersonDetailPopup.Editing) {
        EmberrBottomSheet(
            expanded = true,
            onDismiss = editing.onBack,
            title = editorTitle(popup),
            subtitle = shownPersonName(editing.personName),
            headerAction = popup.detail?.let { detail ->
                EmberrBottomSheetAction(
                    icon = painterResource(Res.drawable.trash),
                    contentDescription = stringResource(Res.string.people_remove),
                    tint = MaterialTheme.colorScheme.error,
                    closesSheet = true,
                    onClick = {
                        editing.onRemove(detail)
                        editing.onPopupChange(null)
                    }
                )
            }
        ) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp)) {
                DetailEditorContent(
                    popup = popup,
                    editing = editing,
                    finish = { action ->
                        closeAnd {
                            action()
                            editing.onPopupChange(null)
                        }
                    },
                    cancel = { closeAnd {} }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailEditorContent(
    popup: PersonDetailPopup.Editing,
    editing: PersonDetailEditing,
    finish: (() -> Unit) -> Unit,
    cancel: () -> Unit
) {
    val kind = popup.kind
    val isBirthday = kind == PersonDetailKind.BIRTHDAY
    val choices = labelChoices(kind)
    val savedLabel = popup.detail?.label.orEmpty()
    val thisYear = editing.today.year

    var chosenLabel by remember(popup) { mutableStateOf(savedLabel.takeIf { it in choices }) }
    var typedLabel by remember(popup) { mutableStateOf(if (savedLabel in choices) "" else savedLabel) }
    var showsTypedLabel by remember(popup) { mutableStateOf(choices.isEmpty() || typedLabel.isNotEmpty()) }
    var shouldFocusTypedLabel by remember(popup) { mutableStateOf(false) }
    val typedLabelFocusRequester = remember { FocusRequester() }
    var value by remember(popup) { mutableStateOf(if (isBirthday) "" else popup.detail?.value.orEmpty()) }
    var birthday by remember(popup) { mutableStateOf(birthdayChoiceFrom(popup.detail?.value.orEmpty())) }
    var showsMistakes by remember(popup) { mutableStateOf(false) }
    val valueFocusRequester = remember { FocusRequester() }

    LaunchedEffect(popup) {
        if (isDesktopPlatform && !isBirthday) runCatching { valueFocusRequester.requestFocus() }
    }

    LaunchedEffect(shouldFocusTypedLabel) {
        if (!shouldFocusTypedLabel) return@LaunchedEffect
        runCatching { typedLabelFocusRequester.requestFocus() }
        shouldFocusTypedLabel = false
    }

    fun save() {
        val valueToSave = if (isBirthday) birthday.toIsoText(thisYear) else value.trim().takeIf { it.isNotEmpty() }
        if (valueToSave == null) {
            showsMistakes = true
            return
        }
        val label = if (isBirthday) null else typedLabel.trim().ifEmpty { chosenLabel.orEmpty() }.takeIf { it.isNotEmpty() }
        finish { editing.onSave(kind, popup.detail, label, valueToSave) }
    }

    if (isDesktopPlatform) {
        DesktopEditorHeader(popup = popup, personName = editing.personName, onRemove = { detail -> finish { editing.onRemove(detail) } })
        EditorDivider()
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(if (isDesktopPlatform) 12.dp else 0.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isBirthday) {
            Column {
                EmberrTextField(
                    value = value,
                    onValueChange = { text ->
                        value = text
                        showsMistakes = false
                    },
                    placeholder = valuePlaceholder(kind),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardTypeFor(kind), imeAction = ImeAction.Done),
                    onSubmit = ::save,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(valueFocusRequester)
                        .markedWrongIf(showsMistakes && value.isBlank())
                )
                if (showsMistakes && value.isBlank()) MistakeLine(stringResource(Res.string.people_cannot_be_empty))
            }
            Column {
                if (choices.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = if (showsTypedLabel) 8.dp else 0.dp)
                    ) {
                        choices.forEach { choice ->
                            PersonChoiceChip(
                                text = choice,
                                isChosen = chosenLabel == choice,
                                onClick = {
                                    chosenLabel = if (chosenLabel == choice) null else choice
                                    typedLabel = ""
                                    showsTypedLabel = false
                                }
                            )
                        }
                        PersonChoiceChip(
                            text = "",
                            isChosen = showsTypedLabel,
                            icon = painterResource(Res.drawable.plus),
                            iconDescription = stringResource(Res.string.people_custom_label),
                            onClick = {
                                showsTypedLabel = !showsTypedLabel
                                typedLabel = ""
                                if (showsTypedLabel) {
                                    chosenLabel = null
                                    shouldFocusTypedLabel = true
                                }
                            }
                        )
                    }
                }
                if (showsTypedLabel) {
                    EmberrTextField(
                        value = typedLabel,
                        onValueChange = { text -> typedLabel = text },
                        placeholder = stringResource(if (choices.isNotEmpty()) Res.string.people_custom_label_placeholder else Res.string.people_label_optional),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        onSubmit = ::save,
                        modifier = Modifier.fillMaxWidth().focusRequester(typedLabelFocusRequester)
                    )
                }
            }
        } else {
            BirthdayFields(
                birthday = birthday,
                thisYear = thisYear,
                today = editing.today,
                showsMistakes = showsMistakes,
                onBirthdayChange = { changed -> birthday = changed },
                onSubmit = ::save
            )
        }
    }

    if (isDesktopPlatform) EditorDivider()
    Row(modifier = Modifier.fillMaxWidth().padding(if (isDesktopPlatform) PaddingValues(12.dp) else PaddingValues(top = 20.dp))) {
        EmberrButtonSecondary(text = stringResource(Res.string.common_cancel), onClick = cancel, modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.width(8.dp))
        EmberrButtonPrimary(text = stringResource(Res.string.common_save), onClick = ::save, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DesktopEditorHeader(
    popup: PersonDetailPopup.Editing,
    personName: String,
    onRemove: (PersonDetailBlock) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KindTile(kind = popup.kind)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = editorTitle(popup),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = shownPersonName(personName),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        popup.detail?.let { detail ->
            val removeLabel = stringResource(Res.string.people_remove)
            TopBarIconButton(
                icon = painterResource(Res.drawable.trash),
                contentDescription = removeLabel,
                bgColor = Color.Transparent,
                tint = MaterialTheme.colorScheme.error,
                shadowElevation = EmberrShadowElevation.None,
                size = 36.dp,
                iconSize = 18.dp,
                tooltip = removeLabel,
                onClick = { onRemove(detail) }
            )
        }
    }
}

@Composable
private fun KindMenuIcon(kind: PersonDetailKind) {
    Icon(
        painter = kindIcon(kind),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(KindMenuIconSize)
    )
}

@Composable
private fun EditorDivider() {
    HorizontalDivider(thickness = emberrBorderWidth, color = emberrBorderColor)
}

@Composable
private fun BirthdayFields(
    birthday: BirthdayChoice,
    thisYear: Int,
    today: LocalDate,
    showsMistakes: Boolean,
    onBirthdayChange: (BirthdayChoice) -> Unit,
    onSubmit: () -> Unit
) {
    Column {
        BirthdayHeading(title = stringResource(Res.string.people_birthday_month))
        Column(
            modifier = Modifier.markedWrongIf(showsMistakes && birthday.month == null),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            (1..12).chunked(MonthGridColumns).forEach { monthsInRow ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    monthsInRow.forEach { month ->
                        PersonChoiceChip(
                            text = shortMonthName(LocalDate(today.year, month, 1)),
                            isChosen = birthday.month == month,
                            onClick = { onBirthdayChange(birthday.withMonth(month)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
    Column {
        BirthdayHeading(title = stringResource(Res.string.people_birthday_day))
        val dayScrollState = rememberScrollState()
        val density = LocalDensity.current
        LaunchedEffect(Unit) {
            val daysBeforeShownDay = ((birthday.day ?: 1) - 1 - DAYS_SHOWN_BEFORE_CHOSEN_DAY).coerceAtLeast(0)
            dayScrollState.scrollTo(with(density) { ((DayChipWidth + DayChipGap) * daysBeforeShownDay).roundToPx() })
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(DayChipGap),
            modifier = Modifier
                .fillMaxWidth()
                .markedWrongIf(showsMistakes && birthday.day == null)
                .horizontalScroll(dayScrollState)
        ) {
            (1..daysInBirthdayMonth(birthday.month)).forEach { day ->
                PersonChoiceChip(
                    text = day.toString(),
                    isChosen = birthday.day == day,
                    onClick = { onBirthdayChange(birthday.copy(day = day)) },
                    modifier = Modifier.width(DayChipWidth)
                )
            }
        }
    }
    val isYearWrong = showsMistakes && !birthday.isYearValid(thisYear)
    Column {
        BirthdayHeading(title = stringResource(Res.string.people_birthday_year), note = stringResource(Res.string.people_birthday_optional))
        EmberrTextField(
            value = birthday.yearText,
            onValueChange = { text -> onBirthdayChange(birthday.copy(yearText = text.filter(Char::isDigit).take(4))) },
            placeholder = stringResource(Res.string.people_birthday_year_example),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            onSubmit = onSubmit,
            modifier = Modifier.fillMaxWidth().markedWrongIf(isYearWrong)
        )
        if (isYearWrong) MistakeLine(yearMistake(birthday, thisYear))
    }
    val previewBirthday = birthday.toBirthday(thisYear) ?: birthday.copy(yearText = "").toBirthday(thisYear)
    if (previewBirthday != null) {
        Text(
            text = birthdayText(previewBirthday, today),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun BirthdayHeading(title: String, note: String? = null) {
    Row(modifier = Modifier.padding(top = 4.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = HEADING_ALPHA)
        )
        if (note != null) {
            Text(text = note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun MistakeLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun Modifier.markedWrongIf(isWrong: Boolean): Modifier =
    if (isWrong) emberrBorder(FieldShape, color = MaterialTheme.colorScheme.error) else this

@Composable
private fun yearMistake(birthday: BirthdayChoice, thisYear: Int): String {
    val year = birthday.yearText.toIntOrNull()
    val month = birthday.month
    val day = birthday.day
    val isYearInRange = birthday.yearText.length == 4 && year != null && year in EARLIEST_BIRTH_YEAR..thisYear
    return if (isYearInRange && month != null && day != null) {
        stringResource(Res.string.people_birthday_date_not_in_year, "${fullMonthName(month)} $day", birthday.yearText)
    } else {
        stringResource(Res.string.people_birthday_year_range, EARLIEST_BIRTH_YEAR, thisYear)
    }
}

private fun keyboardTypeFor(kind: PersonDetailKind): KeyboardType = when (kind) {
    PersonDetailKind.PHONE -> KeyboardType.Phone
    PersonDetailKind.EMAIL -> KeyboardType.Email
    PersonDetailKind.WEBSITE -> KeyboardType.Uri
    else -> KeyboardType.Text
}
