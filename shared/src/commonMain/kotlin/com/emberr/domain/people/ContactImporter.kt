package com.emberr.domain.people

import com.emberr.domain.importer.TextFileDecoder
import com.emberr.domain.util.media.MediaStorageHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ContactImportProgress {
    data object Idle : ContactImportProgress
    data class Importing(val handledCount: Int, val totalCount: Int) : ContactImportProgress
    data class Finished(val result: ContactImportResult) : ContactImportProgress
}

data class ContactImportResult(
    val importedPersonNoteIds: List<String>,
    val copiesSkipped: Int,
    val failures: Int,
    val couldNotReadFile: Boolean = false
) {
    val foundNoContacts: Boolean get() = importedPersonNoteIds.isEmpty() && copiesSkipped == 0 && failures == 0 && !couldNotReadFile
}

class ContactImporter(
    private val peopleRepository: PeopleRepository,
    private val mediaStorageHelper: MediaStorageHelper,
    private val appScope: CoroutineScope
) {

    private val _progress = MutableStateFlow<ContactImportProgress>(ContactImportProgress.Idle)
    val progress: StateFlow<ContactImportProgress> = _progress.asStateFlow()

    fun startImport(contacts: List<ContactToImport>): Boolean = start { contacts }

    fun startImportFromFiles(fileContents: List<ByteArray>): Boolean =
        start { fileContents.flatMap { bytes -> readContactsFromVCardText(TextFileDecoder.decode(bytes)) } }

    fun takeFinishedResult(): ContactImportResult? {
        val currentProgress = _progress.value as? ContactImportProgress.Finished ?: return null
        return if (_progress.compareAndSet(currentProgress, ContactImportProgress.Idle)) currentProgress.result else null
    }

    private fun start(readContacts: () -> List<ContactToImport>): Boolean {
        val currentProgress = _progress.value
        if (currentProgress is ContactImportProgress.Importing) return false
        if (!_progress.compareAndSet(currentProgress, ContactImportProgress.Importing(handledCount = 0, totalCount = 0))) return false
        appScope.launch {
            val contacts = try {
                readContacts()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            val result = if (contacts == null) {
                ContactImportResult(importedPersonNoteIds = emptyList(), copiesSkipped = 0, failures = 0, couldNotReadFile = true)
            } else {
                addContacts(contacts)
            }
            _progress.value = ContactImportProgress.Finished(result)
        }
        return true
    }

    private suspend fun addContacts(contacts: List<ContactToImport>): ContactImportResult =
        peopleRepository.createPeopleFromContacts(
            contacts = contacts,
            savePhoto = ::savePhoto,
            onContactHandled = { handledCount ->
                _progress.value = ContactImportProgress.Importing(handledCount = handledCount, totalCount = contacts.size)
            }
        )

    private suspend fun savePhoto(photoBytes: ByteArray): String? {
        val (extension, mimeType) = contactPhotoExtensionAndMimeType(photoBytes)
        return mediaStorageHelper.saveBytesToInternalStorage(photoBytes, extension, mimeType)?.localFileName
    }
}
