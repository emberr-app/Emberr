package com.emberr.domain.util.export

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_attachments_left_out
import org.jetbrains.compose.resources.getPluralString

object MarkdownZipWriter {

    fun write(export: MarkdownExport, destination: OutputStream, mediaFileOf: (storedFileName: String) -> File): Int {
        var missingAttachmentCount = 0
        ZipOutputStream(destination).use { zip ->
            zip.putNextEntry(ZipEntry(export.markdownFileName))
            zip.write(export.markdown.toByteArray())
            zip.closeEntry()
            for (attachment in export.attachments) {
                val mediaFile = mediaFileOf(attachment.storedFileName)
                if (!mediaFile.isFile) {
                    missingAttachmentCount++
                    continue
                }
                zip.putNextEntry(ZipEntry(attachment.pathInExport))
                mediaFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
            for (canvasFile in export.canvasFiles) {
                zip.putNextEntry(ZipEntry(canvasFile.pathInExport))
                zip.write(canvasFile.canvasJson.toByteArray())
                zip.closeEntry()
            }
        }
        return missingAttachmentCount
    }

    fun writeVault(vault: MarkdownVault, destination: OutputStream, mediaFileOf: (storedFileName: String) -> File): Int {
        var missingAttachmentCount = 0
        ZipOutputStream(destination).use { zip ->
            for (folderPath in vault.folderPaths) {
                zip.putNextEntry(ZipEntry("$folderPath/"))
                zip.closeEntry()
            }
            for (textFile in vault.textFiles) {
                zip.putNextEntry(ZipEntry(textFile.path))
                zip.write(textFile.text.toByteArray())
                zip.closeEntry()
            }
            for (attachment in vault.attachments) {
                val mediaFile = mediaFileOf(attachment.storedFileName)
                if (!mediaFile.isFile) {
                    missingAttachmentCount++
                    continue
                }
                zip.putNextEntry(ZipEntry(attachment.pathInExport))
                mediaFile.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
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
