package com.emberr.domain.util.eventbus

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WidgetComposeRequest { NEW_TASK, NEW_NOTE, NEW_EVENT, CONTINUE_DAILY }

object WidgetComposeRequestBus {
    private val pendingRequest = MutableStateFlow<WidgetComposeRequest?>(null)
    val pendingRequests: StateFlow<WidgetComposeRequest?> = pendingRequest.asStateFlow()

    fun request(request: WidgetComposeRequest) {
        pendingRequest.value = request
    }

    fun consume(request: WidgetComposeRequest): Boolean =
        pendingRequest.compareAndSet(request, null)
}
