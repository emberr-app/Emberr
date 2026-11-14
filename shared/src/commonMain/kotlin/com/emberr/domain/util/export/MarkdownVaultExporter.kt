package com.emberr.domain.util.export

import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.canvas.CanvasRepository
import com.emberr.domain.database.withDatabasesAsTables
import com.emberr.domain.model.CanvasBlock
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

private data class VaultWideDetails(
    val attachmentPathsFromVaultRoot: Map<String, String>,
    val categoryNamesById: Map<String, String>,
    val linkedNoteTitlesById: Map<String, String>,
    val canvasPathsByNoteId: Map<String, String>,
    val vaultLinks: VaultLinks
)

class MarkdownVaultExporter(
    private val repository: NoteRepository,
    private val canvasRepository: CanvasRepository,
    private val spaceRepository: SpaceRepository
) {

    suspend fun build(spaceIds: Set<String>): MarkdownVault = withContext(Dispatchers.IO) {
        val notes = notesIn(spaceIds)
        val plan = MarkdownVaultPaths.plan(spacesIn(spaceIds), foldersIn(spaceIds), notes)

        val textNotes = notes.filter { it.kind == NoteKind.NOTE && it.noteId in plan.notePathsById }
        val blocksByNoteId = textNotes.associate { note -> note.noteId to blocksForExport(note.noteId) }
        val allBlocks = blocksByNoteId.values.flatten()
        val embeddedCanvases = embeddedCanvasesIn(allBlocks)
        val standaloneCanvases = standaloneCanvasesIn(notes, plan)
        val attachments = MarkdownAttachments.collect(allBlocks, embeddedCanvases + standaloneCanvases)
        val embeddedCanvasFiles = MarkdownAttachments.canvasFilesFor(embeddedCanvases, attachments)

        val details = VaultWideDetails(
            attachmentPathsFromVaultRoot = attachments.associate { it.storedFileName to it.pathInExport },
            categoryNamesById = categoryNamesById(),
            linkedNoteTitlesById = linkedNoteTitlesIn(allBlocks, repository::getNoteById),
            canvasPathsByNoteId = embeddedCanvasFiles.associate { it.canvasNoteId to it.pathInExport } +
                standaloneCanvases.associate { it.canvasNoteId to plan.notePathsById.getValue(it.canvasNoteId) },
            vaultLinks = VaultLinks(plan.linkTargetsById)
        )

        val noteFiles = textNotes.map { note ->
            noteFileFor(plan.notePathsById.getValue(note.noteId), blocksByNoteId.getValue(note.noteId), details)
        }
        val standaloneCanvasFiles = standaloneCanvases.map { canvas ->
            MarkdownVaultTextFile(
                path = plan.notePathsById.getValue(canvas.canvasNoteId),
                text = ObsidianCanvasWriter.write(canvas.content, details.attachmentPathsFromVaultRoot)
            )
        }
        val embeddedCanvasTextFiles = embeddedCanvasFiles.map { MarkdownVaultTextFile(it.pathInExport, it.canvasJson) }

        MarkdownVault(
            folderPaths = plan.folderPaths,
            textFiles = noteFiles + standaloneCanvasFiles + embeddedCanvasTextFiles,
            attachments = attachments
        )
    }

    private suspend fun spacesIn(spaceIds: Set<String>): List<SpaceEntity> =
        spaceRepository.observeSpaces().first().filter { it.spaceId in spaceIds && !it.isDeleted }

    private suspend fun foldersIn(spaceIds: Set<String>): List<FolderEntity> =
        repository.getAllFoldersAcrossSpaces().filter { it.spaceId in spaceIds }

    private suspend fun notesIn(spaceIds: Set<String>): List<NoteMetadataEntity> =
        repository.getAllNotesAcrossSpaces().filter { note ->
            note.spaceId in spaceIds && note.databaseId == null && (note.kind == NoteKind.NOTE || note.kind == NoteKind.CANVAS)
        }

    private suspend fun categoryNamesById(): Map<String, String> =
        repository.getAllCategories().first()
            .filter { !it.isDeleted }
            .associate { it.categoryId to it.name }

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

    private suspend fun standaloneCanvasesIn(notes: List<NoteMetadataEntity>, plan: MarkdownVaultPlan): List<ExportedCanvas> =
        notes
            .filter { it.kind == NoteKind.CANVAS && it.noteId in plan.notePathsById }
            .map { note -> ExportedCanvas(note.noteId, note.title, canvasRepository.loadCanvasIncludingDeleted(note.noteId)) }

    private suspend fun noteFileFor(notePath: String, blocks: List<NoteBlock>, details: VaultWideDetails): MarkdownVaultTextFile {
        val stepsUpToVaultRoot = PARENT_FOLDER_STEP.repeat(notePath.count { it == '/' })
        return MarkdownVaultTextFile(
            path = notePath,
            text = ExportEngine.generateMarkdown(
                blocks = blocks,
                noteTitlesById = details.linkedNoteTitlesById,
                categoryNamesById = details.categoryNamesById,
                attachmentPathsByStoredName = details.attachmentPathsFromVaultRoot.mapValues { (_, path) -> stepsUpToVaultRoot + path },
                personNamesById = personNamesLinkedIn(blocks, repository::getNoteById),
                canvasPathsByNoteId = details.canvasPathsByNoteId,
                vaultLinks = details.vaultLinks
            )
        )
    }
}
