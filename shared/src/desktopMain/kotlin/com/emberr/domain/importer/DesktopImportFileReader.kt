package com.emberr.domain.importer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DesktopImportFileReader : ImportFileReader {

    override suspend fun readPickedFiles(pickedPaths: List<String>): PickedImportFiles = withContext(Dispatchers.IO) {
        val pickedFiles = pickedPaths.map(::File)
        val pickedNames = pickedFiles.map { file -> file.name }
        val collector = ImportFilesCollector()
        try {
            pickedFiles.forEach { file -> collector.addPickedFile(file.name, file) }
            collector.finish(pickedNames)
        } catch (error: Throwable) {
            collector.finish(pickedNames).close()
            throw error
        }
    }
}
