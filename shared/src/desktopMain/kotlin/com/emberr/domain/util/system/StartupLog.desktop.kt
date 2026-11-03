package com.emberr.domain.util.system

import com.emberr.core.desktop.DesktopAppStorage
import java.io.File
import java.io.IOException
import java.io.Writer
import java.lang.management.ManagementFactory
import java.util.Locale

actual object StartupLog {

    private const val BYTES_PER_MEGABYTE = 1024 * 1024
    private const val MILLIS_PER_SECOND = 1000.0

    private val logFile = File(File(DesktopAppStorage.emberrDirectory, "logs"), "startup.log")
    private val writeLock = Any()
    private var writer: Writer? = null
    private var hasGivenUpWriting = false

    actual fun step(message: String) {
        val line = "${timeSinceLaunch()}  ${memorySummary()}  $message\n"
        synchronized(writeLock) {
            if (hasGivenUpWriting) return
            try {
                val openWriter = writer ?: openFreshLogFile().also { writer = it }
                openWriter.write(line)
                openWriter.flush()
            } catch (cause: IOException) {
                hasGivenUpWriting = true
            }
        }
    }

    private fun openFreshLogFile(): Writer {
        logFile.parentFile.mkdirs()
        return logFile.bufferedWriter()
    }

    private fun timeSinceLaunch(): String =
        String.format(Locale.ROOT, "%8.3fs", ManagementFactory.getRuntimeMXBean().uptime / MILLIS_PER_SECOND)

    private fun memorySummary(): String {
        val runtime = Runtime.getRuntime()
        val usedMegabytes = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_PER_MEGABYTE
        val maximumMegabytes = runtime.maxMemory() / BYTES_PER_MEGABYTE
        val garbageCollectionMillis = ManagementFactory.getGarbageCollectorMXBeans()
            .sumOf { collector -> collector.collectionTime.coerceAtLeast(0) }
        val garbageCollectionSeconds = String.format(Locale.ROOT, "%.1f", garbageCollectionMillis / MILLIS_PER_SECOND)
        return "heap $usedMegabytes/$maximumMegabytes MB  gc ${garbageCollectionSeconds}s"
    }
}
