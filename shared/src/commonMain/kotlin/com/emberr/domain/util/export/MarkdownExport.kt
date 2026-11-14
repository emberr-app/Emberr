package com.emberr.domain.util.export

import com.emberr.domain.canvas.CanvasContent
import com.emberr.domain.canvas.isImage
import com.emberr.domain.model.DocumentBlock
import com.emberr.domain.model.ImageBlock
import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.VoiceBlock

internal const val MARKDOWN_ATTACHMENTS_FOLDER = "attachments"
const val UNTITLED_NOTE_TITLE = "Untitled"
private const val DEFAULT_CANVAS_NAME = "Canvas"
private const val CANVAS_FILE_EXTENSION = "canvas"

data class MarkdownAttachment(val storedFileName: String, val pathInExport: String)

data class ExportedCanvas(val canvasNoteId: String, val title: String, val content: CanvasContent)

data class MarkdownCanvasFile(val canvasNoteId: String, val pathInExport: String, val canvasJson: String)

data class MarkdownExport(
    val baseFileName: String,
    val markdown: String,
    val attachments: List<MarkdownAttachment>,
    val canvasFiles: List<MarkdownCanvasFile> = emptyList()
) {
    val isZip: Boolean get() = attachments.isNotEmpty() || canvasFiles.isNotEmpty()
    val markdownFileName: String get() = "$baseFileName.md"
    val fileName: String get() = if (isZip) "$baseFileName.zip" else markdownFileName
}

object MarkdownAttachments {

    private val unsafeFileNameCharacters = Regex("[^\\p{L}\\p{N}._-]")

    fun collect(blocks: List<NoteBlock>, canvases: List<ExportedCanvas> = emptyList()): List<MarkdownAttachment> {
        val attachmentsByStoredName = linkedMapOf<String, MarkdownAttachment>()
        val usedLowercaseNames = mutableSetOf<String>()
        fun add(storedFileName: String, preferredName: String) {
            if (storedFileName in attachmentsByStoredName) return
            val exportedName = uniqueAttachmentNameOf(preferredName, usedLowercaseNames)
            usedLowercaseNames += exportedName.lowercase()
            attachmentsByStoredName[storedFileName] =
                MarkdownAttachment(storedFileName, "$MARKDOWN_ATTACHMENTS_FOLDER/$exportedName")
        }
        for (block in blocks) {
            if (block.isDeleted) continue
            val storedFileName = storedFileNameOf(block) ?: continue
            add(storedFileName, preferredNameOf(block, storedFileName))
        }
        for (canvas in canvases) {
            for (node in canvas.content.liveOnly().nodes) {
                if (!node.isImage) continue
                val storedFileName = storedFileNameOf(node.imagePath) ?: continue
                add(storedFileName, storedFileName)
            }
        }
        return attachmentsByStoredName.values.toList()
    }

    fun canvasFilesFor(canvases: List<ExportedCanvas>, attachments: List<MarkdownAttachment>): List<MarkdownCanvasFile> {
        val usedLowercaseNames = attachments.mapTo(mutableSetOf()) { it.pathInExport.substringAfterLast('/').lowercase() }
        val attachmentPathsByStoredName = attachments.associate { it.storedFileName to it.pathInExport }
        return canvases.map { canvas ->
            val baseName = safeFileNameOf(canvas.title).ifEmpty { DEFAULT_CANVAS_NAME }
            val exportedName = uniqueAttachmentNameOf("$baseName.$CANVAS_FILE_EXTENSION", usedLowercaseNames)
            usedLowercaseNames += exportedName.lowercase()
            MarkdownCanvasFile(
                canvasNoteId = canvas.canvasNoteId,
                pathInExport = "$MARKDOWN_ATTACHMENTS_FOLDER/$exportedName",
                canvasJson = ObsidianCanvasWriter.write(canvas.content, attachmentPathsByStoredName)
            )
        }
    }

    fun storedFileNameOf(localFilePath: String?): String? = localFilePath
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.takeIf { it.isNotBlank() }

    private fun storedFileNameOf(block: NoteBlock): String? = when (block) {
        is ImageBlock -> storedFileNameOf(block.localFilePath)
        is DocumentBlock -> storedFileNameOf(block.localFilePath)
        is VoiceBlock -> storedFileNameOf(block.localFilePath)
        is MovieBlock -> storedFileNameOf(block.details?.posterFilePath)
        else -> null
    }

    private fun preferredNameOf(block: NoteBlock, storedFileName: String): String {
        if (block !is DocumentBlock) return storedFileName
        val safeName = safeFileNameOf(block.fileName)
        if (safeName.isEmpty()) return storedFileName
        val storedExtension = storedFileName.substringAfterLast('.', missingDelimiterValue = "")
        return if (safeName.contains('.') || storedExtension.isEmpty()) safeName else "$safeName.$storedExtension"
    }

    private fun safeFileNameOf(name: String): String = name.trim().replace(unsafeFileNameCharacters, "_").trim('.')

    private fun uniqueAttachmentNameOf(name: String, usedLowercaseNames: Set<String>): String {
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
