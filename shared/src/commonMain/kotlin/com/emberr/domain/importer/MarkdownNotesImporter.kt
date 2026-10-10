package com.emberr.domain.importer

import com.emberr.data.local.room.entity.CustomPropertyEntity
import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.importer.markdown.ImportedNoteConverter
import com.emberr.domain.importer.markdown.PendingAttachment
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.generateSnippet
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.util.media.MediaInfo
import com.emberr.domain.util.media.MediaStorageHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val UNKNOWN_MIME_TYPE = "application/octet-stream"
private val mimeTypesByExtension = mapOf(
    "png" to "image/png",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "gif" to "image/gif",
    "webp" to "image/webp",
    "bmp" to "image/bmp",
    "heic" to "image/heic",
    "heif" to "image/heif",
    "svg" to "image/svg+xml",
    "pdf" to "application/pdf",
    "txt" to "text/plain",
    "csv" to "text/csv",
    "doc" to "application/msword",
    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "xls" to "application/vnd.ms-excel",
    "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "ppt" to "application/vnd.ms-powerpoint",
    "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "zip" to "application/zip",
    "mp3" to "audio/mpeg",
    "m4a" to "audio/mp4",
    "wav" to "audio/wav",
    "mp4" to "video/mp4"
)

sealed interface ImportProgress {
    data object NotStarted : ImportProgress
    data class Importing(val handledNoteCount: Int, val totalNoteCount: Int) : ImportProgress
    data class Finished(val result: ImportResult) : ImportProgress
}

data class ImportResult(
    val spaceId: String,
    val importedNoteCount: Int,
    val createdFolderCount: Int,
    val copiedAttachmentCount: Int,
    val failedNames: List<String>
)

class MarkdownNotesImporter(
    private val repository: NoteRepository,
    private val mediaStorageHelper: MediaStorageHelper,
    private val appScope: CoroutineScope
) {

    private val _progress = MutableStateFlow<ImportProgress>(ImportProgress.NotStarted)
    val progress: StateFlow<ImportProgress> = _progress.asStateFlow()

    fun startImport(pickedFiles: PickedImportFiles, spaceId: String) {
        _progress.value = ImportProgress.Importing(handledNoteCount = 0, totalNoteCount = noteFilesOf(pickedFiles).size)
        appScope.launch {
            val savedNotes = mutableListOf<Pair<NoteMetadataEntity, NoteContent>>()
            val result = pickedFiles.use { files ->
                importFiles(files, spaceId) { metadata, content -> savedNotes += metadata to content }
            }
            _progress.value = ImportProgress.Finished(result)
            savedNotes.forEach { (metadata, content) -> repository.indexNote(metadata, content) }
        }
    }

    fun clearFinishedImport() {
        if (_progress.value is ImportProgress.Finished) _progress.value = ImportProgress.NotStarted
    }

    suspend fun importFiles(
        pickedFiles: PickedImportFiles,
        spaceId: String,
        onNoteSaved: (NoteMetadataEntity, NoteContent) -> Unit = { _, _ -> }
    ): ImportResult {
        val now = System.currentTimeMillis()
        val noteFiles = noteFilesOf(pickedFiles)
        val attachmentFilesByPath = pickedFiles.files
            .filterNot { file -> ImportPaths.isNoteFile(file.relativePath) }
            .associateBy { file -> file.relativePath }
        val plannedNotes = noteFiles.map { file -> PlannedNote(file = file, noteId = newId()) }
        val linkTargets = ImportedLinkTargets(
            noteIdsByPath = plannedNotes.distinctBy { it.file.relativePath }.associate { it.file.relativePath to it.noteId },
            attachmentPaths = attachmentFilesByPath.keys
        )

        val failedNames = linkedSetOf<String>()
        val folderIdsByPath = createFolders(noteFiles, spaceId, now, failedNames)
        val copiedAttachments = mutableMapOf<String, MediaInfo?>()
        val customPropertiesByName = mutableMapOf<String, CustomPropertyEntity>()
        var importedNoteCount = 0

        plannedNotes.forEachIndexed { index, plannedNote ->
            val notePath = plannedNote.file.relativePath
            try {
                val converted = ImportedNoteConverter.convert(
                    relativePath = notePath,
                    content = plannedNote.file.readBytes().decodeToString(),
                    linkResolver = linkTargets.resolverFor(notePath),
                    now = now
                )
                val details = ImportedProperties.detailsOf(converted.frontMatter, converted.inlineTags)
                val propertyBlocks = propertyBlocksFor(details.properties, spaceId, now, customPropertiesByName)
                val blocks = propertyBlocks + withAttachmentsCopied(converted.blocks, converted.pendingAttachments, now, failedNames) { attachmentPath ->
                    copiedAttachments.getOrPut(attachmentPath) { copyAttachment(attachmentFilesByPath.getValue(attachmentPath)) }
                }
                val content = NoteContent(blocks = blocks)
                val metadata = NoteMetadataEntity(
                    noteId = plannedNote.noteId,
                    title = converted.title,
                    folderId = folderIdsByPath[notePath.substringBeforeLast('/', missingDelimiterValue = "")],
                    isDaily = false,
                    dateString = null,
                    createdAt = details.createdAt ?: now,
                    updatedAt = now,
                    filePath = "",
                    snippet = generateSnippet(blocks),
                    spaceId = spaceId
                )
                repository.saveNoteInSpace(spaceId, metadata, content)
                onNoteSaved(metadata, content)
                importedNoteCount++
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                failedNames += notePath
            }
            _progress.value = ImportProgress.Importing(handledNoteCount = index + 1, totalNoteCount = plannedNotes.size)
        }

        return ImportResult(
            spaceId = spaceId,
            importedNoteCount = importedNoteCount,
            createdFolderCount = folderIdsByPath.size,
            copiedAttachmentCount = copiedAttachments.values.count { it != null },
            failedNames = failedNames.toList()
        )
    }

    private suspend fun createFolders(
        noteFiles: List<ImportFile>,
        spaceId: String,
        now: Long,
        failedNames: MutableSet<String>
    ): Map<String, String> {
        val folderIdsByPath = mutableMapOf<String, String>()
        val folderPaths = ImportPaths.folderPathsHoldingNotes(noteFiles.map { it.relativePath })
            .sortedWith(compareBy({ it.count { character -> character == '/' } }, { it }))

        for (folderPath in folderPaths) {
            val folderId = newId()
            try {
                repository.insertFolderInSpace(
                    spaceId,
                    FolderEntity(
                        folderId = folderId,
                        name = folderPath.substringAfterLast('/'),
                        parentFolderId = folderIdsByPath[folderPath.substringBeforeLast('/', missingDelimiterValue = "")],
                        createdAt = now,
                        spaceId = spaceId
                    )
                )
                folderIdsByPath[folderPath] = folderId
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                failedNames += folderPath
            }
        }
        return folderIdsByPath
    }

    private suspend fun propertyBlocksFor(
        properties: List<ImportedProperty>,
        spaceId: String,
        now: Long,
        customPropertiesByName: MutableMap<String, CustomPropertyEntity>
    ): List<NoteBlock> =
        properties.mapNotNull { property ->
            val emptyBlock = when (val destination = property.destination) {
                is PropertyDestination.BuiltIn -> PropertyBlock(id = newId(), propertyType = destination.propertyType, updatedAt = now)
                is PropertyDestination.Custom -> {
                    val customProperty = customPropertiesByName.getOrPut(destination.name.lowercase()) {
                        repository.findOrCreateCustomPropertyInSpace(spaceId, destination.name, destination.valueType)
                    }
                    PropertyBlock(
                        id = newId(),
                        customPropertyId = customProperty.propertyId,
                        customLabel = customProperty.name,
                        customValueType = customProperty.valueType,
                        updatedAt = now
                    )
                }
            }
            val filledBlock = emptyBlock.withImportedValue(property.value) ?: return@mapNotNull null
            if (filledBlock.valueType.holdsTags) {
                repository.createPropertyTagsInSpace(spaceId, filledBlock.tagPoolKey, filledBlock.tags)
            }
            filledBlock
        }

    private suspend fun withAttachmentsCopied(
        blocks: List<NoteBlock>,
        pendingAttachments: List<PendingAttachment>,
        now: Long,
        failedNames: MutableSet<String>,
        copiedAttachmentFor: suspend (String) -> MediaInfo?
    ): List<NoteBlock> {
        val attachmentPathsByBlockId = pendingAttachments.associate { it.blockId to it.relativePath }
        return blocks.map { block ->
            val attachmentPath = attachmentPathsByBlockId[block.id] ?: return@map block
            val mediaInfo = copiedAttachmentFor(attachmentPath)
            when {
                mediaInfo == null -> {
                    failedNames += attachmentPath
                    TextBlock(
                        id = block.id,
                        text = attachmentPath.substringAfterLast('/'),
                        indentationLevel = block.indentationLevel,
                        updatedAt = now
                    )
                }
                block is ImageBlock -> block.copy(localFilePath = mediaInfo.localFileName)
                block is DocumentBlock -> block.copy(
                    localFilePath = mediaInfo.localFileName,
                    mimeType = mediaInfo.mimeType,
                    fileSizeString = mediaInfo.formattedSize
                )
                else -> block
            }
        }
    }

    private suspend fun copyAttachment(file: ImportFile): MediaInfo? {
        val extension = file.relativePath.substringAfterLast('/').substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return try {
            mediaStorageHelper.saveBytesToInternalStorage(
                bytes = file.readBytes(),
                extension = extension,
                mimeType = mimeTypesByExtension[extension] ?: UNKNOWN_MIME_TYPE
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
    }

    private fun noteFilesOf(pickedFiles: PickedImportFiles): List<ImportFile> =
        pickedFiles.files.filter { file -> ImportPaths.isNoteFile(file.relativePath) }

    @OptIn(ExperimentalUuidApi::class)
    private fun newId(): String = Uuid.random().toString()

    private class PlannedNote(val file: ImportFile, val noteId: String)
}
