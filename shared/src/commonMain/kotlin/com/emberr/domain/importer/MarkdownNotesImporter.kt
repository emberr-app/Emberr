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
import com.emberr.domain.importer.markdown.isImagePath
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
    "webm" to "audio/webm",
    "ogg" to "audio/ogg",
    "opus" to "audio/opus",
    "flac" to "audio/flac",
    "mp4" to "video/mp4"
)
private const val UNLINKED_ATTACHMENTS_TITLE = "Unlinked attachments"

sealed interface ImportProgress {
    data object NotStarted : ImportProgress
    data class Importing(
        val handledNoteCount: Int,
        val totalNoteCount: Int,
        val doneFraction: Float,
        val copyingFileName: String? = null
    ) : ImportProgress
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
    private val canvasRepository: CanvasRepository,
    private val mediaStorageHelper: MediaStorageHelper,
    private val webImageFetcher: WebImageFetcher,
    private val reminderRescheduler: ReminderRescheduler,
    private val appScope: CoroutineScope
) {

    private val _progress = MutableStateFlow<ImportProgress>(ImportProgress.NotStarted)
    val progress: StateFlow<ImportProgress> = _progress.asStateFlow()

    fun startImport(pickedFiles: PickedImportFiles, spaceId: String) {
        _progress.value = ImportProgress.Importing(handledNoteCount = 0, totalNoteCount = noteFilesOf(pickedFiles).size, doneFraction = 0f)
        appScope.launch {
            val savedNotes = mutableListOf<Pair<NoteMetadataEntity, NoteContent>>()
            val savedCanvases = mutableListOf<Pair<String, CanvasContent>>()
            val result = pickedFiles.use { files ->
                importFiles(files, spaceId, onCanvasSaved = { noteId, canvas -> savedCanvases += noteId to canvas }) { metadata, content ->
                    savedNotes += metadata to content
                }
            }
            reminderRescheduler.rescheduleUpcomingReminders()
            _progress.value = ImportProgress.Finished(result)
            savedNotes.forEach { (metadata, content) -> repository.indexNote(metadata, content) }
            savedCanvases.forEach { (noteId, canvas) -> repository.indexCanvas(noteId, canvas) }
        }
    }

    fun clearFinishedImport() {
        if (_progress.value is ImportProgress.Finished) _progress.value = ImportProgress.NotStarted
    }

    suspend fun importFiles(
        pickedFiles: PickedImportFiles,
        spaceId: String,
        onCanvasSaved: (String, CanvasContent) -> Unit = { _, _ -> },
        onNoteSaved: (NoteMetadataEntity, NoteContent) -> Unit = { _, _ -> }
    ): ImportResult {
        val now = System.currentTimeMillis()
        val noteFiles = noteFilesOf(pickedFiles)
        val attachmentFilesByPath = pickedFiles.files
            .filterNot { file -> ImportPaths.isNoteFile(file.relativePath) }
            .associateBy { file -> file.relativePath }
        val plannedNotes = noteFiles.map { file -> PlannedNote(file = file, noteId = newId()) }
        val (plannedCanvases, plannedMarkdownNotes) = plannedNotes.partition { ImportPaths.isCanvasFile(it.file.relativePath) }
        val linkTargets = ImportedLinkTargets(
            noteIdsByPath = plannedNotes.distinctBy { it.file.relativePath }.associate { it.file.relativePath to it.noteId },
            attachmentPaths = attachmentFilesByPath.keys
        )

        val failedNames = linkedSetOf<String>()
        val convertedNotesByNoteId = mutableMapOf<String, ConvertedNote>()
        plannedMarkdownNotes.forEach { plannedNote ->
            val notePath = plannedNote.file.relativePath
            try {
                convertedNotesByNoteId[plannedNote.noteId] = ImportedNoteConverter.convert(
                    relativePath = notePath,
                    content = TextFileDecoder.decode(plannedNote.file.readBytes()),
                    linkResolver = linkTargets.resolverFor(notePath),
                    now = now
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                failedNames += notePath
            }
        }

        val noteTitlesById = convertedNotesByNoteId.mapValues { (_, converted) -> converted.title }
        val convertedCanvasesByNoteId = mutableMapOf<String, ConvertedCanvas>()
        plannedCanvases.forEach { plannedCanvas ->
            val canvasPath = plannedCanvas.file.relativePath
            try {
                convertedCanvasesByNoteId[plannedCanvas.noteId] = ObsidianCanvasConverter.convert(
                    canvasJson = TextFileDecoder.decode(plannedCanvas.file.readBytes()),
                    linkResolver = linkTargets.resolverFor(canvasPath),
                    noteTitlesById = noteTitlesById,
                    now = now
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                failedNames += canvasPath
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
            publish = { progress -> _progress.value = progress }
        )

        val folderIdsByPath = createFolders(noteFiles, spaceId, now, failedNames)
        val copiedAttachments = mutableMapOf<String, MediaInfo?>()
        val downloadedWebImages = mutableMapOf<String, MediaInfo?>()
        val customPropertiesByName = mutableMapOf<String, CustomPropertyEntity>()
        var importedNoteCount = 0

        suspend fun copiedAttachmentFor(attachmentPath: String): MediaInfo? =
            if (attachmentPath in copiedAttachments) {
                copiedAttachments[attachmentPath]
            } else {
                copyAttachment(attachmentFilesByPath.getValue(attachmentPath), progressTracker)
                    .also { mediaInfo -> copiedAttachments[attachmentPath] = mediaInfo }
            }

        plannedNotes.forEach { plannedNote ->
            val notePath = plannedNote.file.relativePath
            val converted = convertedNotesByNoteId[plannedNote.noteId]
            if (converted != null) {
                try {
                    val details = ImportedProperties.detailsOf(converted.frontMatter, converted.inlineTags)
                    val propertyBlocks = propertyBlocksFor(details.properties, spaceId, now, customPropertiesByName)
                    val blocksWithAttachments = withAttachmentsCopied(converted.blocks, converted.pendingAttachments, now, failedNames) { attachmentPath ->
                        copiedAttachmentFor(attachmentPath)
                    }
                    val blocks = propertyBlocks + withWebImagesDownloaded(blocksWithAttachments, converted.pendingWebImages, now) { url ->
                        if (url in downloadedWebImages) {
                            downloadedWebImages[url]
                        } else {
                            downloadWebImage(url, progressTracker).also { mediaInfo -> downloadedWebImages[url] = mediaInfo }
                        }
                    }
                    val content = NoteContent(blocks = blocks)
                    val metadata = NoteMetadataEntity(
                        noteId = plannedNote.noteId,
                        title = converted.title,
                        folderId = folderIdsByPath[notePath.substringBeforeLast('/', missingDelimiterValue = "")],
                        isDaily = false,
                        dateString = null,
                        createdAt = details.createdAt ?: fileDateOf(plannedNote.file, now),
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
            }
            val convertedCanvas = convertedCanvasesByNoteId[plannedNote.noteId]
            if (convertedCanvas != null) {
                try {
                    val content = withCanvasImagesCopied(convertedCanvas, failedNames) { imagePath -> copiedAttachmentFor(imagePath) }
                    val metadata = NoteMetadataEntity(
                        noteId = plannedNote.noteId,
                        title = notePath.substringAfterLast('/').substringBeforeLast('.').trim(),
                        folderId = folderIdsByPath[notePath.substringBeforeLast('/', missingDelimiterValue = "")],
                        isDaily = false,
                        dateString = null,
                        createdAt = fileDateOf(plannedNote.file, now),
                        updatedAt = now,
                        filePath = "",
                        spaceId = spaceId,
                        kind = NoteKind.CANVAS
                    )
                    repository.saveNoteInSpace(spaceId, metadata, NoteContent(blocks = emptyList()))
                    canvasRepository.saveChanges(plannedNote.noteId, content, stampNoteUpdatedAt = false)
                    onCanvasSaved(plannedNote.noteId, content)
                    importedNoteCount++
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (_: Exception) {
                    failedNames += notePath
                }
            }
            progressTracker.noteHandled(workBytesOf(plannedNote.file))
        }

        if (unlinkedAttachmentPaths.isNotEmpty()) {
            try {
                val (blocks, pendingAttachments) = unlinkedAttachmentBlocks(unlinkedAttachmentPaths, now)
                val content = NoteContent(blocks = withAttachmentsCopied(blocks, pendingAttachments, now, failedNames) { attachmentPath ->
                    copiedAttachmentFor(attachmentPath)
                })
                val metadata = NoteMetadataEntity(
                    noteId = newId(),
                    title = UNLINKED_ATTACHMENTS_TITLE,
                    folderId = sharedTopFolderOf(pickedFiles.files)?.let(folderIdsByPath::get),
                    isDaily = false,
                    dateString = null,
                    createdAt = now,
                    updatedAt = now,
                    filePath = "",
                    snippet = generateSnippet(content.blocks),
                    spaceId = spaceId
                )
                repository.saveNoteInSpace(spaceId, metadata, content)
                onNoteSaved(metadata, content)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                failedNames += UNLINKED_ATTACHMENTS_TITLE
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

    private fun unlinkedAttachmentBlocks(attachmentPaths: List<String>, now: Long): Pair<List<NoteBlock>, List<PendingAttachment>> {
        val blocks = mutableListOf<NoteBlock>()
        val pendingAttachments = mutableListOf<PendingAttachment>()
        attachmentPaths.groupBy { it.substringBeforeLast('/', missingDelimiterValue = "") }.forEach { (folderPath, pathsInFolder) ->
            if (folderPath.isNotEmpty()) blocks += HeadingBlock(id = newId(), text = folderPath, level = 2, updatedAt = now)
            pathsInFolder.forEach { attachmentPath ->
                val blockId = newId()
                blocks += if (isImagePath(attachmentPath)) {
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
                block.copy(localFilePath = mediaInfo.localFileName)
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

    private suspend fun downloadWebImage(url: String, progressTracker: ImportProgressTracker): MediaInfo? {
        val shownName = url.substringBefore('?').substringAfterLast('/').ifBlank { url }
        progressTracker.attachmentCopying(shownName, copiedBytes = 0L)
        return webImageFetcher.fetchIntoMedia(url)
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

    private suspend fun copyAttachment(file: ImportFile, progressTracker: ImportProgressTracker): MediaInfo? {
        val fileName = file.relativePath.substringAfterLast('/')
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        val mediaInfo = mediaStorageHelper.saveStreamToInternalStorage(
            openSource = file::openSource,
            extension = extension,
            mimeType = mimeTypesByExtension[extension] ?: UNKNOWN_MIME_TYPE,
            onBytesCopied = { copiedBytes -> progressTracker.attachmentCopying(fileName, copiedBytes.coerceAtMost(file.sizeBytes)) }
        )
        progressTracker.attachmentFinished(file.sizeBytes)
        return mediaInfo
    }

    private fun fileDateOf(file: ImportFile, now: Long): Long = file.lastModifiedAt?.coerceAtMost(now) ?: now

    private fun workBytesOf(noteFile: ImportFile): Long = noteFile.sizeBytes.coerceAtLeast(1)

    private fun noteFilesOf(pickedFiles: PickedImportFiles): List<ImportFile> =
        pickedFiles.files.filter { file -> ImportPaths.isNoteFile(file.relativePath) }

    @OptIn(ExperimentalUuidApi::class)
    private fun newId(): String = Uuid.random().toString()

    private class PlannedNote(val file: ImportFile, val noteId: String)
}

private class ImportProgressTracker(
    private val totalNoteCount: Int,
    private val totalWorkBytes: Long,
    private val publish: (ImportProgress.Importing) -> Unit
) {
    private var doneWorkBytes = 0L
    private var handledNoteCount = 0
    private var lastPublished: ImportProgress.Importing? = null

    fun noteHandled(noteBytes: Long) {
        handledNoteCount++
        doneWorkBytes += noteBytes
        publishIfVisiblyChanged(copyingFileName = null, bytesInProgress = 0L)
    }

    fun attachmentCopying(fileName: String, copiedBytes: Long) {
        publishIfVisiblyChanged(copyingFileName = fileName, bytesInProgress = copiedBytes)
    }

    fun attachmentFinished(attachmentBytes: Long) {
        doneWorkBytes += attachmentBytes
    }

    private fun publishIfVisiblyChanged(copyingFileName: String?, bytesInProgress: Long) {
        val doneFraction = when {
            totalWorkBytes > 0 -> ((doneWorkBytes + bytesInProgress).toFloat() / totalWorkBytes).coerceIn(0f, 1f)
            totalNoteCount > 0 -> handledNoteCount.toFloat() / totalNoteCount
            else -> 0f
        }
        val progress = ImportProgress.Importing(handledNoteCount, totalNoteCount, doneFraction, copyingFileName)
        val previous = lastPublished
        val looksTheSame = previous != null &&
            previous.handledNoteCount == progress.handledNoteCount &&
            previous.copyingFileName == progress.copyingFileName &&
            wholePercentOf(previous.doneFraction) == wholePercentOf(progress.doneFraction)
        if (looksTheSame) return
        lastPublished = progress
        publish(progress)
    }

    private fun wholePercentOf(fraction: Float): Int = (fraction * 100).toInt()
}
