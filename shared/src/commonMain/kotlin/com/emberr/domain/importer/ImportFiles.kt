package com.emberr.domain.importer

import okio.Source

interface ImportFile {
    val relativePath: String
    val sizeBytes: Long
    val lastModifiedAt: Long?
    suspend fun readBytes(): ByteArray
    fun openSource(): Source
}

class PickedImportFiles(
    val pickedNames: List<String>,
    val files: List<ImportFile>,
    val skippedNames: List<String>,
    private val releaseResources: () -> Unit
) : AutoCloseable {
    override fun close() = releaseResources()
}

interface ImportFileReader {
    suspend fun readPickedFiles(pickedPaths: List<String>): PickedImportFiles
}

interface ImportFolderReader {
    suspend fun readPickedFolder(folderLocation: String): PickedImportFiles
}
