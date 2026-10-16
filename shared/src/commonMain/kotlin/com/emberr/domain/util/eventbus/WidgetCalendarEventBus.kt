package com.emberr.domain.util.eventbus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class CalendarEventRequest(
    val blockId: String,
    val occurrenceDate: String?
)

object WidgetCalendarEventBus {
    private val _requestedEvents = MutableSharedFlow<CalendarEventRequest>(replay = 1)
    val requestedEvents = _requestedEvents.asSharedFlow()

    fun requestEvent(blockId: String, occurrenceDate: String? = null) {
        _requestedEvents.tryEmit(CalendarEventRequest(blockId, occurrenceDate))
    }

    fun consumeRequestedEvent() {
        _requestedEvents.resetReplayCache()
    }
}
