package com.emberr.domain.util.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

enum class DeviceLockConfirmation {
    CONFIRMED,
    NOT_CONFIRMED,
    NO_DEVICE_LOCK
}

@Stable
interface DeviceLockPrompt {
    suspend fun confirm(title: String, description: String): DeviceLockConfirmation
}

@Composable
expect fun rememberDeviceLockPrompt(): DeviceLockPrompt
