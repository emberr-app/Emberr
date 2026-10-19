package com.emberr.domain.util.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberDeviceLockPrompt(): DeviceLockPrompt = remember {
    object : DeviceLockPrompt {
        override suspend fun confirm(title: String, description: String): DeviceLockConfirmation =
            DeviceLockConfirmation.NO_DEVICE_LOCK
    }
}
