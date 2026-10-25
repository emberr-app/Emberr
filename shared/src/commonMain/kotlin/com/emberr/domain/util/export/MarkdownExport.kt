package com.emberr.domain.util.export

import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.VoiceBlock

private const val ATTACHMENTS_FOLDER_NAME = "attachments"

data class MarkdownAttachment(val storedFileName: String, val pathInExport: String)

data class MarkdownExport(
    val baseFileName: String,
    val markdown: String,
    val attachments: List<MarkdownAttachment>
) {
    val isZip: Boolean get() = attachments.isNotEmpty()
    val markdownFileName: String get() = "$baseFileName.md"
    val fileName: String get() = if (isZip) "$baseFileName.zip" else markdownFileName
}

object MarkdownAttachments {

    private val unsafeFileNameCharacters = Regex("[^\\p{L}\\p{N}._-]")

    fun collect(blocks: List<NoteBlock>): List<MarkdownAttachment> {
        val attachmentsByStoredName = linkedMapOf<String, MarkdownAttachment>()
        val usedLowercaseNames = mutableSetOf<String>()
        for (block in blocks) {
            if (block.isDeleted) continue
            val storedFileName = storedFileNameOf(block) ?: continue
            if (storedFileName in attachmentsByStoredName) continue
            val exportedName = uniqueNameOf(preferredNameOf(block, storedFileName), usedLowercaseNames)
            usedLowercaseNames += exportedName.lowercase()
            attachmentsByStoredName[storedFileName] =
                MarkdownAttachment(storedFileName, "$ATTACHMENTS_FOLDER_NAME/$exportedName")
        }
        return attachmentsByStoredName.values.toList()
    }

    fun storedFileNameOf(localFilePath: String?): String? = localFilePath
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.takeIf { it.isNotBlank() }

    private fun storedFileNameOf(block: NoteBlock): String? = when (block) {
        is ImageBlock -> storedFileNameOf(block.localFilePath)
        is DocumentBlock -> storedFileNameOf(block.localFilePath)
        is VoiceBlock -> storedFileNameOf(block.localFilePath)
        else -> null
    }

    private fun preferredNameOf(block: NoteBlock, storedFileName: String): String {
        if (block !is DocumentBlock) return storedFileName
        val safeName = block.fileName.trim().replace(unsafeFileNameCharacters, "_").trim('.')
        if (safeName.isEmpty()) return storedFileName
        val storedExtension = storedFileName.substringAfterLast('.', missingDelimiterValue = "")
        return if (safeName.contains('.') || storedExtension.isEmpty()) safeName else "$safeName.$storedExtension"
    }

    private fun uniqueNameOf(name: String, usedLowercaseNames: Set<String>): String {
        if (name.lowercase() !in usedLowercaseNames) return name
        val stem = name.substringBeforeLast('.')
        val extension = name.substringAfterLast('.', missingDelimiterValue = "")
        var copyNumber = 2
        while (true) {
            val candidate = if (extension.isEmpty()) "${stem}_$copyNumber" else "${stem}_$copyNumber.$extension"
            if (candidate.lowercase() !in usedLowercaseNames) return candidate
            copyNumber++
        }
    }
}
