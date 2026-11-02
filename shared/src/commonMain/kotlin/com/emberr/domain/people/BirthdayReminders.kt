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

private const val BIRTHDAY_REMINDER_ID_PREFIX = "birthday_"
private const val BIRTHDAY_NOTIFICATION_TITLE = "Birthday"

data class BirthdayReminder(
    val reminderId: String,
    val text: String,
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
                text = birthdayReminderText(birthday),
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

private fun birthdayReminderText(birthday: BirthdayOnDate): String {
    val name = personDisplayName(birthday.personName)
    return birthday.turningAge?.let { age -> "$name turns $age today" } ?: "It's $name's birthday today"
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
                reminders.forEach { reminder ->
                    changeOneReminder {
                        reminderScheduler.schedule(reminder.reminderId, BIRTHDAY_NOTIFICATION_TITLE, reminder.text, reminder.timestamp)
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
