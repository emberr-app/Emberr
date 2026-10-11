package com.emberr.domain.importer

import okio.Buffer
import okio.Source
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportSummaryTest {

    private class FakeImportFile(override val relativePath: String) : ImportFile {
        override val sizeBytes: Long = 0L
        override val lastModifiedAt: Long? = null
        override suspend fun readBytes(): ByteArray = ByteArray(0)
        override fun openSource(): Source = Buffer()
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
    fun aCanvasCountsAsANoteAndItsFolderIsCounted() {
        val summary = ImportSummary.of(pickedFiles(listOf("Boards/Roadmap.canvas", "plan.md")))

        assertEquals(2, summary.noteCount)
        assertEquals(1, summary.folderCount)
        assertEquals(0, summary.attachmentCount)
    }

    @Test
    fun aZipWithoutNotesHasNoNotesAndNoFolders() {
        val summary = ImportSummary.of(pickedFiles(listOf("Photos/beach.png")))

        assertEquals(0, summary.noteCount)
        assertEquals(0, summary.folderCount)
        assertEquals(1, summary.attachmentCount)
    }
}
