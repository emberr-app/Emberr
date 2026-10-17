package com.emberr.presentation.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DesktopToast(val message: String, val shownAtNanos: Long)

object DesktopToastBus {
    private val _currentToast = MutableStateFlow<DesktopToast?>(null)
    val currentToast: StateFlow<DesktopToast?> = _currentToast.asStateFlow()

    fun show(message: String) {
        _currentToast.value = DesktopToast(message, System.nanoTime())
    }
}
