package com.emberr.domain.util.export

import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.SpaceEntity

private const val MARKDOWN_EXTENSION = "md"
private const val CANVAS_EXTENSION = "canvas"
private const val UNTITLED_NAME = "Untitled"
private const val ATTACHMENTS_FOLDER_NAME = "attachments"
private const val MAX_NAME_LENGTH = 120

data class MarkdownVaultPlan(
    val folderPaths: List<String>,
    val notePathsById: Map<String, String>,
    val linkTargetsById: Map<String, String>
)

object MarkdownVaultPaths {

    private val charactersObsidianCannotUse = Regex("""[\\/:*?"<>|#^\[\]\p{Cntrl}]""")
    private val whitespaceRun = Regex("\\s+")

    fun plan(spaces: List<SpaceEntity>, folders: List<FolderEntity>, notes: List<NoteMetadataEntity>): MarkdownVaultPlan {
        val usedNamesByFolderPath = mutableMapOf("" to mutableSetOf(ATTACHMENTS_FOLDER_NAME))
        fun claimName(parentPath: String, wantedStem: String, extension: String? = null): String {
            val usedNames = usedNamesByFolderPath.getOrPut(parentPath) { mutableSetOf() }
            val name = uniqueNameOf(wantedStem, extension, usedNames)
            usedNames += name.lowercase()
            return name
        }
        fun childPath(parentPath: String, name: String): String = if (parentPath.isEmpty()) name else "$parentPath/$name"

        val folderPaths = mutableListOf<String>()
        val spaceRootPathsById = spaces.associate { space ->
            val rootPath = if (spaces.size > 1) childPath("", claimName("", safeNameOf(space.displayName))) else ""
            if (rootPath.isNotEmpty()) folderPaths += rootPath
            space.spaceId to rootPath
        }

        val foldersById = folders.associateBy { it.folderId }
        val folderPathsById = mutableMapOf<String, String>()
        fun pathOfFolder(folder: FolderEntity, foldersBeingPlaced: Set<String>): String? {
            folderPathsById[folder.folderId]?.let { return it }
            val spaceRootPath = spaceRootPathsById[folder.spaceId] ?: return null
            val parent = folder.parentFolderId?.let(foldersById::get)?.takeIf { it.spaceId == folder.spaceId && it.folderId !in foldersBeingPlaced }
            val parentPath = parent?.let { pathOfFolder(it, foldersBeingPlaced + folder.folderId) } ?: spaceRootPath
            val path = childPath(parentPath, claimName(parentPath, safeNameOf(folder.name)))
            folderPathsById[folder.folderId] = path
            folderPaths += path
            return path
        }
        folders.sortedWith(compareBy<FolderEntity> { it.sortOrder == 0 }.thenBy { it.sortOrder }.thenBy { it.createdAt })
            .forEach { folder -> pathOfFolder(folder, emptySet()) }

        val notePathsById = linkedMapOf<String, String>()
        notes.sortedBy { it.createdAt }.forEach { note ->
            val spaceRootPath = spaceRootPathsById[note.spaceId] ?: return@forEach
            val folderPath = note.folderId
                ?.let(foldersById::get)
                ?.takeIf { it.spaceId == note.spaceId }
                ?.let { folderPathsById[it.folderId] }
                ?: spaceRootPath
            val extension = if (note.kind == NoteKind.CANVAS) CANVAS_EXTENSION else MARKDOWN_EXTENSION
            notePathsById[note.noteId] = childPath(folderPath, claimName(folderPath, safeNameOf(note.title), extension))
        }

        return MarkdownVaultPlan(
            folderPaths = folderPaths,
            notePathsById = notePathsById,
            linkTargetsById = linkTargetsOf(notePathsById)
        )
    }

    private fun linkTargetsOf(notePathsById: Map<String, String>): Map<String, String> {
        val linkPathsById = notePathsById.mapValues { (_, path) -> path.removeSuffix(".$MARKDOWN_EXTENSION") }
        val countsByLinkName = linkPathsById.values.groupingBy { it.substringAfterLast('/').lowercase() }.eachCount()
        return linkPathsById.mapValues { (_, linkPath) ->
            val linkName = linkPath.substringAfterLast('/')
            if (countsByLinkName.getValue(linkName.lowercase()) == 1) linkName else linkPath
        }
    }

    private fun safeNameOf(name: String): String = name
        .replace(charactersObsidianCannotUse, "-")
        .replace(whitespaceRun, " ")
        .trim()
        .trim('.')
        .trim()
        .take(MAX_NAME_LENGTH)
        .trim()
        .ifEmpty { UNTITLED_NAME }

    private fun uniqueNameOf(stem: String, extension: String?, usedLowercaseNames: Set<String>): String {
        fun nameWith(copyNumber: Int?): String {
            val numberedStem = if (copyNumber == null) stem else "$stem $copyNumber"
            return if (extension == null) numberedStem else "$numberedStem.$extension"
        }
        if (nameWith(null).lowercase() !in usedLowercaseNames) return nameWith(null)
        var copyNumber = 2
        while (nameWith(copyNumber).lowercase() in usedLowercaseNames) copyNumber++
        return nameWith(copyNumber)
    }
}
