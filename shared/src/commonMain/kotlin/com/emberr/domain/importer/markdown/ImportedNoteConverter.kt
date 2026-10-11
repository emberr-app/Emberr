package com.emberr.domain.importer.markdown

import com.emberr.domain.importer.ImportLinkResolver
import com.emberr.domain.importer.ImportPaths
import com.emberr.domain.importer.newImportId
import com.emberr.domain.importer.whitespaceRun
import com.emberr.domain.model.HeadingBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.TextBlock

private const val PLAIN_TEXT_EXTENSION = ".txt"
private const val FRONT_MATTER_TITLE_KEY = "title"

internal data class PendingAttachment(val blockId: String, val relativePath: String)

internal data class PendingWebImage(val blockId: String, val url: String, val altText: String)

internal data class ConvertedNote(
    val title: String,
    val blocks: List<NoteBlock>,
    val pendingAttachments: List<PendingAttachment>,
    val pendingWebImages: List<PendingWebImage>,
    val frontMatter: List<FrontMatterField>,
    val inlineTags: List<String>
)

internal object ImportedNoteConverter {

    fun convert(
        relativePath: String,
        content: String,
        linkResolver: ImportLinkResolver,
        now: Long,
        newBlockId: () -> String = ::newImportId
    ): ConvertedNote {
        val cleanedContent = content.replace("\r\n", "\n").replace('\r', '\n')

        if (relativePath.endsWith(PLAIN_TEXT_EXTENSION, ignoreCase = true)) {
            return ConvertedNote(
                title = ImportPaths.noteTitleOf(relativePath),
                blocks = PlainTextToBlocks.convert(cleanedContent, now, newBlockId),
                pendingAttachments = emptyList(),
                pendingWebImages = emptyList(),
                frontMatter = emptyList(),
                inlineTags = emptyList()
            )
        }

        val withFrontMatter = FrontMatter.split(cleanedContent)
        val title = titleFrom(withFrontMatter.frontMatter) ?: ImportPaths.noteTitleOf(relativePath)
        val converted = MarkdownToBlocks.convert(withFrontMatter.body, linkResolver, now, newBlockId)
        return ConvertedNote(
            title = title,
            blocks = converted.blocks.withoutRepeatedTitle(title),
            pendingAttachments = converted.pendingAttachments,
            pendingWebImages = converted.pendingWebImages,
            frontMatter = withFrontMatter.frontMatter,
            inlineTags = converted.inlineTags
        )
    }

    private fun titleFrom(frontMatter: List<FrontMatterField>): String? =
        frontMatter.firstOrNull { it.key.equals(FRONT_MATTER_TITLE_KEY, ignoreCase = true) }
            ?.let { (it.value as? FrontMatterValue.Single)?.text?.trim() }
            ?.takeIf { it.isNotEmpty() }

    private fun List<NoteBlock>.withoutRepeatedTitle(title: String): List<NoteBlock> {
        val firstHeading = firstOrNull() as? HeadingBlock ?: return this
        return if (firstHeading.level == 1 && comparableTitle(firstHeading.text) == comparableTitle(title)) drop(1) else this
    }

    private fun comparableTitle(text: String): String = text.trim().replace(whitespaceRun, " ").lowercase()
}

internal object PlainTextToBlocks {

    fun convert(text: String, now: Long, newBlockId: () -> String): List<NoteBlock> =
        text.lines()
            .dropWhile { it.isBlank() }
            .dropLastWhile { it.isBlank() }
            .map { line -> TextBlock(id = newBlockId(), text = line.trimEnd(), updatedAt = now) }
}
