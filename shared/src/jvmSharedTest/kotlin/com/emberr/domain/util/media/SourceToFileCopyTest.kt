package com.emberr.domain.util.media

import okio.Buffer
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class SourceToFileCopyTest {

    private val workingDirectory = Files.createTempDirectory("emberr-source-copy").toFile()

    @AfterTest
    fun deleteWorkingDirectory() {
        workingDirectory.deleteRecursively()
    }

    @Test
    fun everyByteIsCopiedAndTheLastProgressIsTheFullSize() {
        val bytes = ByteArray(200 * 1024) { index -> (index % 251).toByte() }
        val file = File(workingDirectory, "copy.bin")
        val reportedProgress = mutableListOf<Long>()

        copySourceIntoFile(openSource = { Buffer().write(bytes) }, file = file, onBytesCopied = reportedProgress::add)

        assertContentEquals(bytes, file.readBytes())
        assertEquals(bytes.size.toLong(), reportedProgress.last())
        assertEquals(reportedProgress.sorted(), reportedProgress)
    }

    @Test
    fun anEmptySourceMakesAnEmptyFileWithoutProgress() {
        val file = File(workingDirectory, "empty.bin")
        val reportedProgress = mutableListOf<Long>()

        copySourceIntoFile(openSource = { Buffer() }, file = file, onBytesCopied = reportedProgress::add)

        assertEquals(0L, file.length())
        assertEquals(emptyList(), reportedProgress)
    }
}
