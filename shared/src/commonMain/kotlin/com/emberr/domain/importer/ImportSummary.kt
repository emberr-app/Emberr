package com.emberr.domain.importer

data class ImportSummary(
    val pickedNames: List<String>,
    val noteCount: Int,
    val folderCount: Int,
    val attachmentCount: Int,
    val skippedNames: List<String>
) {
    companion object {
        fun of(pickedFiles: PickedImportFiles): ImportSummary {
            val relativePaths = pickedFiles.files.map { file -> file.relativePath }
            val noteCount = relativePaths.count(ImportPaths::isNoteFile)
            return ImportSummary(
                pickedNames = pickedFiles.pickedNames,
                noteCount = noteCount,
                folderCount = ImportPaths.folderPathsHoldingNotes(relativePaths).size,
                attachmentCount = relativePaths.size - noteCount,
                skippedNames = pickedFiles.skippedNames
            )
        }
    }
}
