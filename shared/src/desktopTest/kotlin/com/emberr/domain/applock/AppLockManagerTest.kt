package com.emberr.domain.applock

import com.emberr.data.local.prefs.SettingsManager
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AppLockManagerTest {

    private var now = Duration.ZERO

    private val settings: SettingsManager = stubOf { methodName ->
        when (methodName) {
            "isAppLockEnabled" -> true
            "getAppLockDelay" -> AutoLockDelay.ONE_MINUTE.name
            "isScreenSecurityEnabled" -> false
            else -> null
        }
    }

    private fun unlockedManager() = AppLockManager(settings, timeSinceDeviceStarted = { now }).apply { unlock() }

    @Test
    fun locksOnlyAfterBeingAwayForTheChosenDelay() {
        val manager = unlockedManager()

        manager.onAppLeft()
        now += 59.seconds
        manager.onAppReturned()
        assertFalse(manager.isLocked.value)

        manager.onAppLeft()
        now += 1.minutes
        manager.onAppReturned()
        assertTrue(manager.isLocked.value)
    }

    @Test
    fun returningFromEmberrPickerGetsFiveMinutesEvenWhenSetToImmediately() {
        val manager = unlockedManager()
        manager.changeAutoLockDelay(AutoLockDelay.IMMEDIATELY)

        manager.onAppLeft()
        now += 4.minutes
        manager.onReturnedFromEmberrPicker()
        manager.onAppReturned()
        assertFalse(manager.isLocked.value)

        manager.onAppLeft()
        now += 1.seconds
        manager.onAppReturned()
        assertTrue(manager.isLocked.value)
    }

    private inline fun <reified T : Any> stubOf(crossinline answer: (methodName: String) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { stub, method, arguments ->
            when (method.name) {
                "toString" -> "stub of ${T::class.simpleName}"
                "hashCode" -> System.identityHashCode(stub)
                "equals" -> stub === arguments?.firstOrNull()
                else -> answer(method.name)
            }
        } as T
}
