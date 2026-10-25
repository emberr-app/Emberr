package com.emberr.domain.util.export

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipInputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class MarkdownZipWriterTest {

    private val mediaDirectory = Files.createTempDirectory("emberr-markdown-zip").toFile()

    @AfterTest
    fun deleteMediaDirectory() {
        mediaDirectory.deleteRecursively()
    }

    @Test
    fun theZipHoldsTheNoteAndEveryAttachmentOnThisDeviceAndCountsTheMissingOnes() {
        val photoBytes = ByteArray(1000) { index -> (index % 251).toByte() }
        File(mediaDirectory, "media_photo.jpg").writeBytes(photoBytes)
        val export = MarkdownExport(
            baseFileName = "Prices",
            markdown = "# Prices\n\n![](attachments/media_photo.jpg)",
            attachments = listOf(
                MarkdownAttachment("media_photo.jpg", "attachments/media_photo.jpg"),
                MarkdownAttachment("media_not_downloaded.jpg", "attachments/media_not_downloaded.jpg")
            )
        )
        val zipBytes = ByteArrayOutputStream()

        val missingAttachmentCount = MarkdownZipWriter.write(export, zipBytes) { File(mediaDirectory, it) }

        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes.toByteArray())).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entry -> entries[entry.name] = zip.readBytes() }
        }
        assertEquals(1, missingAttachmentCount)
        assertEquals(listOf("Prices.md", "attachments/media_photo.jpg"), entries.keys.toList())
        assertEquals(export.markdown, entries.getValue("Prices.md").decodeToString())
        assertContentEquals(photoBytes, entries.getValue("attachments/media_photo.jpg"))
    }
}
