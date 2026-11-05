package com.emberr.domain.applock

import com.emberr.data.local.prefs.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

enum class AutoLockDelay(val waitBeforeLocking: Duration) {
    IMMEDIATELY(Duration.ZERO),
    ONE_MINUTE(1.minutes),
    FIVE_MINUTES(5.minutes),
    FIFTEEN_MINUTES(15.minutes),
    ONE_HOUR(1.hours);

    companion object {
        fun named(name: String): AutoLockDelay = entries.firstOrNull { it.name == name } ?: ONE_MINUTE
    }
}

val shortestWaitAfterOwnScreen: Duration = 5.minutes

class AppLockManager(
    private val settingsManager: SettingsManager,
    private val timeSinceDeviceStarted: () -> Duration
) {
    private val mutableIsEnabled = MutableStateFlow(settingsManager.isAppLockEnabled())
    val isEnabled: StateFlow<Boolean> = mutableIsEnabled.asStateFlow()

    private val mutableAutoLockDelay = MutableStateFlow(AutoLockDelay.named(settingsManager.getAppLockDelay()))
    val autoLockDelay: StateFlow<AutoLockDelay> = mutableAutoLockDelay.asStateFlow()

    private val mutableIsScreenSecurityOn = MutableStateFlow(settingsManager.isScreenSecurityEnabled())
    val isScreenSecurityOn: StateFlow<Boolean> = mutableIsScreenSecurityOn.asStateFlow()

    private val mutableIsLocked = MutableStateFlow(mutableIsEnabled.value)
    val isLocked: StateFlow<Boolean> = mutableIsLocked.asStateFlow()

    private var leftAppAt: Duration? = null
    private var cameBackFromOwnScreen = false

    fun onAppLeft() {
        if (mutableIsEnabled.value && !mutableIsLocked.value) {
            leftAppAt = timeSinceDeviceStarted()
        }
    }

    fun onReturnedFromOwnScreen() {
        cameBackFromOwnScreen = true
    }

    fun onAppReturned() {
        val leftAt = leftAppAt
        val wasOwnScreen = cameBackFromOwnScreen
        leftAppAt = null
        cameBackFromOwnScreen = false
        if (leftAt == null || !mutableIsEnabled.value) return

        val chosenWait = mutableAutoLockDelay.value.waitBeforeLocking
        val allowedTimeAway = if (wasOwnScreen) maxOf(chosenWait, shortestWaitAfterOwnScreen) else chosenWait
        if (timeSinceDeviceStarted() - leftAt >= allowedTimeAway) {
            mutableIsLocked.value = true
        }
    }

    fun unlock() {
        mutableIsLocked.value = false
        leftAppAt = null
    }

    fun turnOn() {
        settingsManager.saveAppLockEnabled(true)
        mutableIsEnabled.value = true
    }

    fun turnOff() {
        settingsManager.saveAppLockEnabled(false)
        mutableIsEnabled.value = false
        unlock()
    }

    fun changeAutoLockDelay(delay: AutoLockDelay) {
        settingsManager.saveAppLockDelay(delay.name)
        mutableAutoLockDelay.value = delay
    }

    fun changeScreenSecurity(isOn: Boolean) {
        settingsManager.saveScreenSecurityEnabled(isOn)
        mutableIsScreenSecurityOn.value = isOn
    }
}
