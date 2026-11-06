// Watches the note list so a renamed, edited or deleted note updates its card while the app runs.
package com.emberr.presentation.widget.notecard

import android.content.Context
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.WidgetNoteSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val cardSettleDelayMillis = 150L

class NoteCardWidgetCoordinator(
    private val context: Context,
    private val noteSource: WidgetNoteSource
) {
    private val coordinatorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    fun start() {
        coordinatorScope.launch {
            try {
                noteSource.observeSelectableNotes()
                    .distinctUntilChanged()
                    .debounce(cardSettleDelayMillis)
                    .collect { refreshNoteCardWidgets(context) }
            } catch (cause: Exception) {
                WidgetLog.e("Stopped watching notes for the card widgets", cause)
            }
        }
    }
}
