package com.emberr.domain.importer

data class ImportSummary(
    val pickedNames: List<String>,
    val noteCount: Int,
    val folderCount: Int,
    val attachmentCount: Int,
    val skippedNames: List<String>
) {
    companion object {
        suspend fun of(pickedFiles: PickedImportFiles): ImportSummary {
            val noteFiles = pickedFiles.files.filter { file -> ImportPaths.isNoteFile(file.relativePath) }
            val documentLinkedNotePaths = DocumentLinkedNoteFiles.pathsLinkedOnlyAsDocuments(
                DocumentLinkedNoteFiles.readableMarkdownTextsOf(noteFiles)
            )
            val notePaths = noteFiles.map { file -> file.relativePath } - documentLinkedNotePaths
            return ImportSummary(
                pickedNames = pickedFiles.pickedNames,
                noteCount = notePaths.size,
                folderCount = ImportPaths.folderPathsHoldingNotes(notePaths).size,
                attachmentCount = pickedFiles.files.size - notePaths.size,
                skippedNames = pickedFiles.skippedNames
            )
        }
    }
}
