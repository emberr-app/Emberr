package com.emberr.domain.importer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

class DesktopImportFileReader : ImportFileReader {

    override suspend fun readPickedFiles(pickedPaths: List<String>): PickedImportFiles {
        val pickedFiles = pickedPaths.map(::File)
        val pickedNames = pickedFiles.map { file -> file.name }
        val collector = ImportFilesCollector()
        try {
            withContext(Dispatchers.IO) {
                pickedFiles.forEach { file ->
                    ensureActive()
                    collector.addPickedFile(file.name, file)
                }
            }
        } catch (error: Throwable) {
            collector.finish(pickedNames).close()
            throw error
        }
        return collector.finish(pickedNames)
    }
}
