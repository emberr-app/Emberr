package com.emberr.domain.importer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DesktopImportFileReader : ImportFileReader {

    override suspend fun readPickedFiles(pickedPaths: List<String>): PickedImportFiles = withContext(Dispatchers.IO) {
        val pickedFiles = pickedPaths.map(::File)
        val collector = ImportFilesCollector()
        pickedFiles.forEach { file -> collector.addPickedFile(file.name, file) }
        collector.finish(pickedNames = pickedFiles.map { file -> file.name })
    }
}
