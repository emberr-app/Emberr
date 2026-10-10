package com.emberr.domain.importer

import kotlin.test.Test
import kotlin.test.assertEquals

class ImportSummaryTest {

    private class FakeImportFile(override val relativePath: String) : ImportFile {
        override suspend fun readBytes(): ByteArray = ByteArray(0)
    }

    private fun pickedFiles(relativePaths: List<String>, skippedNames: List<String> = emptyList()) =
        PickedImportFiles(
            pickedNames = listOf("vault.zip"),
            files = relativePaths.map(::FakeImportFile),
            skippedNames = skippedNames,
            releaseResources = {}
        )

    @Test
    fun notesFoldersAndAttachmentsAreCounted() {
        val summary = ImportSummary.of(
            pickedFiles(
                relativePaths = listOf(
                    "Work/Projects/plan.md",
                    "Work/ideas.markdown",
                    "journal.txt",
                    "Work/photo.png",
                    "assets/diagram.pdf"
                ),
                skippedNames = listOf("song.mp3")
            )
        )

        assertEquals(
            ImportSummary(
                pickedNames = listOf("vault.zip"),
                noteCount = 3,
                folderCount = 2,
                attachmentCount = 2,
                skippedNames = listOf("song.mp3")
            ),
            summary
        )
    }

    @Test
    fun aZipWithoutNotesHasNoNotesAndNoFolders() {
        val summary = ImportSummary.of(pickedFiles(listOf("Photos/beach.png")))

        assertEquals(0, summary.noteCount)
        assertEquals(0, summary.folderCount)
        assertEquals(1, summary.attachmentCount)
    }
}
