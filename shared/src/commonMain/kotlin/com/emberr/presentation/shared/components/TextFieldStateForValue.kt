package com.emberr.presentation.shared.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.text.TextRange

internal class TextFieldStateForValue(
    val state: TextFieldState,
    val reportTypedTextNow: () -> Unit
)

@Composable
internal fun rememberTextFieldStateFor(value: String, onValueChange: (String) -> Unit): TextFieldStateForValue {
    val state = remember { TextFieldState(value) }
    val latestValue = rememberUpdatedState(value)
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val reportedTexts = remember { mutableListOf<String>() }

    fun isAlreadyKnown(text: String): Boolean = text == latestValue.value || text == reportedTexts.lastOrNull()

    LaunchedEffect(state) {
        snapshotFlow { latestValue.value }.collect { newValue ->
            val echoIndex = reportedTexts.indexOf(newValue)
            if (echoIndex >= 0) {
                repeat(echoIndex + 1) { reportedTexts.removeAt(0) }
                return@collect
            }
            reportedTexts.clear()
            if (newValue != state.text.toString()) state.setTextAndPlaceCursorAtEnd(newValue)
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { typedText ->
            if (isAlreadyKnown(typedText)) return@collect
            val valueBeforeTyping = latestValue.value
            reportedTexts += typedText
            latestOnValueChange(typedText)
            withFrameNanos { }
            withFrameNanos { }
            val callerKeptOldValue = latestValue.value == valueBeforeTyping && state.text.toString() == typedText
            if (callerKeptOldValue) {
                reportedTexts.remove(typedText)
                state.replaceTextKeepingCursor(valueBeforeTyping)
            }
        }
    }

    return remember(state) {
        TextFieldStateForValue(
            state = state,
            reportTypedTextNow = {
                val typedText = state.text.toString()
                if (!isAlreadyKnown(typedText)) {
                    reportedTexts += typedText
                    latestOnValueChange(typedText)
                }
            }
        )
    }
}

private fun TextFieldState.replaceTextKeepingCursor(newText: String) {
    edit {
        val cursor = selection.end.coerceAtMost(newText.length)
        replace(0, length, newText)
        selection = TextRange(cursor)
    }
}
