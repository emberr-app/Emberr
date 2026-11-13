package com.emberr.domain.util.export

import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.SpaceEntity
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.TableBlock
import com.emberr.domain.model.TextBlock
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownVaultTest {

    private fun note(id: String, title: String, spaceId: String, folderId: String? = null, createdAt: Long, kind: NoteKind = NoteKind.NOTE) =
        NoteMetadataEntity(
            noteId = id,
            title = title,
            folderId = folderId,
            isDaily = false,
            dateString = null,
            createdAt = createdAt,
            updatedAt = createdAt,
            filePath = "",
            spaceId = spaceId,
            kind = kind
        )

    @Test
    fun notesGetSafeUniqueFilePathsAndLinksUseThePathOnlyWhenTheNameIsShared() {
        val spaces = listOf(
            SpaceEntity(spaceId = "personal", displayName = "Personal", createdAt = 1L),
            SpaceEntity(spaceId = "work", displayName = "Work", createdAt = 2L)
        )
        val folders = listOf(
            FolderEntity(folderId = "projects", name = "Projects", parentFolderId = null, createdAt = 1L, spaceId = "work"),
            FolderEntity(folderId = "version", name = "v1.2", parentFolderId = "projects", createdAt = 2L, spaceId = "work")
        )
        val notes = listOf(
            note("plan", "Plan", "personal", createdAt = 1L),
            note("plan-copy", "Plan", "personal", createdAt = 2L),
            note("work-plan", "Plan", "work", folderId = "projects", createdAt = 3L),
            note("ideas", "Ideas: big/small", "work", folderId = "version", createdAt = 4L),
            note("board", "Board", "personal", createdAt = 5L, kind = NoteKind.CANVAS),
            note("untitled", "", "personal", createdAt = 6L)
        )

        val plan = MarkdownVaultPaths.plan(spaces, folders, notes)

        assertEquals(listOf("Personal", "Work", "Work/Projects", "Work/Projects/v1.2"), plan.folderPaths)
        assertEquals(
            mapOf(
                "plan" to "Personal/Plan.md",
                "plan-copy" to "Personal/Plan 2.md",
                "work-plan" to "Work/Projects/Plan.md",
                "ideas" to "Work/Projects/v1.2/Ideas- big-small.md",
                "board" to "Personal/Board.canvas",
                "untitled" to "Personal/Untitled.md"
            ),
            plan.notePathsById
        )
        assertEquals(
            mapOf(
                "plan" to "Personal/Plan",
                "plan-copy" to "Plan 2",
                "work-plan" to "Work/Projects/Plan",
                "ideas" to "Ideas- big-small",
                "board" to "Board.canvas",
                "untitled" to "Untitled"
            ),
            plan.linkTargetsById
        )
    }

    @Test
    fun mentionsAndLinkedNotesBecomeWikiLinksInsideTheVaultAndPlainTextOutsideIt() {
        val markdown = ExportEngine.generateMarkdown(
            blocks = listOf(
                TextBlock(id = "text", text = "See [Plan](emberr://note/plan) and [Bob](emberr://note/person)"),
                LinkedNoteBlock(id = "linked-in-vault", linkedNoteId = "plan-copy"),
                LinkedNoteBlock(id = "linked-outside", linkedNoteId = "daily"),
                TableBlock(id = "table", rows = listOf(listOf("Link"), listOf("[Plan](emberr://note/plan)")))
            ),
            noteTitlesById = mapOf("plan-copy" to "Plan", "daily" to "Morning pages"),
            vaultLinkTargetsById = mapOf("plan" to "Personal/Plan", "plan-copy" to "Plan 2")
        )

        assertEquals(
            "See [[Personal/Plan|Plan]] and Bob\n\n[[Plan 2|Plan]]\n\nMorning pages\n\n| Link |\n| --- |\n| [[Personal/Plan\\|Plan]] |",
            markdown
        )
    }
}
