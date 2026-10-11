package com.emberr.domain.importer.canvas

import com.emberr.domain.importer.ImportLinkResolver
import com.emberr.domain.importer.ImportPaths
import com.emberr.domain.importer.markdown.MarkdownToBlocks
import com.emberr.domain.model.BookmarkBlock
import com.emberr.domain.model.BulletedListBlock
import com.emberr.domain.model.CalloutBlock
import com.emberr.domain.model.CheckboxBlock
import com.emberr.domain.model.CodeBlock
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.NumberedListBlock
import com.emberr.domain.model.QuoteBlock
import com.emberr.domain.model.SolidDividerBlock
import com.emberr.domain.model.TableBlock
import com.emberr.domain.model.TextBlock
import com.emberr.domain.model.ThreeDotDividerBlock
import com.emberr.domain.model.ToggleBlock

private const val INDENTATION_PER_LEVEL = "  "
private const val BULLET_PREFIX = "• "
private const val CHECKED_PREFIX = "☑ "
private const val UNCHECKED_PREFIX = "☐ "
private const val DIVIDER_LINE = "———"
private const val TABLE_CELL_SEPARATOR = " | "

internal object CanvasCardText {

    private val imagesOnlyResolver = object : ImportLinkResolver {
        override fun noteIdFor(linkTarget: String): String? = null
        override fun attachmentPathFor(linkTarget: String): String? = linkTarget.takeIf(ImportPaths::isImageFile)
    }

    fun fromMarkdown(markdown: String, now: Long): String {
        var lastBlockNumber = 0
        val converted = MarkdownToBlocks.convert(markdown, imagesOnlyResolver, now, newBlockId = { "card-block-${++lastBlockNumber}" })
        val attachmentPathsByBlockId = converted.pendingAttachments.associate { it.blockId to it.relativePath }
        return converted.blocks
            .mapNotNull { block ->
                val line = lineOf(block, attachmentPathsByBlockId[block.id]) ?: return@mapNotNull null
                INDENTATION_PER_LEVEL.repeat(block.indentationLevel) + line
            }
            .joinToString("\n")
            .trimEnd()
    }

    private fun lineOf(block: NoteBlock, attachmentPath: String?): String? {
        if (attachmentPath != null) return attachmentPath.substringAfterLast('/')
        return when (block) {
            is TextBlock -> block.text
            is HeadingBlock -> block.text
            is QuoteBlock -> block.text
            is ToggleBlock -> block.text
            is CalloutBlock -> block.text.ifBlank { block.calloutTypeName.lowercase().replaceFirstChar { it.uppercase() } }
            is BulletedListBlock -> BULLET_PREFIX + block.text
            is NumberedListBlock -> "${block.number}. ${block.text}"
            is CheckboxBlock -> (if (block.isChecked) CHECKED_PREFIX else UNCHECKED_PREFIX) + block.text
            is CodeBlock -> block.code
            is BookmarkBlock -> block.title?.let { title -> "$title (${block.url})" } ?: block.url
            is TableBlock -> block.rows.joinToString("\n") { row -> row.joinToString(TABLE_CELL_SEPARATOR) }
            is DocumentBlock -> block.fileName
            is SolidDividerBlock, is ThreeDotDividerBlock -> DIVIDER_LINE
            else -> null
        }
    }
}
