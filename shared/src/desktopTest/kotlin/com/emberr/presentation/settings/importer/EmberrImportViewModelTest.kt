package com.emberr.presentation.settings.importer

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.emberr.data.local.room.AppDatabase
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.backup.manual.BackupMediaCopyOutcome
import com.emberr.domain.backup.manual.EmberrBackupData
import com.emberr.domain.backup.manual.EmberrBackupFile
import com.emberr.domain.backup.manual.EmberrBackupFileReader
import com.emberr.domain.backup.manual.EmberrBackupImporter
import com.emberr.domain.reminders.ReminderRescheduler
import com.emberr.domain.reminders.ReminderScheduler
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.util.media.MediaStorageHelper
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EmberrImportViewModelTest {

    private val appDatabase = Room.inMemoryDatabaseBuilder<AppDatabase>()
        .setDriver(BundledSQLiteDriver())
        .build()
    private val backupFileReader = ControlledBackupFileReader()

    @BeforeTest
    fun useTestMainDispatcher() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun restoreMainDispatcherAndCloseDatabase() {
        Dispatchers.resetMain()
        appDatabase.close()
    }

    private class ControlledBackupFileReader : EmberrBackupFileReader {
        val pendingReads = mutableListOf<CompletableDeferred<EmberrBackupFile>>()

        override suspend fun readBackupFile(location: String): EmberrBackupFile {
            val read = CompletableDeferred<EmberrBackupFile>()
            pendingReads += read
            return read.await()
        }
    }

    private class FakeBackupFile(spaces: List<SpaceEntity>) : EmberrBackupFile {
        override val pickedName = "backup.emberr"
        override val backupData = EmberrBackupData(exportTimestamp = 1L, spaces = spaces)
        var isClosed = false

        override suspend fun copyMediaFileIfMissing(fileName: String) = BackupMediaCopyOutcome.NOT_IN_BACKUP

        override fun close() {
            isClosed = true
        }
    }

    private fun space(spaceId: String, isDeleted: Boolean = false) =
        SpaceEntity(spaceId = spaceId, displayName = spaceId, createdAt = 1L, isDeleted = isDeleted)

    private fun viewModel() = EmberrImportViewModel(
        backupFileReader = backupFileReader,
        emberrBackupImporter = EmberrBackupImporter(
            appDatabase = appDatabase,
            repository = stubOf<NoteRepository>(),
            mediaStorageHelper = stubOf<MediaStorageHelper>(),
            reminderRescheduler = ReminderRescheduler(
                calendarTaskDao = appDatabase.calendarTaskDao(),
                calendarEventExceptionDao = appDatabase.calendarEventExceptionDao(),
                noteDao = appDatabase.noteDao(),
                reminderScheduler = stubOf<ReminderScheduler>()
            ),
            appScope = CoroutineScope(Job())
        )
    )

    @Test
    fun aReadBackupShowsOnlyItsLiveSpaces() {
        val viewModel = viewModel()

        viewModel.readBackupFile("backup.emberr")
        backupFileReader.pendingReads[0].complete(FakeBackupFile(listOf(space("Work"), space("Old", isDeleted = true))))

        val state = assertIs<PickedBackupState.Ready>(viewModel.pickedBackupState.value)
        assertEquals(listOf("Work"), state.spaces.map { it.displayName })
    }

    @Test
    fun aFailedReadShowsTheReason() {
        val viewModel = viewModel()

        viewModel.readBackupFile("notes.txt")
        backupFileReader.pendingReads[0].completeExceptionally(IllegalStateException("This file is not an Emberr backup."))

        assertEquals(PickedBackupState.CouldNotRead("This file is not an Emberr backup."), viewModel.pickedBackupState.value)
    }

    @Test
    fun anOlderReadThatFinishesLateIsClosedAndIgnored() {
        val viewModel = viewModel()
        val olderBackup = FakeBackupFile(listOf(space("Older")))

        viewModel.readBackupFile("first.emberr")
        viewModel.readBackupFile("second.emberr")
        backupFileReader.pendingReads[1].complete(FakeBackupFile(listOf(space("Newer"))))
        backupFileReader.pendingReads[0].complete(olderBackup)

        val state = assertIs<PickedBackupState.Ready>(viewModel.pickedBackupState.value)
        assertEquals(listOf("Newer"), state.spaces.map { it.displayName })
        assertTrue(olderBackup.isClosed)
    }

    @Test
    fun closingTheSheetClosesThePickedBackup() {
        val viewModel = viewModel()
        val backupFile = FakeBackupFile(listOf(space("Work")))
        viewModel.readBackupFile("backup.emberr")
        backupFileReader.pendingReads[0].complete(backupFile)

        viewModel.closeEmberrImport()

        assertTrue(backupFile.isClosed)
        assertEquals(PickedBackupState.NothingPicked, viewModel.pickedBackupState.value)
    }

    @Test
    fun aReadFailureOnAnotherAttemptDoesNotHideANewerReadThatIsStillGoing() {
        val viewModel = viewModel()

        viewModel.readBackupFile("first.emberr")
        viewModel.readBackupFile("second.emberr")
        backupFileReader.pendingReads[0].completeExceptionally(IOException("provider failed"))

        assertEquals(PickedBackupState.Reading, viewModel.pickedBackupState.value)
    }

    private inline fun <reified T : Any> stubOf(): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { stub, method, arguments ->
            when (method.name) {
                "toString" -> "stub of ${T::class.simpleName}"
                "hashCode" -> System.identityHashCode(stub)
                "equals" -> stub === arguments?.firstOrNull()
                else -> error("These tests do not expect ${T::class.simpleName}.${method.name} to be called")
            }
        } as T
}
