package com.emberr.domain.importer

import kotlinx.coroutines.test.runTest
import okio.Buffer
import okio.Source
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportSummaryTest {

    private class FakeImportFile(override val relativePath: String, private val text: String = "") : ImportFile {
        override val sizeBytes: Long = 0L
        override val lastModifiedAt: Long? = null
        override suspend fun readBytes(): ByteArray = text.encodeToByteArray()
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
    fun notesFoldersAndAttachmentsAreCounted() = runTest {
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
    fun aCanvasCountsAsANoteAndItsFolderIsCounted() = runTest {
        val summary = ImportSummary.of(pickedFiles(listOf("Boards/Roadmap.canvas", "plan.md")))

        assertEquals(2, summary.noteCount)
        assertEquals(1, summary.folderCount)
        assertEquals(0, summary.attachmentCount)
    }

    @Test
    fun aZipWithoutNotesHasNoNotesAndNoFolders() = runTest {
        val summary = ImportSummary.of(pickedFiles(listOf("Photos/beach.png")))

        assertEquals(0, summary.noteCount)
        assertEquals(0, summary.folderCount)
        assertEquals(1, summary.attachmentCount)
    }

    @Test
    fun aMarkdownFileLinkedOnlyAsAFileCountsAsAnAttachment() = runTest {
        val summary = ImportSummary.of(
            PickedImportFiles(
                pickedNames = listOf("vault.zip"),
                files = listOf(
                    FakeImportFile("Home.md", text = "See [Readme.md](Docs/Readme.md)"),
                    FakeImportFile("Docs/Readme.md")
                ),
                skippedNames = emptyList(),
                releaseResources = {}
            )
        )

        assertEquals(1, summary.noteCount)
        assertEquals(0, summary.folderCount)
        assertEquals(1, summary.attachmentCount)
    }
}
