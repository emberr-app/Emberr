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
import emberr.shared.generated.resources.settings_app_lock
import emberr.shared.generated.resources.settings_app_lock_needs_screen_lock
import emberr.shared.generated.resources.settings_app_lock_turn_off_prompt
import emberr.shared.generated.resources.settings_app_lock_turn_on_prompt
import emberr.shared.generated.resources.settings_auto_lock
import emberr.shared.generated.resources.settings_auto_lock_after_fifteen_minutes
import emberr.shared.generated.resources.settings_auto_lock_after_five_minutes
import emberr.shared.generated.resources.settings_auto_lock_after_one_hour
import emberr.shared.generated.resources.settings_auto_lock_after_one_minute
import emberr.shared.generated.resources.settings_auto_lock_immediately
import emberr.shared.generated.resources.settings_group_security
import emberr.shared.generated.resources.settings_screen_security
import emberr.shared.generated.resources.settings_screen_security_hint
import emberr.shared.generated.resources.settings_screen_security_hint_with_screenshots
import emberr.shared.generated.resources.eye_closed
import emberr.shared.generated.resources.lock_keyhole2
import emberr.shared.generated.resources.timer_reset
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
    val screenLockNeededMessage = stringResource(Res.string.settings_app_lock_needs_screen_lock)
    val turnOnPromptTitle = stringResource(Res.string.settings_app_lock_turn_on_prompt)
    val turnOffPromptTitle = stringResource(Res.string.settings_app_lock_turn_off_prompt)

    fun changeAppLock(turnOn: Boolean) {
        if (!context.phoneHasScreenLock()) {
            if (turnOn) showNativeToast(screenLockNeededMessage) else appLockManager.turnOff()
            return
        }
        val prompt = systemUnlockPrompt ?: return
        coroutineScope.launch {
            val result = prompt.ask(title = if (turnOn) turnOnPromptTitle else turnOffPromptTitle)
            when (result) {
                SystemUnlockResult.Unlocked -> if (turnOn) appLockManager.turnOn() else appLockManager.turnOff()
                SystemUnlockResult.Cancelled -> Unit
                is SystemUnlockResult.Failed -> showNativeToast(result.message)
            }
        }
    }

    SettingsGroup(title = stringResource(Res.string.settings_group_security)) {
        SettingsToggleRow(
            icon = painterResource(Res.drawable.lock_keyhole2),
            title = stringResource(Res.string.settings_app_lock),
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
                    title = stringResource(Res.string.settings_auto_lock),
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
            title = stringResource(Res.string.settings_screen_security),
            isChecked = isScreenSecurityOn,
            subtitle = stringResource(screenSecurityFootnoteResource),
            onCheckedChange = appLockManager::changeScreenSecurity
        )
    }
}

private val screenSecurityFootnoteResource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    Res.string.settings_screen_security_hint
} else {
    Res.string.settings_screen_security_hint_with_screenshots
}

@Composable
private fun autoLockDelayLabel(delay: AutoLockDelay): String = stringResource(
    when (delay) {
        AutoLockDelay.IMMEDIATELY -> Res.string.settings_auto_lock_immediately
        AutoLockDelay.ONE_MINUTE -> Res.string.settings_auto_lock_after_one_minute
        AutoLockDelay.FIVE_MINUTES -> Res.string.settings_auto_lock_after_five_minutes
        AutoLockDelay.FIFTEEN_MINUTES -> Res.string.settings_auto_lock_after_fifteen_minutes
        AutoLockDelay.ONE_HOUR -> Res.string.settings_auto_lock_after_one_hour
    }
)
