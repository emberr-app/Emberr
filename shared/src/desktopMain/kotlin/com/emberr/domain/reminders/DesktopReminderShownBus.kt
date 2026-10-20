package com.emberr.domain.reminders

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DesktopReminderShownBus {

    private val _remindersShown = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val remindersShown: SharedFlow<Unit> = _remindersShown.asSharedFlow()

    fun reminderShown() {
        _remindersShown.tryEmit(Unit)
    }
}
