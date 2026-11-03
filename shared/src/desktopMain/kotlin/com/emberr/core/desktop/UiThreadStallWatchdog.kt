package com.emberr.core.desktop

import com.emberr.domain.util.system.StartupLog
import java.lang.management.ManagementFactory
import java.lang.management.ThreadInfo
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

object UiThreadStallWatchdog {

    private const val PAUSE_BETWEEN_CHECKS_MILLIS = 500L
    private const val STALL_THRESHOLD_MILLIS = 2_000L
    private const val REPORT_AGAIN_EVERY_MILLIS = 5_000L
    private const val WATCH_LENGTH_MILLIS = 5 * 60_000L
    private const val FRAMES_SHOWN_FOR_OTHER_BUSY_THREADS = 12
    private const val UI_THREAD_NAME_PREFIX = "AWT-EventQueue"
    private const val WATCHDOG_THREAD_NAME = "emberr-ui-stall-watchdog"

    private val stateLock = Any()
    private var watchEndsAtMillis = 0L
    private var isWatching = false

    fun watchForTheNextFewMinutes() {
        synchronized(stateLock) {
            watchEndsAtMillis = System.currentTimeMillis() + WATCH_LENGTH_MILLIS
            if (isWatching) return
            isWatching = true
        }
        Thread(::watchUntilTimeIsUp, WATCHDOG_THREAD_NAME).apply {
            isDaemon = true
            start()
        }
    }

    private fun watchUntilTimeIsUp() {
        while (isStillWatching()) {
            checkUiThreadOnce()
        }
    }

    private fun isStillWatching(): Boolean = synchronized(stateLock) {
        isWatching = isWatchWindowOpen()
        isWatching
    }

    private fun isWatchWindowOpen(): Boolean = synchronized(stateLock) {
        System.currentTimeMillis() < watchEndsAtMillis
    }

    private fun checkUiThreadOnce() {
        val uiThreadAnswered = CountDownLatch(1)
        SwingUtilities.invokeLater { uiThreadAnswered.countDown() }
        if (uiThreadAnswered.await(STALL_THRESHOLD_MILLIS, TimeUnit.MILLISECONDS)) {
            Thread.sleep(PAUSE_BETWEEN_CHECKS_MILLIS)
            return
        }

        val stallStartedAtMillis = System.currentTimeMillis() - STALL_THRESHOLD_MILLIS
        reportStall(stallStartedAtMillis)
        while (!uiThreadAnswered.await(REPORT_AGAIN_EVERY_MILLIS, TimeUnit.MILLISECONDS) && isWatchWindowOpen()) {
            reportStall(stallStartedAtMillis)
        }

        if (uiThreadAnswered.count == 0L) {
            StartupLog.step("UI thread answered again after ${secondsSince(stallStartedAtMillis)} s")
        } else {
            StartupLog.step("Stopped watching while the UI thread was still stuck after ${secondsSince(stallStartedAtMillis)} s")
        }
    }

    private fun reportStall(stallStartedAtMillis: Long) {
        val allThreads = ManagementFactory.getThreadMXBean().dumpAllThreads(false, false)
        val uiThreads = allThreads.filter { thread -> thread.threadName.startsWith(UI_THREAD_NAME_PREFIX) }
        val otherBusyThreads = allThreads.filter { thread ->
            thread.threadState == Thread.State.RUNNABLE &&
                !thread.threadName.startsWith(UI_THREAD_NAME_PREFIX) &&
                thread.threadName != WATCHDOG_THREAD_NAME
        }

        val report = buildString {
            append("UI thread stuck for ${secondsSince(stallStartedAtMillis)} s")
            uiThreads.forEach { thread -> appendThread(thread, framesToShow = thread.stackTrace.size) }
            otherBusyThreads.forEach { thread -> appendThread(thread, framesToShow = FRAMES_SHOWN_FOR_OTHER_BUSY_THREADS) }
        }
        StartupLog.step(report)
    }

    private fun StringBuilder.appendThread(thread: ThreadInfo, framesToShow: Int) {
        append("\n    Thread \"${thread.threadName}\" ${thread.threadState}")
        thread.lockName?.let { lockName -> append(" waiting for $lockName") }
        thread.lockOwnerName?.let { ownerName -> append(" held by \"$ownerName\"") }
        thread.stackTrace.take(framesToShow).forEach { frame -> append("\n        at $frame") }
    }

    private fun secondsSince(startMillis: Long): Long = (System.currentTimeMillis() - startMillis) / 1000
}
