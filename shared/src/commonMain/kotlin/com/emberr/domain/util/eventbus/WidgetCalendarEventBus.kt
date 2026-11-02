package com.emberr.domain.util.eventbus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class CalendarEventRequest(
    val blockId: String,
    val occurrenceDate: String?,
    val birthdayPersonNoteId: String? = null
)

object WidgetCalendarEventBus {
    private val _requestedEvents = MutableSharedFlow<CalendarEventRequest>(replay = 1)
    val requestedEvents = _requestedEvents.asSharedFlow()

    fun requestEvent(blockId: String, occurrenceDate: String? = null, birthdayPersonNoteId: String? = null) {
        _requestedEvents.tryEmit(CalendarEventRequest(blockId, occurrenceDate, birthdayPersonNoteId))
    }

    fun consumeRequestedEvent() {
        _requestedEvents.resetReplayCache()
    }
}
