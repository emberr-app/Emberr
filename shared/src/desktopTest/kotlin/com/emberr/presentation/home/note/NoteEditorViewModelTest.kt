package com.emberr.presentation.home.note

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.data.local.room.dao.CanvasDao
import com.emberr.data.local.room.dao.NoteDao
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.canvas.CanvasRepository
import com.emberr.domain.model.NoteContent
import com.emberr.domain.movie.MovieTitleSource
import com.emberr.domain.people.PeopleRepository
import com.emberr.domain.reminders.ReminderScheduler
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.table.DeviceTableLayoutStore
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.domain.util.media.WebImageFetcher
import com.emberr.domain.util.voice.AudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import java.lang.reflect.Proxy
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class NoteEditorViewModelTest {

    private val storedMetadata = MutableStateFlow(
        NoteMetadataEntity(
            noteId = "note-1",
            title = "Original title",
            folderId = null,
            isDaily = false,
            dateString = null,
            createdAt = 1_000L,
            updatedAt = 1_000L,
            filePath = ""
        )
    )
    private val metadataSavedByEditor = MutableStateFlow<NoteMetadataEntity?>(null)
    private var noteWasDeleted = false

    @BeforeTest
    fun useTestMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun aNoteDeletedWhileItIsOpenIsNotSavedBackIntoTheActiveSpace() = runBlocking<Unit> {
        val viewModel = openedNote()
        noteWasDeleted = true

        viewModel.toggleWordCount()

        withTimeout(5_000) { viewModel.showWordCount.first { isShown -> !isShown } }
        assertNull(metadataSavedByEditor.value)
    }

    @Test
    fun renamingTheOpenNoteElsewhereShowsTheNewTitle() = runBlocking<Unit> {
        val viewModel = openedNote()

        renameNoteOutsideEditor("Renamed from home")

        withTimeout(5_000) { viewModel.noteTitle.first { it == "Renamed from home" } }
    }

    @Test
    fun theEditorsNextSaveKeepsTheTitleRenamedElsewhere() = runBlocking {
        val viewModel = openedNote()
        renameNoteOutsideEditor("Renamed from home")
        withTimeout(5_000) { viewModel.noteTitle.first { it == "Renamed from home" } }

        viewModel.toggleWordCount()

        val savedMetadata = withTimeout(5_000) { metadataSavedByEditor.first { it != null } }
        assertEquals("Renamed from home", savedMetadata?.title)
    }

    private suspend fun openedNote(): NoteEditorViewModel {
        val viewModel = NoteEditorViewModel(
            repository = noteRepositoryBackedByStoredMetadata(),
            mediaStorageHelper = neverCalledStub<MediaStorageHelper>(),
            webImageFetcher = neverCalledStub<WebImageFetcher>(),
            reminderScheduler = neverCalledStub<ReminderScheduler>(),
            audioRecorder = neverCalledStub<AudioRecorder>(),
            appScope = CoroutineScope(Job()),
            deviceTableLayoutStore = DeviceTableLayoutStore(neverCalledStub<SettingsManager>()),
            movieTitleSource = neverCalledStub<MovieTitleSource>(),
            peopleRepository = peopleRepositoryWithNobody(),
            canvasRepository = CanvasRepository(neverCalledStub<CanvasDao>(), neverCalledStub<NoteDao>())
        )
        viewModel.loadNote("note-1")
        withTimeout(5_000) { viewModel.isLoading.first { isLoading -> !isLoading } }
        return viewModel
    }

    private fun renameNoteOutsideEditor(newTitle: String) {
        val current = storedMetadata.value
        storedMetadata.value = current.copy(title = newTitle, titleUpdatedAt = current.updatedAt + 1, updatedAt = current.updatedAt + 1)
    }

    private fun noteRepositoryBackedByStoredMetadata(): NoteRepository =
        Proxy.newProxyInstance(NoteRepository::class.java.classLoader, arrayOf(NoteRepository::class.java)) { stub, method, arguments ->
            when (method.name) {
                "toString" -> "stub of NoteRepository"
                "hashCode" -> System.identityHashCode(stub)
                "equals" -> stub === arguments?.firstOrNull()
                "getNoteById" -> if (noteWasDeleted) null else storedMetadata.value
                "getNoteContent" -> NoteContent(blocks = emptyList())
                "observeNoteMetadata" -> storedMetadata
                "saveNote" -> {
                    val savedMetadata = arguments!![0] as NoteMetadataEntity
                    storedMetadata.value = savedMetadata
                    metadataSavedByEditor.value = savedMetadata
                    Unit
                }
                else ->
                    if (Flow::class.java.isAssignableFrom(method.returnType)) emptyFlow<Any>()
                    else error("These tests do not expect NoteRepository.${method.name} to be called")
            }
        } as NoteRepository

    private fun peopleRepositoryWithNobody(): PeopleRepository =
        Proxy.newProxyInstance(PeopleRepository::class.java.classLoader, arrayOf(PeopleRepository::class.java)) { stub, method, arguments ->
            when (method.name) {
                "toString" -> "stub of PeopleRepository"
                "hashCode" -> System.identityHashCode(stub)
                "equals" -> stub === arguments?.firstOrNull()
                "observePeople" -> flowOf(emptyList<NoteMetadataEntity>())
                else -> error("These tests do not expect PeopleRepository.${method.name} to be called")
            }
        } as PeopleRepository

    private inline fun <reified T : Any> neverCalledStub(): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { stub, method, arguments ->
            when (method.name) {
                "toString" -> "stub of ${T::class.simpleName}"
                "hashCode" -> System.identityHashCode(stub)
                "equals" -> stub === arguments?.firstOrNull()
                else -> error("These tests do not expect ${T::class.simpleName}.${method.name} to be called")
            }
        } as T
}
