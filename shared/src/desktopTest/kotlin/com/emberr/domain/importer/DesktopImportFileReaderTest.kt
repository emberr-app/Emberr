package com.emberr.domain.importer

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopImportFileReaderTest {

    private val workingDirectory = Files.createTempDirectory("emberr-desktop-reader").toFile()

    @AfterTest
    fun deleteWorkingDirectory() {
        workingDirectory.deleteRecursively()
    }

    private fun zipFile(name: String): File {
        val zip = File(workingDirectory, name)
        ZipOutputStream(zip.outputStream()).use { output ->
            output.putNextEntry(ZipEntry("Note.md"))
            output.write("Hello".toByteArray())
            output.closeEntry()
        }
        return zip
    }

    private fun isOpenByThisProcess(file: File): Boolean {
        val realPath = file.toPath().toRealPath()
        return File("/proc/self/fd").listFiles().orEmpty().any { descriptor ->
            runCatching { Files.readSymbolicLink(descriptor.toPath()) }.getOrNull() == realPath
        }
    }

    @Test
    fun aFinishedReadKeepsTheZipOpenUntilItIsClosed() = runBlocking {
        val zip = zipFile("vault.zip")

        val pickedFiles = DesktopImportFileReader().readPickedFiles(listOf(zip.path))

        assertEquals(listOf("Note.md"), pickedFiles.files.map { it.relativePath })
        assertTrue(isOpenByThisProcess(zip))
        pickedFiles.close()
        assertFalse(isOpenByThisProcess(zip))
    }

    @Test
    fun stoppingAReadAsItHandsBackItsFilesClosesTheZip() {
        val zip = zipFile("vault.zip")
        val callerThread = Executors.newSingleThreadExecutor()
        val callerDispatcher = callerThread.asCoroutineDispatcher()
        val callerThreadIsHeld = CountDownLatch(1)

        try {
            runBlocking {
                val read = launch(callerDispatcher) { DesktopImportFileReader().readPickedFiles(listOf(zip.path)).close() }
                callerThread.execute { callerThreadIsHeld.await() }
                withTimeout(5_000) {
                    while (!isOpenByThisProcess(zip) && !read.isCompleted) delay(10)
                }

                read.cancel()
                callerThreadIsHeld.countDown()
                read.join()
            }
        } finally {
            callerThreadIsHeld.countDown()
            callerDispatcher.close()
        }

        assertFalse(isOpenByThisProcess(zip))
    }
}
