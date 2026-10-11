package com.emberr.presentation.settings.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.backup.manual.EmberrBackupFile
import com.emberr.domain.backup.manual.EmberrBackupFileReader
import com.emberr.domain.backup.manual.EmberrBackupImporter
import com.emberr.domain.backup.manual.EmberrImportProgress
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PickedBackupState {
    data object NothingPicked : PickedBackupState
    data object Reading : PickedBackupState
    data class CouldNotRead(val reason: String) : PickedBackupState
    data class Ready(val pickedName: String, val spaces: List<SpaceEntity>) : PickedBackupState
}

class EmberrImportViewModel(
    private val backupFileReader: EmberrBackupFileReader,
    private val emberrBackupImporter: EmberrBackupImporter
) : ViewModel() {

    private val _pickedBackupState = MutableStateFlow<PickedBackupState>(PickedBackupState.NothingPicked)
    val pickedBackupState: StateFlow<PickedBackupState> = _pickedBackupState.asStateFlow()

    val importProgress: StateFlow<EmberrImportProgress> = emberrBackupImporter.progress

    private var pickedBackupFile: EmberrBackupFile? = null
    private var readingJob: Job? = null
    private var latestReadNumber = 0

    fun readBackupFile(location: String) {
        releasePickedBackup()
        val thisReadNumber = ++latestReadNumber
        _pickedBackupState.value = PickedBackupState.Reading
        readingJob = viewModelScope.launch {
            val backupFile = try {
                withContext(NonCancellable) { backupFileReader.readBackupFile(location) }
            } catch (error: Exception) {
                if (isActive && thisReadNumber == latestReadNumber) {
                    _pickedBackupState.value = PickedBackupState.CouldNotRead(error.message ?: "Unknown error")
                }
                return@launch
            }
            if (!isActive || thisReadNumber != latestReadNumber) {
                backupFile.close()
                return@launch
            }
            pickedBackupFile = backupFile
            _pickedBackupState.value = PickedBackupState.Ready(
                pickedName = backupFile.pickedName,
                spaces = backupFile.backupData.spaces.filterNot { it.isDeleted }
            )
        }
    }

    fun startImport(spaceIds: Set<String>) {
        val backupFile = pickedBackupFile ?: return
        pickedBackupFile = null
        _pickedBackupState.value = PickedBackupState.NothingPicked
        emberrBackupImporter.startImport(backupFile, spaceIds)
    }

    fun stopImport() {
        emberrBackupImporter.stopImport()
    }

    fun closeEmberrImport() {
        latestReadNumber++
        releasePickedBackup()
        emberrBackupImporter.clearFinishedImport()
    }

    private fun releasePickedBackup() {
        readingJob?.cancel()
        pickedBackupFile?.close()
        pickedBackupFile = null
        _pickedBackupState.value = PickedBackupState.NothingPicked
    }

    override fun onCleared() {
        pickedBackupFile?.close()
    }
}
