package com.emberr.presentation.applock

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.applock.AppLockManager
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.settings.SettingsViewModel
import com.emberr.ui.theme.EmberrTheme
import com.emberr.ui.theme.FontSizePreference
import com.emberr.ui.theme.FontStylePreference
import com.emberr.ui.theme.ThemePreference
import com.emberr.ui.theme.collectColorThemeSettings
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import kotlinx.coroutines.MainScope
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.app_lock_off_no_screen_lock
import emberr.shared.generated.resources.app_lock_unlock_emberr
import org.jetbrains.compose.resources.getString

private val appLockToastScope = MainScope()

fun Activity.openAppLockScreen() {
    startActivity(Intent(this, AppLockActivity::class.java))
}

class AppLockActivity : FragmentActivity() {

    private val appLockManager: AppLockManager by inject()
    private val settingsViewModel: SettingsViewModel by inject()
    private val settingsManager: SettingsManager by inject()
    private lateinit var systemUnlockPrompt: SystemUnlockPrompt
    private var hasAskedSinceShown = false
    private var unlockFailureMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        skipOpenAndCloseAnimations()
        enableEdgeToEdge()

        systemUnlockPrompt = SystemUnlockPrompt(this)
        onBackPressedDispatcher.addCallback(this) { moveTaskToBack(true) }

        setContent {
            val themePreferenceName by settingsViewModel.themePreference.collectAsState()
            val fontSizePreferenceName by settingsViewModel.fontSizePreference.collectAsState()
            val fontStylePreferenceName by settingsViewModel.fontStylePreference.collectAsState()
            val colorThemeSettings = settingsManager.collectColorThemeSettings()
            val interfaceScalePercent by settingsViewModel.interfaceScalePercent.collectAsState()

            val darkTheme = when (runCatching { ThemePreference.valueOf(themePreferenceName) }.getOrDefault(ThemePreference.SYSTEM)) {
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
            }

            EmberrTheme(
                darkTheme = darkTheme,
                fontSizePreference = runCatching { FontSizePreference.valueOf(fontSizePreferenceName) }
                    .getOrDefault(FontSizePreference.DEFAULT),
                fontStylePreference = runCatching { FontStylePreference.valueOf(fontStylePreferenceName) }
                    .getOrDefault(FontStylePreference.POPPINS),
                colorThemeSettings = colorThemeSettings,
                interfaceScalePercent = interfaceScalePercent
            ) {
                AppLockScreen(
                    unlockFailureMessage = unlockFailureMessage,
                    onUnlockClick = ::askToUnlock
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!appLockManager.isLocked.value) {
            unlockAndClose()
            return
        }
        if (!phoneHasScreenLock()) {
            appLockManager.turnOff()
            appLockToastScope.launch { showNativeToast(getString(Res.string.app_lock_off_no_screen_lock)) }
            unlockAndClose()
            return
        }
        if (!hasAskedSinceShown) {
            hasAskedSinceShown = true
            askToUnlock()
        }
    }

    override fun onStop() {
        super.onStop()
        if (!systemUnlockPrompt.isShowing) hasAskedSinceShown = false
    }

    private fun askToUnlock() {
        unlockFailureMessage = null
        lifecycleScope.launch {
            when (val result = systemUnlockPrompt.ask(title = getString(Res.string.app_lock_unlock_emberr))) {
                SystemUnlockResult.Unlocked -> unlockAndClose()
                SystemUnlockResult.Cancelled -> Unit
                is SystemUnlockResult.Failed -> unlockFailureMessage = result.message
            }
        }
    }

    private fun unlockAndClose() {
        appLockManager.unlock()
        finish()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    private fun skipOpenAndCloseAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
