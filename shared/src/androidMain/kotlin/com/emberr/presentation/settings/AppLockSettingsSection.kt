package com.emberr.presentation.settings

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.emberr.MainActivity
import com.emberr.domain.applock.AppLockManager
import com.emberr.domain.applock.AutoLockDelay
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.applock.SystemUnlockResult
import com.emberr.presentation.applock.phoneHasScreenLock
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.eye_closed
import emberr.shared.generated.resources.lock_keyhole2
import emberr.shared.generated.resources.timer_reset
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
actual fun AppLockSettingsSection() {
    val appLockManager = koinInject<AppLockManager>()
    val isAppLockOn by appLockManager.isAppLockOn.collectAsState()
    val autoLockDelay by appLockManager.autoLockDelay.collectAsState()
    val isScreenSecurityOn by appLockManager.isScreenSecurityOn.collectAsState()
    val systemUnlockPrompt = (LocalActivity.current as? MainActivity)?.systemUnlockPrompt
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun changeAppLock(turnOn: Boolean) {
        if (!context.phoneHasScreenLock()) {
            if (turnOn) showNativeToast("Set a screen lock on your phone first.") else appLockManager.turnOff()
            return
        }
        val prompt = systemUnlockPrompt ?: return
        coroutineScope.launch {
            val result = prompt.ask(title = if (turnOn) "Turn on App Lock" else "Turn off App Lock")
            when (result) {
                SystemUnlockResult.Unlocked -> if (turnOn) appLockManager.turnOn() else appLockManager.turnOff()
                SystemUnlockResult.Cancelled -> Unit
                is SystemUnlockResult.Failed -> showNativeToast(result.message)
            }
        }
    }

    SettingsGroup(title = "Security") {
        SettingsToggleRow(
            icon = painterResource(Res.drawable.lock_keyhole2),
            title = "App Lock",
            isChecked = isAppLockOn,
            onCheckedChange = ::changeAppLock
        )

        AnimatedVisibility(
            visible = isAppLockOn,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SettingsDivider()
                SettingsChoiceRow(
                    icon = painterResource(Res.drawable.timer_reset),
                    title = "Auto-Lock",
                    value = autoLockDelayLabel(autoLockDelay),
                    choices = AutoLockDelay.entries.map { delay ->
                        SettingsChoice(
                            label = autoLockDelayLabel(delay),
                            isSelected = delay == autoLockDelay,
                            onSelect = { appLockManager.changeAutoLockDelay(delay) }
                        )
                    }
                )
            }
        }

        SettingsDivider()
        SettingsToggleRow(
            icon = painterResource(Res.drawable.eye_closed),
            title = "Screen Security",
            isChecked = isScreenSecurityOn,
            subtitle = screenSecurityFootnote,
            onCheckedChange = appLockManager::changeScreenSecurity
        )
    }
}

private val screenSecurityFootnote = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    "Hides Emberr's content in Recents."
} else {
    "Hides Emberr's content in Recents. Screenshots are blocked too."
}

private fun autoLockDelayLabel(delay: AutoLockDelay): String = when (delay) {
    AutoLockDelay.IMMEDIATELY -> "Immediately"
    AutoLockDelay.ONE_MINUTE -> "After 1 minute"
    AutoLockDelay.FIVE_MINUTES -> "After 5 minutes"
    AutoLockDelay.FIFTEEN_MINUTES -> "After 15 minutes"
    AutoLockDelay.ONE_HOUR -> "After 1 hour"
}
