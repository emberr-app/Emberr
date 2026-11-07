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
import com.emberr.domain.applock.AppLockManager
import com.emberr.domain.util.system.showNativeToast
import com.emberr.presentation.settings.SettingsViewModel
import com.emberr.ui.theme.EmberrTheme
import com.emberr.ui.theme.FontSizePreference
import com.emberr.ui.theme.FontStylePreference
import com.emberr.ui.theme.ThemePreference
import com.emberr.ui.theme.colorThemePreferenceNamed
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

fun Activity.openAppLockScreen() {
    startActivity(Intent(this, AppLockActivity::class.java))
}

class AppLockActivity : FragmentActivity() {

    private val appLockManager: AppLockManager by inject()
    private val settingsViewModel: SettingsViewModel by inject()
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
            val colorThemePreferenceName by settingsViewModel.colorThemePreference.collectAsState()
            val themedTextAndIcons by settingsViewModel.themedTextAndIcons.collectAsState()
            val colorThemeStrength by settingsViewModel.colorThemeStrength.collectAsState()
            val paperTextureEnabled by settingsViewModel.paperTextureEnabled.collectAsState()
            val paperTextureStrength by settingsViewModel.paperTextureStrength.collectAsState()

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
                colorThemePreference = colorThemePreferenceNamed(colorThemePreferenceName),
                themedTextAndIcons = themedTextAndIcons,
                colorThemeStrength = colorThemeStrength,
                paperTextureStrength = if (paperTextureEnabled) paperTextureStrength else 0
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
            showNativeToast("App Lock is off because this phone has no screen lock.")
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
            when (val result = systemUnlockPrompt.ask(title = "Unlock Emberr")) {
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
