package com.emberr.presentation.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import com.emberr.domain.util.system.StartupLog
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.awt.AWTEvent
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent

object SlowFrameReporter {

    private const val SLOW_FRAME_MILLIS = 50L
    private const val NANOS_PER_MILLI = 1_000_000L
    private const val IDLE_AFTER_LAST_INPUT_NANOS = 1_000L * NANOS_PER_MILLI
    private const val WATCHED_INPUT_EVENTS =
        AWTEvent.MOUSE_WHEEL_EVENT_MASK or AWTEvent.MOUSE_EVENT_MASK or AWTEvent.KEY_EVENT_MASK

    @Volatile
    var currentScreenName: String = "Unknown"

    @Composable
    fun ReportSlowFramesDuringInput() {
        LaunchedEffect(Unit) {
            val inputs = Channel<String>(Channel.CONFLATED)
            val listener = AWTEventListener { event -> describeInput(event)?.let { input -> inputs.trySend(input) } }
            Toolkit.getDefaultToolkit().addAWTEventListener(listener, WATCHED_INPUT_EVENTS)
            try {
                for (firstInput in inputs) {
                    measureFramesUntilInputStops(firstInput, inputs)
                }
            } finally {
                Toolkit.getDefaultToolkit().removeAWTEventListener(listener)
            }
        }
    }

    private suspend fun measureFramesUntilInputStops(firstInput: String, inputs: ReceiveChannel<String>) {
        val screenName = currentScreenName
        var lastInputAt = System.nanoTime()
        var previousFrameAt = withFrameNanos { frameTime -> frameTime }
        var frameCount = 0
        var slowFrameCount = 0
        var slowestFrameMillis = 0L

        while (System.nanoTime() - lastInputAt < IDLE_AFTER_LAST_INPUT_NANOS) {
            val frameAt = withFrameNanos { frameTime -> frameTime }
            val frameMillis = (frameAt - previousFrameAt) / NANOS_PER_MILLI
            previousFrameAt = frameAt
            frameCount++
            if (frameMillis > SLOW_FRAME_MILLIS) slowFrameCount++
            if (frameMillis > slowestFrameMillis) slowestFrameMillis = frameMillis
            if (inputs.tryReceive().isSuccess) lastInputAt = System.nanoTime()
        }

        if (slowFrameCount > 0) {
            StartupLog.step(
                "Slow frames while $firstInput on $screenName: $slowFrameCount of $frameCount frames " +
                    "took over $SLOW_FRAME_MILLIS ms, slowest $slowestFrameMillis ms"
            )
        }
    }

    private fun describeInput(event: AWTEvent): String? = when {
        event is MouseWheelEvent -> "scrolling"
        event is MouseEvent && event.id == MouseEvent.MOUSE_PRESSED -> "clicking"
        event is KeyEvent && event.id == KeyEvent.KEY_PRESSED -> "typing"
        else -> null
    }
}
