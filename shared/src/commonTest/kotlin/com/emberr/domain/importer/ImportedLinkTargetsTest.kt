package com.emberr.domain.importer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImportedLinkTargetsTest {

    private val linkTargets = ImportedLinkTargets(
        noteIdsByPath = mapOf(
            "Vault/Work/Plan.md" to "plan",
            "Vault/Ideas.md" to "ideas",
            "Vault/A/Todo.md" to "todo-a",
            "Vault/B/Todo.md" to "todo-b"
        ),
        attachmentPaths = listOf("Vault/_resources/chart.png", "Vault/Work/files/report.pdf")
    )

    private val fromPlan = linkTargets.resolverFor("Vault/Work/Plan.md")

    @Test
    fun aLinkIsFirstReadFromTheNotesOwnFolder() {
        assertEquals("ideas", fromPlan.noteIdFor("../Ideas.md"))
        assertEquals("Vault/Work/files/report.pdf", fromPlan.attachmentPathFor("files/report.pdf"))
    }

    @Test
    fun aLinkCanAlsoStartFromTheTopOfTheImport() {
        assertEquals("ideas", fromPlan.noteIdFor("Vault/Ideas.md"))
    }

    @Test
    fun aNoteLinkWithoutItsExtensionStillMatches() {
        assertEquals("ideas", fromPlan.noteIdFor("../Ideas"))
    }

    @Test
    fun lettersInADifferentCaseStillMatch() {
        assertEquals("ideas", fromPlan.noteIdFor("../ideas.MD"))
    }

    @Test
    fun aWikiLinkMatchesANoteByItsName() {
        assertEquals("ideas", fromPlan.noteIdFor("Ideas"))
        assertEquals("Vault/_resources/chart.png", fromPlan.attachmentPathFor("chart.png"))
    }

    @Test
    fun aNameUsedTwiceMatchesTheOneInTheSameFolderFirst() {
        assertEquals("todo-b", linkTargets.resolverFor("Vault/B/Week.md").noteIdFor("Todo"))
        assertEquals("todo-a", linkTargets.resolverFor("Vault/A/Week.md").noteIdFor("Todo"))
    }

    @Test
    fun aCanvasIsOnlyMatchedWhenTheLinkNamesItsExtension() {
        val withCanvas = ImportedLinkTargets(
            noteIdsByPath = mapOf("Vault/Boards/Board.canvas" to "board-canvas"),
            attachmentPaths = emptyList()
        ).resolverFor("Vault/Plan.md")

        assertNull(withCanvas.noteIdFor("Board"))
        assertEquals("board-canvas", withCanvas.noteIdFor("Board.canvas"))
    }

    @Test
    fun aNoteAndACanvasWithTheSameNameAreToldApartByTheExtension() {
        val withBoth = ImportedLinkTargets(
            noteIdsByPath = mapOf("Vault/Board.md" to "board-note", "Vault/Board.canvas" to "board-canvas"),
            attachmentPaths = emptyList()
        ).resolverFor("Vault/Plan.md")

        assertEquals("board-note", withBoth.noteIdFor("Board"))
        assertEquals("board-canvas", withBoth.noteIdFor("Board.canvas"))
    }

    @Test
    fun notesAreNeverReturnedAsAttachmentsAndUnknownTargetsMatchNothing() {
        assertNull(fromPlan.attachmentPathFor("../Ideas.md"))
        assertNull(fromPlan.noteIdFor("Nowhere"))
        assertNull(fromPlan.attachmentPathFor("missing.png"))
    }
}
