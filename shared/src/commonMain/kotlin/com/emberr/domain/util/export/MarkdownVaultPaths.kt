package com.emberr.domain.util.export

import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.SpaceEntity

private const val MARKDOWN_EXTENSION = "md"
private const val CANVAS_EXTENSION = "canvas"
private const val MAX_NAME_LENGTH = 120
private const val MAX_PATH_LENGTH = 240
private const val MIN_SHORTENED_NAME_LENGTH = 20
private const val ROOM_FOR_COPY_NUMBER = 4

private val namesWindowsCannotUse = setOf("con", "prn", "aux", "nul") + (1..9).flatMap { number -> listOf("com$number", "lpt$number") }

data class MarkdownVaultPlan(
    val folderPaths: List<String>,
    val notePathsById: Map<String, String>,
    val linkTargetsById: Map<String, String>
)

object MarkdownVaultPaths {

    private val charactersObsidianCannotUse = Regex("""[\\/:*?"<>|#^\[\]\p{Cntrl}]""")
    private val whitespaceRun = Regex("\\s+")

    fun plan(spaces: List<SpaceEntity>, folders: List<FolderEntity>, notes: List<NoteMetadataEntity>): MarkdownVaultPlan {
        val names = UniqueNamesPerFolder()
        val spaceRootPathsById = spaceRootPathsOf(spaces, names)
        val folderPathsById = folderPathsOf(folders, spaceRootPathsById, names)
        val notePathsById = notePathsOf(notes, folders, spaceRootPathsById, folderPathsById, names)
        return MarkdownVaultPlan(
            folderPaths = spaceRootPathsById.values.filter { it.isNotEmpty() } + folderPathsById.values,
            notePathsById = notePathsById,
            linkTargetsById = linkTargetsOf(notePathsById)
        )
    }

    private fun spaceRootPathsOf(spaces: List<SpaceEntity>, names: UniqueNamesPerFolder): Map<String, String> =
        spaces.associate { space ->
            val rootPath = if (spaces.size > 1) names.claimChild(folderPath = "", wantedStem = safeNameOf(space.displayName)) else ""
            space.spaceId to rootPath
        }

    private fun folderPathsOf(
        folders: List<FolderEntity>,
        spaceRootPathsById: Map<String, String>,
        names: UniqueNamesPerFolder
    ): Map<String, String> {
        val foldersById = folders.associateBy { it.folderId }
        val childFoldersByParentId = folders
            .sortedWith(compareBy<FolderEntity> { it.sortOrder == 0 }.thenBy { it.sortOrder }.thenBy { it.createdAt })
            .groupBy { folder -> folder.parentFolderId?.let(foldersById::get)?.takeIf { it.spaceId == folder.spaceId }?.folderId }
        val folderPathsById = linkedMapOf<String, String>()

        fun placeWithChildren(folder: FolderEntity, parentPath: String) {
            val path = names.claimChild(folderPath = parentPath, wantedStem = safeNameOf(folder.name))
            folderPathsById[folder.folderId] = path
            childFoldersByParentId[folder.folderId].orEmpty().forEach { child -> placeWithChildren(child, path) }
        }

        for (topFolder in childFoldersByParentId[null].orEmpty()) {
            val spaceRootPath = spaceRootPathsById[topFolder.spaceId] ?: continue
            placeWithChildren(topFolder, spaceRootPath)
        }
        return folderPathsById
    }

    private fun notePathsOf(
        notes: List<NoteMetadataEntity>,
        folders: List<FolderEntity>,
        spaceRootPathsById: Map<String, String>,
        folderPathsById: Map<String, String>,
        names: UniqueNamesPerFolder
    ): Map<String, String> {
        val spaceIdsByFolderId = folders.associate { it.folderId to it.spaceId }
        val notePathsById = linkedMapOf<String, String>()
        for (note in notes.sortedBy { it.createdAt }) {
            val spaceRootPath = spaceRootPathsById[note.spaceId] ?: continue
            val folderPath = note.folderId
                ?.takeIf { folderId -> spaceIdsByFolderId[folderId] == note.spaceId }
                ?.let(folderPathsById::get)
                ?: spaceRootPath
            val extension = if (note.kind == NoteKind.CANVAS) CANVAS_EXTENSION else MARKDOWN_EXTENSION
            notePathsById[note.noteId] = names.claimChild(folderPath = folderPath, wantedStem = safeNameOf(note.title), extension = extension)
        }
        return notePathsById
    }

    private fun linkTargetsOf(notePathsById: Map<String, String>): Map<String, String> {
        val linkPathsById = notePathsById.mapValues { (_, path) -> path.removeSuffix(".$MARKDOWN_EXTENSION") }
        val countsByLinkName = linkPathsById.values.groupingBy { it.substringAfterLast('/').lowercase() }.eachCount()
        return linkPathsById.mapValues { (_, linkPath) ->
            val linkName = linkPath.substringAfterLast('/')
            if (countsByLinkName.getValue(linkName.lowercase()) == 1) linkName else linkPath
        }
    }

    private fun safeNameOf(name: String): String {
        val shortName = withoutCharactersObsidianCannotUse(name).take(MAX_NAME_LENGTH).trim()
        return when {
            shortName.isEmpty() -> UNTITLED_NOTE_TITLE
            shortName.lowercase() in namesWindowsCannotUse -> "$shortName-"
            else -> shortName
        }
    }

    private fun withoutCharactersObsidianCannotUse(name: String): String = name
        .replace(charactersObsidianCannotUse, "-")
        .replace(whitespaceRun, " ")
        .trim()
        .trim('.')
        .trim()

    private class UniqueNamesPerFolder {

        private val usedNamesByFolderPath = mutableMapOf("" to mutableSetOf(MARKDOWN_ATTACHMENTS_FOLDER))

        fun claimChild(folderPath: String, wantedStem: String, extension: String? = null): String {
            val usedNames = usedNamesByFolderPath.getOrPut(folderPath) { mutableSetOf() }
            val name = uniqueNoteFileNameOf(shortenedToFitPath(wantedStem, folderPath, extension), extension, usedNames)
            usedNames += name.lowercase()
            return if (folderPath.isEmpty()) name else "$folderPath/$name"
        }

        private fun shortenedToFitPath(stem: String, folderPath: String, extension: String?): String {
            val extensionLength = extension?.let { it.length + 1 } ?: 0
            val roomForStem = MAX_PATH_LENGTH - folderPath.length - 1 - extensionLength - ROOM_FOR_COPY_NUMBER
            if (stem.length <= roomForStem) return stem
            return stem.take(maxOf(roomForStem, MIN_SHORTENED_NAME_LENGTH)).trim()
        }

        private fun uniqueNoteFileNameOf(stem: String, extension: String?, usedLowercaseNames: Set<String>): String {
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
}
