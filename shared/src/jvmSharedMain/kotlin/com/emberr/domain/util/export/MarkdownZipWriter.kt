package com.emberr.domain.util.export

import java.io.File
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
        }
        return missingAttachmentCount
    }

    fun missingAttachmentsNotice(missingAttachmentCount: Int): String? = when (missingAttachmentCount) {
        0 -> null
        1 -> "1 attachment wasn't on this device, so it was left out."
        else -> "$missingAttachmentCount attachments weren't on this device, so they were left out."
    }
}
