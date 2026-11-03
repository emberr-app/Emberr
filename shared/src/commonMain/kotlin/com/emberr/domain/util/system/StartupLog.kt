package com.emberr.domain.util.system

import kotlin.time.TimeSource

expect object StartupLog {
    fun step(message: String)
}

inline fun <T> timeStartupStep(stepName: String, step: () -> T): T {
    StartupLog.step("$stepName started")
    val startedAt = TimeSource.Monotonic.markNow()
    try {
        return step()
    } finally {
        StartupLog.step("$stepName ended after ${startedAt.elapsedNow().inWholeMilliseconds} ms")
    }
}
