package com.emberr.domain.template

import com.emberr.data.local.room.entity.NoteKind
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.database.databaseCellBlockId
import com.emberr.domain.database.databaseNoteId
import com.emberr.domain.database.newDatabaseSettings
import com.emberr.domain.database.withColumnAdded
import com.emberr.domain.database.withSettingTimesStamped
import com.emberr.domain.model.BookmarkBlock
import com.emberr.domain.model.BulletedListBlock
import com.emberr.domain.model.CalloutBlock
import com.emberr.domain.model.CalloutType
import com.emberr.domain.model.CanvasBlock
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.CodeBlock
import com.emberr.domain.model.CredentialField
import com.emberr.domain.model.CredentialFieldKind
import com.emberr.domain.model.CredentialsBlock
import com.emberr.domain.model.DatabaseBlock
import com.emberr.domain.model.DatabaseColumnTarget
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.InlineSpan
import com.emberr.domain.model.LinkedNoteBlock
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.MovieDetails
import com.emberr.domain.model.MovieKind
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NoteContent
import com.emberr.domain.model.NumberedListBlock
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.PropertyType
import com.emberr.domain.model.QuoteBlock
import com.emberr.domain.model.SolidDividerBlock
import com.emberr.domain.model.TableBlock
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.ThreeDotDividerBlock
import com.emberr.domain.model.ToggleBlock
import com.emberr.domain.model.VoiceBlock
import com.emberr.domain.model.WatchStatus
import com.emberr.domain.model.WatchlistBlock
import com.emberr.domain.repository.NoteRepository
import com.emberr.domain.sample.STARTER_CONTENT_UPDATED_AT
import kotlinx.datetime.LocalDate

object AllBlocksTemplate : PredefinedTemplate {
    override val templateId: String = "predefined_template_all_blocks_debug"
    override val title: String = "All blocks (debug)"
    override val icon: String? = "🧪"

    private const val ID_PREFIX = "debug_all_blocks"
    private const val DATABASE_ID = "${ID_PREFIX}_database"
    private const val CANVAS_NOTE_ID = "${ID_PREFIX}_canvas"
    private const val WATCHLIST_NOTE_ID = "${ID_PREFIX}_watchlist"
    private const val DATABASE_TITLE = "Reading list"
    private const val CANVAS_TITLE = "Debug canvas"
    private const val STYLED_LINE = "Bold, italic, struck, underlined, highlighted and lime in one line"

    private val statusColumn = DatabaseColumnTarget.Property(PropertyType.STATUS)
    private val databaseRows = listOf("Dune" to "Done", "Piranesi" to "Doing")

    override fun buildContent(): NoteContent = NoteContent(blocks = textBlocks() + listBlocks() + mediaAndLinkBlocks() + structuredBlocks())

    override suspend fun seedSupportingNotes(repository: NoteRepository) {
        if (repository.getNoteById(databaseNoteId(DATABASE_ID)) == null) seedDatabase(repository)
        if (repository.getNoteById(CANVAS_NOTE_ID) == null) {
            saveStarterNote(repository, subNoteMetadata(CANVAS_NOTE_ID, CANVAS_TITLE, NoteKind.CANVAS), emptyList())
        }
        if (repository.getNoteById(WATCHLIST_NOTE_ID) == null) {
            saveStarterNote(repository, subNoteMetadata(WATCHLIST_NOTE_ID, ""), watchlistTitles())
        }
    }

    private fun blockId(name: String): String = "${ID_PREFIX}_$name"

    private fun textBlocks(): List<NoteBlock> = listOf(
        HeadingBlock(id = blockId("heading_1"), text = "Heading 1", level = 1, updatedAt = STARTER_CONTENT_UPDATED_AT),
        HeadingBlock(id = blockId("heading_2"), text = "Heading 2", level = 2, updatedAt = STARTER_CONTENT_UPDATED_AT),
        HeadingBlock(id = blockId("heading_3"), text = "Heading 3", level = 3, updatedAt = STARTER_CONTENT_UPDATED_AT),
        TextBlock(
            id = blockId("styled_text"),
            text = STYLED_LINE,
            inlineSpans = listOf(
                spanOf("Bold", bold = true),
                spanOf("italic", italic = true),
                spanOf("struck", strikeThrough = true),
                spanOf("underlined", underline = true),
                spanOf("highlighted", highlight = true),
                spanOf("lime", highlight = true, highlightColorName = "lime")
            ),
            updatedAt = STARTER_CONTENT_UPDATED_AT
        ),
        TextBlock(id = blockId("two_line_text"), text = "First line\nSecond line of the same block", updatedAt = STARTER_CONTENT_UPDATED_AT),
        QuoteBlock(id = blockId("quote"), text = "A quote\nover two lines", updatedAt = STARTER_CONTENT_UPDATED_AT),
        CalloutBlock(
            id = blockId("callout"),
            text = "Foldable tip callout",
            calloutTypeName = CalloutType.TIP.name,
            isFoldable = true,
            updatedAt = STARTER_CONTENT_UPDATED_AT
        ),
        TextBlock(id = blockId("callout_text"), text = "Text inside the callout", indentationLevel = 1, updatedAt = STARTER_CONTENT_UPDATED_AT),
        BulletedListBlock(id = blockId("callout_bullet"), text = "Bullet inside the callout", indentationLevel = 1, updatedAt = STARTER_CONTENT_UPDATED_AT),
        CodeBlock(
            id = blockId("code"),
            code = "fun greet(name: String) = \"Hi, \$name\"",
            language = "kotlin",
            updatedAt = STARTER_CONTENT_UPDATED_AT
        )
    )

    private fun listBlocks(): List<NoteBlock> = listOf(
        BulletedListBlock(id = blockId("bullet"), text = "Bullet", updatedAt = STARTER_CONTENT_UPDATED_AT),
        BulletedListBlock(id = blockId("nested_bullet"), text = "Nested bullet", indentationLevel = 1, updatedAt = STARTER_CONTENT_UPDATED_AT),
        TextBlock(id = blockId("text_under_bullet"), text = "Paragraph under a bullet", indentationLevel = 1, updatedAt = STARTER_CONTENT_UPDATED_AT),
        NumberedListBlock(id = blockId("number_1"), text = "First step", number = 1, updatedAt = STARTER_CONTENT_UPDATED_AT),
        NumberedListBlock(id = blockId("number_2"), text = "Second step", number = 2, updatedAt = STARTER_CONTENT_UPDATED_AT),
        TextBlock(id = blockId("text_under_number"), text = "Paragraph under a numbered item", indentationLevel = 1, updatedAt = STARTER_CONTENT_UPDATED_AT),
        NumberedListBlock(id = blockId("nested_number"), text = "Nested numbered item", number = 1, indentationLevel = 1, updatedAt = STARTER_CONTENT_UPDATED_AT),
        CheckboxBlock(
            id = blockId("task_open"),
            text = "Open task with a link and details",
            url = "https://example.com/task",
            description = "Task details",
            updatedAt = STARTER_CONTENT_UPDATED_AT
        ),
        CheckboxBlock(id = blockId("task_done"), text = "Finished task", isChecked = true, updatedAt = STARTER_CONTENT_UPDATED_AT),
        ToggleBlock(id = blockId("toggle"), text = "Collapsed toggle", isExpanded = false, updatedAt = STARTER_CONTENT_UPDATED_AT),
        TextBlock(id = blockId("toggle_child"), text = "Hidden until the toggle opens", indentationLevel = 1, updatedAt = STARTER_CONTENT_UPDATED_AT)
    )

    private fun mediaAndLinkBlocks(): List<NoteBlock> = listOf(
        SolidDividerBlock(id = blockId("solid_divider"), updatedAt = STARTER_CONTENT_UPDATED_AT),
        ImageBlock(id = blockId("image"), updatedAt = STARTER_CONTENT_UPDATED_AT),
        DocumentBlock(id = blockId("document"), updatedAt = STARTER_CONTENT_UPDATED_AT),
        VoiceBlock(id = blockId("voice"), updatedAt = STARTER_CONTENT_UPDATED_AT),
        BookmarkBlock(
            id = blockId("bookmark"),
            url = "https://example.com",
            title = "Example bookmark",
            description = "Bookmark description",
            updatedAt = STARTER_CONTENT_UPDATED_AT
        ),
        LinkedNoteBlock(id = blockId("linked_note"), linkedNoteId = CANVAS_NOTE_ID, updatedAt = STARTER_CONTENT_UPDATED_AT),
        ThreeDotDividerBlock(id = blockId("dot_divider"), updatedAt = STARTER_CONTENT_UPDATED_AT)
    )

    private fun structuredBlocks(): List<NoteBlock> = listOf(
        TableBlock(
            id = blockId("table"),
            rows = listOf(listOf("Name", "Note"), listOf("Ann", "Pipe | inside"), listOf("Ben", "Two\nlines")),
            updatedAt = STARTER_CONTENT_UPDATED_AT
        ),
        DatabaseBlock(id = blockId("database"), databaseId = DATABASE_ID, updatedAt = STARTER_CONTENT_UPDATED_AT),
        PropertyBlock(id = blockId("status_property"), propertyType = PropertyType.STATUS, tags = listOf("Doing"), updatedAt = STARTER_CONTENT_UPDATED_AT),
        PropertyBlock(id = blockId("date_property"), propertyType = PropertyType.DATE, date = LocalDate(2026, 11, 13), updatedAt = STARTER_CONTENT_UPDATED_AT),
        PersonDetailBlock(id = blockId("phone"), kind = PersonDetailKind.PHONE, label = "Work", value = "+1 555 0100", updatedAt = STARTER_CONTENT_UPDATED_AT),
        PersonDetailBlock(id = blockId("email"), kind = PersonDetailKind.EMAIL, value = "ann@example.com", updatedAt = STARTER_CONTENT_UPDATED_AT),
        CredentialsBlock(
            id = blockId("credentials"),
            serviceName = "Example service",
            loginUrl = "https://example.com/login",
            fields = listOf(
                CredentialField(id = blockId("credentials_username"), kind = CredentialFieldKind.USERNAME, label = "Username", plainValue = "debug-user"),
                CredentialField(id = blockId("credentials_password"), kind = CredentialFieldKind.PASSWORD, label = "Password")
            ),
            updatedAt = STARTER_CONTENT_UPDATED_AT
        ),
        movieBlock(blockId("movie"), tmdbId = 900_001L, kind = MovieKind.MOVIE, title = "Debug Movie", yearText = "2021", status = WatchStatus.WATCHED, ratingHalfStars = 9),
        WatchlistBlock(id = blockId("watchlist"), watchlistNoteId = WATCHLIST_NOTE_ID, title = "Debug watchlist", updatedAt = STARTER_CONTENT_UPDATED_AT),
        CanvasBlock(id = blockId("canvas"), canvasNoteId = CANVAS_NOTE_ID, updatedAt = STARTER_CONTENT_UPDATED_AT)
    )

    private fun watchlistTitles(): List<NoteBlock> = listOf(
        movieBlock(blockId("watchlist_movie"), tmdbId = 900_002L, kind = MovieKind.MOVIE, title = "Watchlist Movie", yearText = "2019", status = WatchStatus.NOT_WATCHED, ratingHalfStars = null),
        movieBlock(blockId("watchlist_show"), tmdbId = 900_003L, kind = MovieKind.SHOW, title = "Watchlist Show", yearText = "2023", status = WatchStatus.WATCHING, ratingHalfStars = 7)
    )

    private fun movieBlock(
        id: String,
        tmdbId: Long,
        kind: MovieKind,
        title: String,
        yearText: String,
        status: WatchStatus,
        ratingHalfStars: Int?
    ): MovieBlock = MovieBlock(
        id = id,
        details = MovieDetails(tmdbId = tmdbId, kind = kind, title = title, yearText = yearText, overview = "Made-up title for testing."),
        watchStatus = status,
        ratingHalfStars = ratingHalfStars,
        watchStateChangedAt = STARTER_CONTENT_UPDATED_AT,
        addedAt = STARTER_CONTENT_UPDATED_AT,
        updatedAt = STARTER_CONTENT_UPDATED_AT
    )

    private fun spanOf(
        word: String,
        bold: Boolean = false,
        italic: Boolean = false,
        strikeThrough: Boolean = false,
        underline: Boolean = false,
        highlight: Boolean = false,
        highlightColorName: String? = null
    ): InlineSpan {
        val start = STYLED_LINE.indexOf(word)
        return InlineSpan(
            start = start,
            end = start + word.length,
            bold = bold,
            italic = italic,
            strikeThrough = strikeThrough,
            underline = underline,
            highlight = highlight,
            highlightColorName = highlightColorName
        )
    }

    private suspend fun seedDatabase(repository: NoteRepository) {
        val emptySettings = newDatabaseSettings(DATABASE_ID, STARTER_CONTENT_UPDATED_AT)
        val settings = emptySettings.copy(title = DATABASE_TITLE)
            .withColumnAdded(statusColumn)
            .withSettingTimesStamped(before = emptySettings, now = STARTER_CONTENT_UPDATED_AT)
        saveStarterNote(
            repository,
            subNoteMetadata(databaseNoteId(DATABASE_ID), DATABASE_TITLE, NoteKind.DATABASE),
            listOf(settings)
        )
        databaseRows.forEachIndexed { index, (rowTitle, status) ->
            val rowNoteId = "${ID_PREFIX}_database_row_${index + 1}"
            val rowStatus = PropertyBlock(
                id = databaseCellBlockId(statusColumn, rowNoteId),
                propertyType = PropertyType.STATUS,
                tags = listOf(status),
                updatedAt = STARTER_CONTENT_UPDATED_AT
            )
            saveStarterNote(
                repository,
                subNoteMetadata(rowNoteId, rowTitle).copy(databaseId = DATABASE_ID),
                listOf(rowStatus)
            )
        }
    }

    private fun subNoteMetadata(noteId: String, title: String, kind: NoteKind = NoteKind.NOTE): NoteMetadataEntity =
        NoteMetadataEntity(
            noteId = noteId,
            title = title,
            folderId = null,
            isDaily = false,
            dateString = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = STARTER_CONTENT_UPDATED_AT,
            filePath = "",
            isSubNote = true,
            kind = kind
        )

    private suspend fun saveStarterNote(repository: NoteRepository, metadata: NoteMetadataEntity, blocks: List<NoteBlock>) {
        repository.saveNote(metadata = metadata, content = NoteContent(blocks = blocks), stampUpdatedAt = false)
    }
}
