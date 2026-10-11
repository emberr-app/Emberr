package com.emberr.presentation.settings.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.importer.ImportFileReader
import com.emberr.domain.importer.ImportFolderReader
import com.emberr.domain.importer.ImportProgress
import com.emberr.domain.importer.ImportSummary
import com.emberr.domain.importer.MarkdownNotesImporter
import com.emberr.domain.importer.PickedImportFiles
import com.emberr.domain.space.ActiveSpaceStore
import com.emberr.domain.space.SpaceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PickedFilesState {
    data object NothingPicked : PickedFilesState
    data object Reading : PickedFilesState
    data object CouldNotRead : PickedFilesState
    data class Ready(val summary: ImportSummary) : PickedFilesState
}

class ImportViewModel(
    spaceRepository: SpaceRepository,
    private val activeSpaceStore: ActiveSpaceStore,
    private val importFileReader: ImportFileReader,
    private val importFolderReader: ImportFolderReader?,
    private val markdownNotesImporter: MarkdownNotesImporter
) : ViewModel() {

    val spaces: StateFlow<List<SpaceEntity>> = spaceRepository.observeSpaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedSpaceId = MutableStateFlow(activeSpaceStore.currentActiveSpaceId())
    val selectedSpaceId: StateFlow<String> = _selectedSpaceId.asStateFlow()

    private val _pickedFilesState = MutableStateFlow<PickedFilesState>(PickedFilesState.NothingPicked)
    val pickedFilesState: StateFlow<PickedFilesState> = _pickedFilesState.asStateFlow()

    val importProgress: StateFlow<ImportProgress> = markdownNotesImporter.progress

    private var pickedImportFiles: PickedImportFiles? = null
    private var readingJob: Job? = null
    private var latestReadNumber = 0

    fun prepareMarkdownImport() {
        _selectedSpaceId.value = activeSpaceStore.currentActiveSpaceId()
    }

    fun selectSpace(spaceId: String) {
        _selectedSpaceId.value = spaceId
    }

    fun readPickedFiles(pickedPaths: List<String>) {
        if (pickedPaths.isEmpty()) return
        readPicked { importFileReader.readPickedFiles(pickedPaths) }
    }

    fun readPickedFolder(folderLocation: String) {
        val folderReader = importFolderReader ?: return
        readPicked { folderReader.readPickedFolder(folderLocation) }
    }

    private fun readPicked(read: suspend () -> PickedImportFiles) {
        releasePickedFiles()
        val thisReadNumber = ++latestReadNumber
        _pickedFilesState.value = PickedFilesState.Reading
        readingJob = viewModelScope.launch {
            val pickedFiles = try {
                read()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (thisReadNumber == latestReadNumber) _pickedFilesState.value = PickedFilesState.CouldNotRead
                return@launch
            }
            if (thisReadNumber != latestReadNumber) {
                pickedFiles.close()
                return@launch
            }
            pickedImportFiles = pickedFiles
            _pickedFilesState.value = PickedFilesState.Ready(ImportSummary.of(pickedFiles))
        }
    }

    fun startImport() {
        val pickedFiles = pickedImportFiles ?: return
        pickedImportFiles = null
        _pickedFilesState.value = PickedFilesState.NothingPicked
        markdownNotesImporter.startImport(pickedFiles, _selectedSpaceId.value)
    }

    fun stopImport() {
        markdownNotesImporter.stopImport()
    }

    fun closeMarkdownImport() {
        latestReadNumber++
        releasePickedFiles()
        markdownNotesImporter.clearFinishedImport()
    }

    private fun releasePickedFiles() {
        readingJob?.cancel()
        pickedImportFiles?.close()
        pickedImportFiles = null
        _pickedFilesState.value = PickedFilesState.NothingPicked
    }

    override fun onCleared() {
        pickedImportFiles?.close()
    }
}
