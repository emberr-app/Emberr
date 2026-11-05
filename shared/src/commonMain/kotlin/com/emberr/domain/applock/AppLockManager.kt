package com.emberr.domain.applock

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.prefs.SyncConstants
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
        fun named(name: String): AutoLockDelay =
            entries.firstOrNull { it.name == name } ?: valueOf(SyncConstants.DEFAULT_APP_LOCK_DELAY)
    }
}

private val shortestWaitAfterEmberrPicker: Duration = 5.minutes

class AppLockManager(
    private val settingsManager: SettingsManager,
    private val timeSinceDeviceStarted: () -> Duration
) {
    private val mutableIsAppLockOn = MutableStateFlow(settingsManager.isAppLockEnabled())
    val isAppLockOn: StateFlow<Boolean> = mutableIsAppLockOn.asStateFlow()

    private val mutableAutoLockDelay = MutableStateFlow(AutoLockDelay.named(settingsManager.getAppLockDelay()))
    val autoLockDelay: StateFlow<AutoLockDelay> = mutableAutoLockDelay.asStateFlow()

    private val mutableIsScreenSecurityOn = MutableStateFlow(settingsManager.isScreenSecurityEnabled())
    val isScreenSecurityOn: StateFlow<Boolean> = mutableIsScreenSecurityOn.asStateFlow()

    private val mutableIsLocked = MutableStateFlow(mutableIsAppLockOn.value)
    val isLocked: StateFlow<Boolean> = mutableIsLocked.asStateFlow()

    private var leftAppAt: Duration? = null
    private var cameBackFromEmberrPicker = false

    fun onAppLeft() {
        if (mutableIsAppLockOn.value && !mutableIsLocked.value) {
            leftAppAt = timeSinceDeviceStarted()
        }
    }

    fun onReturnedFromEmberrPicker() {
        cameBackFromEmberrPicker = true
    }

    fun onAppReturned() {
        val leftAt = leftAppAt
        val wasEmberrPicker = cameBackFromEmberrPicker
        leftAppAt = null
        cameBackFromEmberrPicker = false
        if (leftAt == null) return

        val chosenWait = mutableAutoLockDelay.value.waitBeforeLocking
        val allowedTimeAway = if (wasEmberrPicker) maxOf(chosenWait, shortestWaitAfterEmberrPicker) else chosenWait
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
        mutableIsAppLockOn.value = true
    }

    fun turnOff() {
        settingsManager.saveAppLockEnabled(false)
        mutableIsAppLockOn.value = false
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
