package com.emberr.domain.util.export

import com.emberr.data.local.room.entity.NoteKind
import com.emberr.domain.canvas.CanvasRepository
import com.emberr.domain.database.withDatabasesAsTables
import com.emberr.domain.model.CanvasBlock
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.movie.withWatchlistsExpanded
import com.emberr.domain.people.personNamesLinkedIn
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.space.SpaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

private const val PARENT_FOLDER_STEP = "../"

const val MARKDOWN_VAULT_FILE_NAME = "Emberr Vault.zip"

data class MarkdownVaultTextFile(val path: String, val text: String)

data class MarkdownVault(
    val folderPaths: List<String>,
    val textFiles: List<MarkdownVaultTextFile>,
    val attachments: List<MarkdownAttachment>
)

class MarkdownVaultExporter(
    private val repository: NoteRepository,
    private val canvasRepository: CanvasRepository,
    private val spaceRepository: SpaceRepository
) {

    suspend fun build(spaceIds: Set<String>): MarkdownVault = withContext(Dispatchers.IO) {
        val spaces = spaceRepository.observeSpaces().first().filter { it.spaceId in spaceIds && !it.isDeleted }
        val folders = repository.getFoldersModifiedSince(-1L).filter { it.spaceId in spaceIds && !it.isDeleted }
        val notes = repository.getAllNotesAcrossSpaces().filter { note ->
            note.spaceId in spaceIds && note.databaseId == null && (note.kind == NoteKind.NOTE || note.kind == NoteKind.CANVAS)
        }
        val plan = MarkdownVaultPaths.plan(spaces, folders, notes)

        val textNotes = notes.filter { it.kind == NoteKind.NOTE && it.noteId in plan.notePathsById }
        val blocksByNoteId = textNotes.associate { note -> note.noteId to blocksForExport(note.noteId) }
        val allBlocks = blocksByNoteId.values.flatten()
        val embeddedCanvases = embeddedCanvasesIn(allBlocks)
        val standaloneCanvases = notes
            .filter { it.kind == NoteKind.CANVAS && it.noteId in plan.notePathsById }
            .map { note -> ExportedCanvas(note.noteId, note.title, canvasRepository.loadCanvasIncludingDeleted(note.noteId)) }
        val attachments = MarkdownAttachments.collect(allBlocks, embeddedCanvases + standaloneCanvases)
        val canvasFiles = MarkdownAttachments.canvasFilesFor(embeddedCanvases, attachments)
        val rootAttachmentPathsByStoredName = attachments.associate { it.storedFileName to it.pathInExport }

        val categoryNamesById = repository.getAllCategories().first()
            .filter { !it.isDeleted }
            .associate { it.categoryId to it.name }
        val linkedNoteTitlesById = linkedNoteTitlesIn(allBlocks)
        val canvasPathsByNoteId = canvasFiles.associate { it.canvasNoteId to it.pathInExport } +
            standaloneCanvases.associate { it.canvasNoteId to plan.notePathsById.getValue(it.canvasNoteId) }

        val noteFiles = textNotes.map { note ->
            val notePath = plan.notePathsById.getValue(note.noteId)
            val blocks = blocksByNoteId.getValue(note.noteId)
            val stepsToVaultRoot = PARENT_FOLDER_STEP.repeat(notePath.count { it == '/' })
            MarkdownVaultTextFile(
                path = notePath,
                text = ExportEngine.generateMarkdown(
                    blocks = blocks,
                    noteTitlesById = linkedNoteTitlesById,
                    categoryNamesById = categoryNamesById,
                    attachmentPathsByStoredName = rootAttachmentPathsByStoredName.mapValues { (_, path) -> stepsToVaultRoot + path },
                    personNamesById = personNamesLinkedIn(blocks, repository::getNoteById),
                    canvasPathsByNoteId = canvasPathsByNoteId,
                    vaultLinkTargetsById = plan.linkTargetsById
                )
            )
        }
        val standaloneCanvasFiles = standaloneCanvases.map { canvas ->
            MarkdownVaultTextFile(
                path = plan.notePathsById.getValue(canvas.canvasNoteId),
                text = ObsidianCanvasWriter.write(canvas.content, rootAttachmentPathsByStoredName)
            )
        }
        val embeddedCanvasFiles = canvasFiles.map { MarkdownVaultTextFile(it.pathInExport, it.canvasJson) }

        MarkdownVault(
            folderPaths = plan.folderPaths,
            textFiles = noteFiles + standaloneCanvasFiles + embeddedCanvasFiles,
            attachments = attachments
        )
    }

    private suspend fun blocksForExport(noteId: String): List<NoteBlock> =
        repository.getNoteContent(noteId)?.blocks.orEmpty()
            .withDatabasesAsTables(repository)
            .withWatchlistsExpanded(repository)

    private suspend fun embeddedCanvasesIn(blocks: List<NoteBlock>): List<ExportedCanvas> =
        blocks.filterIsInstance<CanvasBlock>()
            .filter { !it.isDeleted }
            .map { it.canvasNoteId }
            .distinct()
            .mapNotNull { canvasNoteId ->
                val canvasNote = repository.getNoteById(canvasNoteId)?.takeIf { it.isSubNote } ?: return@mapNotNull null
                ExportedCanvas(canvasNoteId, canvasNote.title, canvasRepository.loadCanvasIncludingDeleted(canvasNoteId))
            }

    private suspend fun linkedNoteTitlesIn(blocks: List<NoteBlock>): Map<String, String> =
        blocks.filterIsInstance<LinkedNoteBlock>()
            .map { it.linkedNoteId }
            .distinct()
            .mapNotNull { noteId -> repository.getNoteById(noteId)?.let { note -> noteId to note.title.ifBlank { "Untitled" } } }
            .toMap()
}
