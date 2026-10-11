package com.emberr.presentation.desktop

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DesktopEscapeKeyBus {
    private val _presses = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val presses = _presses.asSharedFlow()

    fun notifyPressed() {
        _presses.tryEmit(Unit)
    }
}
