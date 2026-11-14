package com.emberr.domain.util.export

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_attachments_left_out
import org.jetbrains.compose.resources.getPluralString

object MarkdownZipWriter {

    fun write(export: MarkdownExport, destination: OutputStream, mediaFileOf: (storedFileName: String) -> File): Int =
        ZipOutputStream(destination).use { zip ->
            zip.addTextFile(export.markdownFileName, export.markdown)
            val missingAttachmentCount = zip.addAttachments(export.attachments, mediaFileOf)
            export.canvasFiles.forEach { canvasFile -> zip.addTextFile(canvasFile.pathInExport, canvasFile.canvasJson) }
            missingAttachmentCount
        }

    fun writeVault(vault: MarkdownVault, destination: OutputStream, mediaFileOf: (storedFileName: String) -> File): Int =
        ZipOutputStream(destination).use { zip ->
            vault.folderPaths.forEach { folderPath -> zip.addFolder(folderPath) }
            vault.textFiles.forEach { textFile -> zip.addTextFile(textFile.path, textFile.text) }
            zip.addAttachments(vault.attachments, mediaFileOf)
        }

    private fun ZipOutputStream.addFolder(folderPath: String) {
        putNextEntry(ZipEntry("$folderPath/"))
        closeEntry()
    }

    private fun ZipOutputStream.addTextFile(path: String, text: String) {
        putNextEntry(ZipEntry(path))
        write(text.toByteArray())
        closeEntry()
    }

    private fun ZipOutputStream.addAttachments(attachments: List<MarkdownAttachment>, mediaFileOf: (storedFileName: String) -> File): Int {
        var missingAttachmentCount = 0
        for (attachment in attachments) {
            val mediaFile = mediaFileOf(attachment.storedFileName)
            if (!mediaFile.isFile) {
                missingAttachmentCount++
                continue
            }
            putNextEntry(ZipEntry(attachment.pathInExport))
            mediaFile.inputStream().use { it.copyTo(this) }
            closeEntry()
        }
        return missingAttachmentCount
    }

    suspend fun missingAttachmentsNotice(missingAttachmentCount: Int): String? =
        if (missingAttachmentCount == 0) {
            null
        } else {
            getPluralString(Res.plurals.message_attachments_left_out, missingAttachmentCount, missingAttachmentCount)
        }
}
