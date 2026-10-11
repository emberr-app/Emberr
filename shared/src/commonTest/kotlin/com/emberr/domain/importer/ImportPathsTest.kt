package com.emberr.domain.importer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportPathsTest {

    @Test
    fun markdownAndTextFilesAreNotes() {
        assertTrue(ImportPaths.isNoteFile("plan.md"))
        assertTrue(ImportPaths.isNoteFile("Work/IDEAS.MARKDOWN"))
        assertTrue(ImportPaths.isNoteFile("journal.txt"))
        assertFalse(ImportPaths.isNoteFile("photo.png"))
        assertFalse(ImportPaths.isNoteFile("README"))
    }

    @Test
    fun obsidianCanvasesAreNotes() {
        assertTrue(ImportPaths.isNoteFile("Boards/Roadmap.canvas"))
        assertTrue(ImportPaths.isCanvasFile("Boards/Roadmap.CANVAS"))
        assertFalse(ImportPaths.isCanvasFile("plan.md"))
        assertTrue(ImportPaths.isSupportedPickedFile("Roadmap.canvas"))
    }

    @Test
    fun imageFilesAreRecognisedByTheirExtension() {
        assertTrue(ImportPaths.isImageFile("assets/Photo.PNG"))
        assertTrue(ImportPaths.isImageFile("scan.jpeg"))
        assertFalse(ImportPaths.isImageFile("diagram.svg"))
        assertFalse(ImportPaths.isImageFile("report.pdf"))
        assertFalse(ImportPaths.isImageFile("png"))
    }

    @Test
    fun theFolderOfAPathIsEverythingBeforeTheFileName() {
        assertEquals("Work/Projects", ImportPaths.folderOf("Work/Projects/plan.md"))
        assertEquals("", ImportPaths.folderOf("plan.md"))
    }

    @Test
    fun aNoteTitleIsTheFileNameWithoutItsExtension() {
        assertEquals("Roadmap", ImportPaths.noteTitleOf("Boards/Roadmap.canvas"))
        assertEquals("v1.2 notes", ImportPaths.noteTitleOf("v1.2 notes.md"))
        assertEquals("Plan", ImportPaths.noteTitleOf("  Plan  .md"))
    }

    @Test
    fun aBlankFileNameBecomesUntitled() {
        assertEquals("Untitled", ImportPaths.noteTitleOf(" .canvas"))
        assertEquals("Untitled", ImportPaths.noteTitleOf("Boards/.md"))
    }

    @Test
    fun excalidrawDrawingsAreRecognisedAndCannotBePicked() {
        assertTrue(ImportPaths.isUnsupportedObsidianFile("Drawings/Sketch.excalidraw.md"))
        assertTrue(ImportPaths.isUnsupportedObsidianFile("Sketch.EXCALIDRAW"))
        assertFalse(ImportPaths.isUnsupportedObsidianFile("excalidraw notes.md"))
        assertFalse(ImportPaths.isSupportedPickedFile("Sketch.excalidraw.md"))
        assertFalse(ImportPaths.isSupportedPickedFile("Sketch.excalidraw"))
    }

    @Test
    fun obsidianBasesAreRecognisedAndCannotBePicked() {
        assertTrue(ImportPaths.isUnsupportedObsidianFile("Views/Books.base"))
        assertFalse(ImportPaths.isUnsupportedObsidianFile("Database notes.md"))
        assertFalse(ImportPaths.isSupportedPickedFile("Books.base"))
    }

    @Test
    fun onlyNotesAndZipsCanBePicked() {
        assertTrue(ImportPaths.isSupportedPickedFile("vault.ZIP"))
        assertTrue(ImportPaths.isSupportedPickedFile("plan.md"))
        assertFalse(ImportPaths.isSupportedPickedFile("photo.png"))
    }

    @Test
    fun folderEntriesAndSystemJunkAreIgnored() {
        assertTrue(ImportPaths.isIgnoredEntry("Work/"))
        assertTrue(ImportPaths.isIgnoredEntry("__MACOSX/Work/._plan.md"))
        assertTrue(ImportPaths.isIgnoredEntry("Work/.DS_Store"))
        assertTrue(ImportPaths.isIgnoredEntry(".obsidian/workspace.json"))
        assertTrue(ImportPaths.isIgnoredEntry("Work\\Thumbs.db"))
        assertFalse(ImportPaths.isIgnoredEntry("Work/plan.md"))
    }

    @Test
    fun anEntryThatClimbsOutOfTheImportIsNotIgnoredSoItCanBeReportedAsSkipped() {
        assertFalse(ImportPaths.isIgnoredEntry("../plan.md"))
    }

    @Test
    fun aSafePathIsCleanedUp() {
        assertEquals("Work/plan.md", ImportPaths.safeRelativePath("Work/plan.md"))
        assertEquals("Work/plan.md", ImportPaths.safeRelativePath("./Work//plan.md"))
        assertEquals("Work/Projects/plan.md", ImportPaths.safeRelativePath("Work\\Projects\\plan.md"))
    }

    @Test
    fun aPathThatLeavesTheImportIsRefused() {
        assertNull(ImportPaths.safeRelativePath("../plan.md"))
        assertNull(ImportPaths.safeRelativePath("Work/../../plan.md"))
        assertNull(ImportPaths.safeRelativePath("/etc/plan.md"))
        assertNull(ImportPaths.safeRelativePath("C:/Users/plan.md"))
        assertNull(ImportPaths.safeRelativePath(""))
    }

    @Test
    fun onlyFoldersThatHoldNotesAreCounted() {
        val folders = ImportPaths.folderPathsHoldingNotes(
            listOf("Work/Projects/plan.md", "Work/ideas.md", "assets/photo.png", "journal.md")
        )

        assertEquals(setOf("Work", "Work/Projects"), folders)
    }
}
