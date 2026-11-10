package com.emberr.domain.people

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.reminders.ReminderScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.common_untitled
import emberr.shared.generated.resources.notifications_birthday
import emberr.shared.generated.resources.notifications_birthday_today
import emberr.shared.generated.resources.notifications_birthday_turns_age
import org.jetbrains.compose.resources.getString

private const val BIRTHDAY_REMINDER_ID_PREFIX = "birthday_"

data class BirthdayReminder(
    val reminderId: String,
    val personName: String,
    val turningAge: Int?,
    val timestamp: Long
)

fun birthdayReminderId(personNoteId: String): String = BIRTHDAY_REMINDER_ID_PREFIX + personNoteId

fun personNoteIdOfBirthdayReminder(reminderId: String): String? =
    reminderId.takeIf { it.startsWith(BIRTHDAY_REMINDER_ID_PREFIX) }?.removePrefix(BIRTHDAY_REMINDER_ID_PREFIX)

fun nextBirthdayReminder(
    person: PersonWithDetails,
    reminderMinuteOfDay: Int,
    nowMillis: Long,
    timeZone: TimeZone
): BirthdayReminder? {
    val today = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
    val reminderTime = LocalTime(reminderMinuteOfDay / 60, reminderMinuteOfDay % 60)
    val lastDateToLook = today.plus(1, DateTimeUnit.YEAR).plus(1, DateTimeUnit.DAY)
    return birthdaysInRange(listOf(person), today, lastDateToLook)
        .map { birthday -> birthday to birthday.date.atTime(reminderTime).toInstant(timeZone).toEpochMilliseconds() }
        .firstOrNull { (_, timestamp) -> timestamp > nowMillis }
        ?.let { (birthday, timestamp) ->
            BirthdayReminder(
                reminderId = birthdayReminderId(birthday.personNoteId),
                personName = birthday.personName.trim(),
                turningAge = birthday.turningAge,
                timestamp = timestamp
            )
        }
}

data class BirthdaySummary(
    val personNoteId: String,
    val name: String,
    val birthdayValues: List<String>
)

fun birthdaySummaryOf(people: List<PersonWithDetails>): List<BirthdaySummary> =
    people.filter { person -> person.hasBirthday() }.map { person ->
        BirthdaySummary(
            personNoteId = person.person.noteId,
            name = person.person.title,
            birthdayValues = person.details.filter { detail -> detail.kind == PersonDetailKind.BIRTHDAY }.map { it.value }
        )
    }

private suspend fun birthdayReminderText(reminder: BirthdayReminder): String {
    val name = reminder.personName.ifEmpty { getString(Res.string.common_untitled) }
    return reminder.turningAge?.let { age -> getString(Res.string.notifications_birthday_turns_age, name, age) }
        ?: getString(Res.string.notifications_birthday_today, name)
}

class BirthdayReminderScheduler(
    private val peopleRepository: PeopleRepository,
    private val settingsManager: SettingsManager,
    private val reminderScheduler: ReminderScheduler,
    private val appScope: CoroutineScope
) {
    private var watchingJob: Job? = null
    private val reschedulingLock = Mutex()

    fun start() {
        if (watchingJob != null) return
        watchingJob = appScope.launch {
            try {
                val birthdaysThatShapeReminders = peopleRepository.observePeopleWithDetails().map(::birthdaySummaryOf)
                combine(
                    birthdaysThatShapeReminders,
                    settingsManager.birthdayRemindersEnabledFlow,
                    settingsManager.birthdayReminderMinuteOfDayFlow
                ) { birthdays, isEnabled, minuteOfDay -> Triple(birthdays, isEnabled, minuteOfDay) }
                    .distinctUntilChanged()
                    .collect { rescheduleBirthdayReminders() }
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                cause.printStackTrace()
            }
        }
    }

    suspend fun rescheduleBirthdayReminders() {
        reschedulingLock.withLock {
            try {
                val people = if (settingsManager.isBirthdayRemindersEnabled()) {
                    peopleRepository.observePeopleWithDetails().first()
                } else {
                    emptyList()
                }
                val reminderMinuteOfDay = settingsManager.getBirthdayReminderMinuteOfDay()
                val nowMillis = Clock.System.now().toEpochMilliseconds()
                val timeZone = TimeZone.currentSystemDefault()
                val reminders = people.mapNotNull { person ->
                    nextBirthdayReminder(person, reminderMinuteOfDay, nowMillis, timeZone)
                }
                val wantedReminderIds = reminders.map { it.reminderId }.toSet()
                val previouslyScheduledIds = settingsManager.getScheduledBirthdayReminderIds()
                settingsManager.saveScheduledBirthdayReminderIds(previouslyScheduledIds + wantedReminderIds)

                val idsThatFailedToCancel = (previouslyScheduledIds - wantedReminderIds).filterNot { reminderId ->
                    changeOneReminder { reminderScheduler.cancel(reminderId) }
                }
                val notificationTitle = getString(Res.string.notifications_birthday)
                reminders.forEach { reminder ->
                    val notificationText = birthdayReminderText(reminder)
                    changeOneReminder {
                        reminderScheduler.schedule(reminder.reminderId, notificationTitle, notificationText, reminder.timestamp)
                    }
                }
                settingsManager.saveScheduledBirthdayReminderIds(wantedReminderIds + idsThatFailedToCancel)
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                cause.printStackTrace()
            }
        }
    }

    private fun changeOneReminder(change: () -> Unit): Boolean =
        try {
            change()
            true
        } catch (cause: Exception) {
            cause.printStackTrace()
            false
        }
}
