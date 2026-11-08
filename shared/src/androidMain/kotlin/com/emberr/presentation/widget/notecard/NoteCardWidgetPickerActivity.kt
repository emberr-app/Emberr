// The setup screen opened when the card widget is added, for choosing which note it shows.
package com.emberr.presentation.widget.notecard

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.emberr.presentation.widget.WidgetLog
import com.emberr.presentation.widget.WidgetNoteChooser
import com.emberr.presentation.widget.WidgetNoteSource
import com.emberr.presentation.widget.WidgetSetupNotice
import com.emberr.data.local.prefs.SettingsManager
import com.emberr.ui.theme.EmberrTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get

class NoteCardWidgetPickerActivity : ComponentActivity() {

    private val noteSource: WidgetNoteSource? by lazy {
        runCatching { get<WidgetNoteSource>() }.getOrNull()
    }

    private val contentReader: NoteCardWidgetContentReader? by lazy {
        runCatching { get<NoteCardWidgetContentReader>() }.getOrNull()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED, resultIntentFor(appWidgetId))

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        enableEdgeToEdge()

        val interfaceScalePercent = get<SettingsManager>().getInterfaceScalePercent()
        setContent {
            EmberrTheme(interfaceScalePercent = interfaceScalePercent) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val source = noteSource
                    if (source == null) {
                        WidgetSetupNotice("Open the app once before adding a widget")
                    } else {
                        val notesFlow = remember { source.observeSelectableNotes() }
                        val notes by notesFlow.collectAsState(initial = emptyList())

                        WidgetNoteChooser(
                            notes = notes,
                            onNoteChosen = { noteId -> applySelection(appWidgetId, noteId) },
                            onClose = { finish() }
                        )
                    }
                }
            }
        }
    }

    private fun applySelection(appWidgetId: Int, noteId: String) {
        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val glanceId = GlanceAppWidgetManager(this@NoteCardWidgetPickerActivity)
                    .getGlanceIdBy(appWidgetId)

                updateAppWidgetState(
                    context = this@NoteCardWidgetPickerActivity,
                    definition = PreferencesGlanceStateDefinition,
                    glanceId = glanceId
                ) { preferences ->
                    preferences.toMutablePreferences().apply {
                        this[cardNoteIdKey] = noteId
                        remove(cardNoteDeletedKey)
                    }
                }

                contentReader?.readContentOnce(noteId)?.let { content ->
                    writeCachedCard(this@NoteCardWidgetPickerActivity, glanceId, content)
                }

                NoteCardWidget().update(this@NoteCardWidgetPickerActivity, glanceId)
                withContext(Dispatchers.Main) { setResult(RESULT_OK, resultIntentFor(appWidgetId)) }
            } catch (cause: Exception) {
                WidgetLog.e("Could not attach note $noteId to the card widget", cause)
                withContext(Dispatchers.Main) {
                    setResult(RESULT_CANCELED, resultIntentFor(appWidgetId))
                }
            }
            withContext(Dispatchers.Main) { finish() }
        }
    }

    private fun resultIntentFor(appWidgetId: Int): Intent =
        Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
