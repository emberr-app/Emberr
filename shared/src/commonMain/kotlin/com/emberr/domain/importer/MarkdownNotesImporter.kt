package com.emberr.domain.importer

import com.emberr.data.local.room.entity.CanvasNodeType
import com.emberr.data.local.room.entity.CustomPropertyEntity
import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.canvas.CanvasContent
import com.emberr.domain.canvas.CanvasRepository
import com.emberr.domain.importer.canvas.ConvertedCanvas
import com.emberr.domain.importer.canvas.ObsidianCanvasConverter
import com.emberr.domain.importer.markdown.ConvertedNote
import com.emberr.domain.importer.markdown.ImportedNoteConverter
import com.emberr.domain.importer.markdown.PendingAttachment
import com.emberr.domain.importer.markdown.PendingWebImage
import com.emberr.domain.model.BookmarkBlock
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.generateSnippet
import com.emberr.domain.reminders.ReminderRescheduler
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.util.media.MediaInfo
import com.emberr.domain.util.media.MediaStorageHelper
import com.emberr.domain.util.media.WebImageFetcher
import com.emberr.domain.util.media.mimeTypeForExtension
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val UNKNOWN_MIME_TYPE = "application/octet-stream"
private const val UNLINKED_ATTACHMENTS_TITLE = "Unlinked attachments"
private const val IMPORT_STOPPED_EARLY = "the rest of the import (it stopped early)"

class MarkdownNotesImporter(
    private val repository: NoteRepository,
    private val canvasRepository: CanvasRepository,
    private val mediaStorageHelper: MediaStorageHelper,
    private val webImageFetcher: WebImageFetcher,
    private val reminderRescheduler: ReminderRescheduler,
    private val appScope: CoroutineScope
) {

    private val _progress = MutableStateFlow<ImportProgress>(ImportProgress.NotStarted)
    val progress: StateFlow<ImportProgress> = _progress.asStateFlow()

    private var importJob: Job? = null

    fun startImport(pickedFiles: PickedImportFiles, spaceId: String) {
        _progress.value = ImportProgress.Importing(handledNoteCount = 0, totalNoteCount = noteFilesOf(pickedFiles).size, doneFraction = 0f)
        importJob = appScope.launch {
            val savedNotes = mutableListOf<Pair<NoteMetadataEntity, NoteContent>>()
            val savedCanvases = mutableListOf<Pair<String, CanvasContent>>()
            val result = try {
                pickedFiles.use { files ->
                    importFiles(files, spaceId, onCanvasSaved = { noteId, canvas -> savedCanvases += noteId to canvas }) { metadata, content ->
                        savedNotes += metadata to content
                    }
                }
            } catch (cancellation: CancellationException) {
                _progress.value = ImportProgress.Stopped
                throw cancellation
            } catch (_: Throwable) {
                ImportResult(
                    spaceId = spaceId,
                    importedNoteCount = savedNotes.size + savedCanvases.size,
                    createdFolderCount = 0,
                    copiedAttachmentCount = 0,
                    failedNames = listOf(IMPORT_STOPPED_EARLY)
                )
            }
            withContext(NonCancellable) {
                reminderRescheduler.rescheduleUpcomingReminders()
                _progress.value = ImportProgress.Finished(result)
                savedNotes.forEach { (metadata, content) -> repository.indexNote(metadata, content) }
                savedCanvases.forEach { (noteId, canvas) -> repository.indexCanvas(noteId, canvas) }
            }
        }
    }

    fun stopImport() {
        if (_progress.value !is ImportProgress.Importing) return
        _progress.value = ImportProgress.Stopping
        importJob?.cancel()
    }

    fun clearFinishedImport() {
        if (_progress.value is ImportProgress.Finished || _progress.value == ImportProgress.Stopped) {
            _progress.value = ImportProgress.NotStarted
        }
    }

    suspend fun importFiles(
        pickedFiles: PickedImportFiles,
        spaceId: String,
        onCanvasSaved: (String, CanvasContent) -> Unit = { _, _ -> },
        onNoteSaved: (NoteMetadataEntity, NoteContent) -> Unit = { _, _ -> }
    ): ImportResult {
        val createdItems = ItemsCreatedByImport()
        return try {
            importFilesRecordingCreatedItems(pickedFiles, spaceId, createdItems, onCanvasSaved, onNoteSaved)
        } catch (cancellation: CancellationException) {
            withContext(NonCancellable) { deleteCreatedItems(createdItems) }
            throw cancellation
        }
    }

    private suspend fun importFilesRecordingCreatedItems(
        pickedFiles: PickedImportFiles,
        spaceId: String,
        createdItems: ItemsCreatedByImport,
        onCanvasSaved: (String, CanvasContent) -> Unit,
        onNoteSaved: (NoteMetadataEntity, NoteContent) -> Unit
    ): ImportResult {
        val now = System.currentTimeMillis()
        val noteFiles = noteFilesOf(pickedFiles)
        val attachmentFilesByPath = pickedFiles.files
            .filterNot { file -> ImportPaths.isNoteFile(file.relativePath) }
            .associateBy { file -> file.relativePath }
        val plannedNotes = noteFiles.map { file -> PlannedNote(file = file, noteId = newImportId()) }
        val (plannedCanvases, plannedMarkdownNotes) = plannedNotes.partition { ImportPaths.isCanvasFile(it.file.relativePath) }
        val linkTargets = ImportedLinkTargets(
            noteIdsByPath = plannedNotes.distinctBy { it.file.relativePath }.associate { it.file.relativePath to it.noteId },
            attachmentPaths = attachmentFilesByPath.keys
        )

        val failedNames = linkedSetOf<String>()
        val convertedNotesByNoteId = mutableMapOf<String, ConvertedNote>()
        plannedMarkdownNotes.forEach { plannedNote ->
            val notePath = plannedNote.file.relativePath
            rememberFailure(failedNames, notePath) {
                convertedNotesByNoteId[plannedNote.noteId] = ImportedNoteConverter.convert(
                    relativePath = notePath,
                    content = TextFileDecoder.decode(plannedNote.file.readBytes()),
                    linkResolver = linkTargets.resolverFor(notePath),
                    now = now
                )
            }
        }

        val noteTitlesById = convertedNotesByNoteId.mapValues { (_, converted) -> converted.title }
        val convertedCanvasesByNoteId = mutableMapOf<String, ConvertedCanvas>()
        plannedCanvases.forEach { plannedCanvas ->
            val canvasPath = plannedCanvas.file.relativePath
            rememberFailure(failedNames, canvasPath) {
                convertedCanvasesByNoteId[plannedCanvas.noteId] = ObsidianCanvasConverter.convert(
                    canvasJson = TextFileDecoder.decode(plannedCanvas.file.readBytes()),
                    linkResolver = linkTargets.resolverFor(canvasPath),
                    noteTitlesById = noteTitlesById,
                    now = now
                )
            }
        }

        val usedAttachmentPaths = (
            convertedNotesByNoteId.values.flatMap { converted -> converted.pendingAttachments.map { it.relativePath } } +
                convertedCanvasesByNoteId.values.flatMap { converted -> converted.pendingImages.map { it.relativePath } }
            ).toSet()
        val unlinkedAttachmentPaths = (attachmentFilesByPath.keys - usedAttachmentPaths).sorted()
        val progressTracker = ImportProgressTracker(
            totalNoteCount = plannedNotes.size,
            totalWorkBytes = plannedNotes.sumOf { workBytesOf(it.file) } +
                attachmentFilesByPath.values.sumOf { it.sizeBytes },
            publish = { progress -> _progress.update { current -> if (current == ImportProgress.Stopping) current else progress } }
        )

        val folderIdsByPath = createFolders(noteFiles, spaceId, now, failedNames, createdItems)
        val copiedAttachments = mutableMapOf<String, MediaInfo?>()
        val downloadedWebImages = mutableMapOf<String, MediaInfo?>()
        val customPropertiesByName = mutableMapOf<String, CustomPropertyEntity>()
        var importedNoteCount = 0

        suspend fun copiedAttachmentFor(attachmentPath: String): MediaInfo? =
            if (attachmentPath in copiedAttachments) {
                copiedAttachments[attachmentPath]
            } else {
                copyAttachment(attachmentFilesByPath.getValue(attachmentPath), progressTracker, createdItems)
                    .also { mediaInfo -> copiedAttachments[attachmentPath] = mediaInfo }
            }

        plannedNotes.forEach { plannedNote ->
            val notePath = plannedNote.file.relativePath
            val converted = convertedNotesByNoteId[plannedNote.noteId]
            if (converted != null) {
                rememberFailure(failedNames, notePath) {
                    val details = ImportedProperties.detailsOf(converted.frontMatter, converted.inlineTags)
                    val propertyBlocks = propertyBlocksFor(details.properties, spaceId, now, customPropertiesByName, createdItems)
                    val blocksWithAttachments = withAttachmentsCopied(converted.blocks, converted.pendingAttachments, now, failedNames) { attachmentPath ->
                        copiedAttachmentFor(attachmentPath)
                    }
                    val blocks = propertyBlocks + withWebImagesDownloaded(blocksWithAttachments, converted.pendingWebImages, now) { url ->
                        if (url in downloadedWebImages) {
                            downloadedWebImages[url]
                        } else {
                            downloadWebImage(url, progressTracker, createdItems).also { mediaInfo -> downloadedWebImages[url] = mediaInfo }
                        }
                    }
                    val content = NoteContent(blocks = blocks)
                    val metadata = importedNoteMetadata(
                        noteId = plannedNote.noteId,
                        title = converted.title,
                        folderId = folderIdsByPath[ImportPaths.folderOf(notePath)],
                        createdAt = details.createdAt ?: fileDateOf(plannedNote.file, now),
                        now = now,
                        spaceId = spaceId,
                        snippet = generateSnippet(blocks)
                    )
                    createdItems.noteIds += metadata.noteId
                    repository.saveNoteInSpace(spaceId, metadata, content)
                    onNoteSaved(metadata, content)
                    importedNoteCount++
                }
            }
            val convertedCanvas = convertedCanvasesByNoteId[plannedNote.noteId]
            if (convertedCanvas != null) {
                rememberFailure(failedNames, notePath) {
                    val content = withCanvasImagesCopied(convertedCanvas, failedNames) { imagePath -> copiedAttachmentFor(imagePath) }
                    val metadata = importedNoteMetadata(
                        noteId = plannedNote.noteId,
                        title = ImportPaths.noteTitleOf(notePath),
                        folderId = folderIdsByPath[ImportPaths.folderOf(notePath)],
                        createdAt = fileDateOf(plannedNote.file, now),
                        now = now,
                        spaceId = spaceId,
                        kind = NoteKind.CANVAS
                    )
                    createdItems.noteIds += metadata.noteId
                    repository.saveNoteInSpace(spaceId, metadata, NoteContent(blocks = emptyList()))
                    canvasRepository.saveChanges(plannedNote.noteId, content, stampNoteUpdatedAt = false)
                    onCanvasSaved(plannedNote.noteId, content)
                    importedNoteCount++
                }
            }
            progressTracker.noteHandled(workBytesOf(plannedNote.file))
        }

        if (unlinkedAttachmentPaths.isNotEmpty()) {
            rememberFailure(failedNames, UNLINKED_ATTACHMENTS_TITLE) {
                val (blocks, pendingAttachments) = unlinkedAttachmentBlocks(unlinkedAttachmentPaths, now)
                val content = NoteContent(blocks = withAttachmentsCopied(blocks, pendingAttachments, now, failedNames) { attachmentPath ->
                    copiedAttachmentFor(attachmentPath)
                })
                val metadata = importedNoteMetadata(
                    noteId = newImportId(),
                    title = UNLINKED_ATTACHMENTS_TITLE,
                    folderId = sharedTopFolderOf(pickedFiles.files)?.let(folderIdsByPath::get),
                    createdAt = now,
                    now = now,
                    spaceId = spaceId,
                    snippet = generateSnippet(content.blocks)
                )
                createdItems.noteIds += metadata.noteId
                repository.saveNoteInSpace(spaceId, metadata, content)
                onNoteSaved(metadata, content)
            }
        }

        return ImportResult(
            spaceId = spaceId,
            importedNoteCount = importedNoteCount,
            createdFolderCount = folderIdsByPath.size,
            copiedAttachmentCount = copiedAttachments.values.count { it != null } + downloadedWebImages.values.count { it != null },
            failedNames = failedNames.toList()
        )
    }

    private suspend fun createFolders(
        noteFiles: List<ImportFile>,
        spaceId: String,
        now: Long,
        failedNames: MutableSet<String>,
        createdItems: ItemsCreatedByImport
    ): Map<String, String> {
        val folderIdsByPath = mutableMapOf<String, String>()
        val folderPaths = ImportPaths.folderPathsHoldingNotes(noteFiles.map { it.relativePath })
            .sortedWith(compareBy({ it.count { character -> character == '/' } }, { it }))

        for (folderPath in folderPaths) {
            val folderId = newImportId()
            rememberFailure(failedNames, folderPath) {
                createdItems.folderIds += folderId
                repository.insertFolderInSpace(
                    spaceId,
                    FolderEntity(
                        folderId = folderId,
                        name = folderPath.substringAfterLast('/'),
                        parentFolderId = folderIdsByPath[ImportPaths.folderOf(folderPath)],
                        createdAt = now,
                        spaceId = spaceId
                    )
                )
                folderIdsByPath[folderPath] = folderId
            }
        }
        return folderIdsByPath
    }

    private suspend fun propertyBlocksFor(
        properties: List<ImportedProperty>,
        spaceId: String,
        now: Long,
        customPropertiesByName: MutableMap<String, CustomPropertyEntity>,
        createdItems: ItemsCreatedByImport
    ): List<NoteBlock> =
        properties.mapNotNull { property ->
            val emptyBlock = when (val destination = property.destination) {
                is PropertyDestination.BuiltIn -> PropertyBlock(id = newImportId(), propertyType = destination.propertyType, updatedAt = now)
                is PropertyDestination.Custom -> {
                    val customProperty = customPropertiesByName.getOrPut(destination.name.lowercase()) {
                        repository.findCustomPropertyInSpace(spaceId, destination.name)
                            ?: finishEvenIfStopped { repository.createCustomPropertyInSpace(spaceId, destination.name, destination.valueType) }
                                .also { createdProperty -> createdItems.customPropertyIds += createdProperty.propertyId }
                    }
                    PropertyBlock(
                        id = newImportId(),
                        customPropertyId = customProperty.propertyId,
                        customLabel = customProperty.name,
                        customValueType = customProperty.valueType,
                        updatedAt = now
                    )
                }
            }
            val filledBlock = emptyBlock.withImportedValue(property.value) ?: return@mapNotNull null
            if (filledBlock.valueType.holdsTags) {
                val createdTags = finishEvenIfStopped { repository.createPropertyTagsInSpace(spaceId, filledBlock.tagPoolKey, filledBlock.tags) }
                createdItems.propertyTagIds += createdTags.map { it.tagId }
            }
            filledBlock
        }

    private fun unlinkedAttachmentBlocks(attachmentPaths: List<String>, now: Long): Pair<List<NoteBlock>, List<PendingAttachment>> {
        val blocks = mutableListOf<NoteBlock>()
        val pendingAttachments = mutableListOf<PendingAttachment>()
        attachmentPaths.groupBy(ImportPaths::folderOf).forEach { (folderPath, pathsInFolder) ->
            if (folderPath.isNotEmpty()) blocks += HeadingBlock(id = newImportId(), text = folderPath, level = 2, updatedAt = now)
            pathsInFolder.forEach { attachmentPath ->
                val blockId = newImportId()
                blocks += if (ImportPaths.isImageFile(attachmentPath)) {
                    ImageBlock(id = blockId, updatedAt = now)
                } else {
                    DocumentBlock(id = blockId, fileName = attachmentPath.substringAfterLast('/'), updatedAt = now)
                }
                pendingAttachments += PendingAttachment(blockId = blockId, relativePath = attachmentPath)
            }
        }
        return blocks to pendingAttachments
    }

    private fun sharedTopFolderOf(files: List<ImportFile>): String? {
        val topFolders = files.map { file -> file.relativePath.substringBefore('/', missingDelimiterValue = "") }.toSet()
        return topFolders.singleOrNull()?.takeIf { it.isNotEmpty() }
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

    private suspend fun withWebImagesDownloaded(
        blocks: List<NoteBlock>,
        pendingWebImages: List<PendingWebImage>,
        now: Long,
        downloadedImageFor: suspend (String) -> MediaInfo?
    ): List<NoteBlock> {
        val webImagesByBlockId = pendingWebImages.associateBy { it.blockId }
        return blocks.map { block ->
            val webImage = webImagesByBlockId[block.id] ?: return@map block
            val mediaInfo = downloadedImageFor(webImage.url)
            if (mediaInfo != null && block is ImageBlock) {
                block.copy(localFilePath = mediaInfo.localFileName, sourceUrl = webImage.url)
            } else {
                BookmarkBlock(
                    id = block.id,
                    url = webImage.url,
                    title = webImage.altText.takeIf { it.isNotBlank() && it != webImage.url },
                    indentationLevel = block.indentationLevel,
                    updatedAt = now
                )
            }
        }
    }

    private suspend fun downloadWebImage(url: String, progressTracker: ImportProgressTracker, createdItems: ItemsCreatedByImport): MediaInfo? {
        val shownName = url.substringBefore('?').substringAfterLast('/').ifBlank { url }
        progressTracker.attachmentCopying(shownName, copiedBytes = 0L)
        return webImageFetcher.fetchIntoMedia(url)?.also { mediaInfo -> createdItems.mediaFileNames += mediaInfo.localFileName }
    }

    private suspend fun withCanvasImagesCopied(
        convertedCanvas: ConvertedCanvas,
        failedNames: MutableSet<String>,
        copiedAttachmentFor: suspend (String) -> MediaInfo?
    ): CanvasContent {
        val imagePathsByNodeId = convertedCanvas.pendingImages.associate { it.nodeId to it.relativePath }
        val nodes = convertedCanvas.content.nodes.map { node ->
            val imagePath = imagePathsByNodeId[node.nodeId] ?: return@map node
            val mediaInfo = copiedAttachmentFor(imagePath)
            if (mediaInfo == null) {
                failedNames += imagePath
                node.copy(type = CanvasNodeType.TEXT, text = imagePath.substringAfterLast('/'))
            } else {
                node.copy(imagePath = mediaInfo.localFileName)
            }
        }
        return convertedCanvas.content.copy(nodes = nodes)
    }

    private suspend fun copyAttachment(
        file: ImportFile,
        progressTracker: ImportProgressTracker,
        createdItems: ItemsCreatedByImport
    ): MediaInfo? {
        val fileName = file.relativePath.substringAfterLast('/')
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        val importJob = currentCoroutineContext().job
        val mediaInfo = finishEvenIfStopped {
            mediaStorageHelper.saveStreamToInternalStorage(
                openSource = file::openSource,
                extension = extension,
                mimeType = mimeTypeForExtension(extension) ?: UNKNOWN_MIME_TYPE,
                onBytesCopied = { copiedBytes ->
                    importJob.ensureActive()
                    progressTracker.attachmentCopying(fileName, copiedBytes.coerceAtMost(file.sizeBytes))
                }
            )
        }
        mediaInfo?.let { createdItems.mediaFileNames += it.localFileName }
        progressTracker.attachmentFinished(file.sizeBytes)
        return mediaInfo
    }

    private suspend fun <T> finishEvenIfStopped(work: suspend () -> T): T = withContext(NonCancellable) { work() }

    private suspend fun deleteCreatedItems(createdItems: ItemsCreatedByImport) {
        createdItems.noteIds.forEach { noteId ->
            tryDeleting { if (repository.getNoteById(noteId) != null) repository.deleteNote(noteId, filePath = "") }
        }
        createdItems.folderIds.forEach { folderId -> tryDeleting { repository.deleteFolder(folderId) } }
        tryDeleting { repository.deleteCustomPropertiesAndTags(createdItems.customPropertyIds, createdItems.propertyTagIds) }
        createdItems.mediaFileNames.forEach { fileName -> tryDeleting { mediaStorageHelper.deleteMediaFile(fileName) } }
    }

    private inline fun tryDeleting(delete: () -> Unit) {
        try {
            delete()
        } catch (_: Exception) {
        }
    }

    private fun importedNoteMetadata(
        noteId: String,
        title: String,
        folderId: String?,
        createdAt: Long,
        now: Long,
        spaceId: String,
        snippet: String = "",
        kind: NoteKind = NoteKind.NOTE
    ): NoteMetadataEntity =
        NoteMetadataEntity(
            noteId = noteId,
            title = title,
            folderId = folderId,
            isDaily = false,
            dateString = null,
            createdAt = createdAt,
            updatedAt = now,
            filePath = "",
            snippet = snippet,
            spaceId = spaceId,
            kind = kind
        )

    private inline fun rememberFailure(failedNames: MutableSet<String>, name: String, action: () -> Unit) {
        try {
            action()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            failedNames += name
        }
    }

    private fun fileDateOf(file: ImportFile, now: Long): Long = file.lastModifiedAt?.coerceAtMost(now) ?: now

    private fun workBytesOf(noteFile: ImportFile): Long = noteFile.sizeBytes.coerceAtLeast(1)

    private fun noteFilesOf(pickedFiles: PickedImportFiles): List<ImportFile> =
        pickedFiles.files.filter { file -> ImportPaths.isNoteFile(file.relativePath) }

    private class PlannedNote(val file: ImportFile, val noteId: String)

    private class ItemsCreatedByImport {
        val noteIds = mutableListOf<String>()
        val folderIds = mutableListOf<String>()
        val customPropertyIds = mutableListOf<String>()
        val propertyTagIds = mutableListOf<String>()
        val mediaFileNames = mutableListOf<String>()
    }
}
