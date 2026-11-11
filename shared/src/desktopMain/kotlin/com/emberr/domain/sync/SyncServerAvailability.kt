package com.emberr.domain.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.BindException

class SyncServerAvailability {

    private val _status = MutableStateFlow<SyncServerStatus>(SyncServerStatus.Idle)
    val status = _status.asStateFlow()

    fun markRunning(port: Int) {
        _status.value = SyncServerStatus.Running(port)
    }

    fun markIdle() {
        _status.value = SyncServerStatus.Idle
    }

    fun markUnavailable(port: Int, cause: Throwable) {
        val isPortAlreadyTaken = generateSequence(cause) { it.cause }
            .take(MAX_CAUSE_DEPTH)
            .any { it is BindException }
        _status.value = SyncServerStatus.Unavailable(port, isPortAlreadyTaken, cause.message)
    }

    private companion object {
        const val MAX_CAUSE_DEPTH = 5
    }
}
