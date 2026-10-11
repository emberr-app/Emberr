package com.emberr.presentation.settings.importer

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.dao.CalendarEventExceptionDao
import com.emberr.data.local.room.dao.CalendarTaskDao
import com.emberr.data.local.room.dao.CanvasDao
import com.emberr.data.local.room.dao.NoteDao
import com.emberr.data.local.room.dao.SpaceDao
import com.emberr.data.local.room.entity.DEFAULT_SPACE_ID
import com.emberr.domain.ai.chat.ChatSessionRepository
import com.emberr.domain.canvas.CanvasRepository
import com.emberr.domain.importer.ImportFileReader
import com.emberr.domain.importer.MarkdownNotesImporter
import com.emberr.domain.importer.PickedImportFiles
import com.emberr.domain.reminders.ReminderRescheduler
import com.emberr.domain.reminders.ReminderScheduler
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.space.ActiveSpaceStore
import com.emberr.domain.space.SpaceRepository
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.domain.util.media.WebImageFetcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.io.IOException
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

    private val fileReader = ControlledFileReader()

    @BeforeTest
    fun useTestMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun aFailedReadShowsCouldNotRead() {
        val viewModel = importViewModel()

        viewModel.readPickedFiles(listOf("broken.md"))
        fileReader.pendingReads[0].completeExceptionally(IOException("provider failed"))

        assertEquals(PickedFilesState.CouldNotRead, viewModel.pickedFilesState.value)
    }

    @Test
    fun aFailedOlderReadDoesNotReplaceANewerRead() {
        val viewModel = importViewModel()

        viewModel.readPickedFiles(listOf("first.md"))
        viewModel.readPickedFiles(listOf("second.md"))
        fileReader.pendingReads[0].completeExceptionally(IOException("provider failed"))

        assertEquals(PickedFilesState.Reading, viewModel.pickedFilesState.value)

        fileReader.pendingReads[1].complete(
            PickedImportFiles(pickedNames = listOf("second.md"), files = emptyList(), skippedNames = emptyList()) {}
        )

        val state = assertIs<PickedFilesState.Ready>(viewModel.pickedFilesState.value)
        assertEquals(listOf("second.md"), state.summary.pickedNames)
    }

    @Test
    fun closingTheSheetWhileReadingStopsTheRead() {
        val viewModel = importViewModel()

        viewModel.readPickedFiles(listOf("vault.zip"))
        viewModel.closeMarkdownImport()

        assertEquals(1, fileReader.stoppedReadCount)
        assertEquals(PickedFilesState.NothingPicked, viewModel.pickedFilesState.value)
    }

    private class ControlledFileReader : ImportFileReader {
        val pendingReads = mutableListOf<CompletableDeferred<PickedImportFiles>>()
        var stoppedReadCount = 0

        override suspend fun readPickedFiles(pickedPaths: List<String>): PickedImportFiles {
            val pendingRead = CompletableDeferred<PickedImportFiles>().also { pendingReads += it }
            try {
                return pendingRead.await()
            } catch (cancellation: CancellationException) {
                stoppedReadCount++
                throw cancellation
            }
        }
    }

    private fun importViewModel(): ImportViewModel {
        val settings: SettingsManager = stubOf { methodName ->
            when (methodName) {
                "getActiveSpaceId" -> DEFAULT_SPACE_ID
                "getActiveSpaceIdFlow" -> flowOf(DEFAULT_SPACE_ID)
                else -> error("These tests do not expect SettingsManager.$methodName to be called")
            }
        }
        val spaceDao: SpaceDao = stubOf { methodName ->
            when (methodName) {
                "getAllSpaces" -> flowOf(emptyList<Any>())
                else -> error("These tests do not expect SpaceDao.$methodName to be called")
            }
        }
        val activeSpaceStore = ActiveSpaceStore(settings)
        val noteRepository: NoteRepository = neverCalledStub()
        val noteDao: NoteDao = neverCalledStub()

        return ImportViewModel(
            spaceRepository = SpaceRepository(
                spaceDao = spaceDao,
                activeSpaceStore = activeSpaceStore,
                noteRepository = noteRepository,
                chatSessionRepository = neverCalledStub<ChatSessionRepository>()
            ),
            activeSpaceStore = activeSpaceStore,
            importFileReader = fileReader,
            importFolderReader = null,
            markdownNotesImporter = MarkdownNotesImporter(
                repository = noteRepository,
                canvasRepository = CanvasRepository(canvasDao = neverCalledStub<CanvasDao>(), noteDao = noteDao),
                mediaStorageHelper = neverCalledStub<MediaStorageHelper>(),
                webImageFetcher = neverCalledStub<WebImageFetcher>(),
                reminderRescheduler = ReminderRescheduler(
                    calendarTaskDao = neverCalledStub<CalendarTaskDao>(),
                    calendarEventExceptionDao = neverCalledStub<CalendarEventExceptionDao>(),
                    noteDao = noteDao,
                    reminderScheduler = neverCalledStub<ReminderScheduler>()
                ),
                appScope = CoroutineScope(Job())
            )
        )
    }

    private inline fun <reified T : Any> neverCalledStub(): T =
        stubOf { methodName -> error("These tests do not expect ${T::class.simpleName}.$methodName to be called") }

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
