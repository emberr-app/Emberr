package com.emberr.domain.importer

import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class ImportFilesCollectorTest {

    private val workingDirectory = Files.createTempDirectory("emberr-import-files").toFile()

    @AfterTest
    fun deleteWorkingDirectory() {
        workingDirectory.deleteRecursively()
    }

    private fun zipFile(
        name: String,
        entries: Map<String, String>,
        nameCharset: Charset = Charsets.UTF_8
    ): File {
        val zip = File(workingDirectory, name)
        ZipOutputStream(zip.outputStream(), nameCharset).use { output ->
            entries.forEach { (path, content) ->
                output.putNextEntry(ZipEntry(path))
                output.write(content.toByteArray())
                output.closeEntry()
            }
        }
        return zip
    }

    private fun looseFile(name: String, content: String): File =
        File(workingDirectory, name).apply { writeText(content) }

    private fun collect(vararg picked: File): PickedImportFiles {
        val collector = ImportFilesCollector()
        picked.forEach { file -> collector.addPickedFile(file.name, file) }
        return collector.finish(pickedNames = picked.map { file -> file.name })
    }

    @Test
    fun foldersInsideAZipBecomeRelativePaths() {
        val zip = zipFile(
            "vault.zip",
            mapOf(
                "Vault/Work/" to "",
                "Vault/Work/Projects/plan.md" to "# Plan",
                "Vault/journal.txt" to "Today",
                "Vault/assets/photo.png" to "png"
            )
        )

        collect(zip).use { picked ->
            assertEquals(
                setOf("Vault/Work/Projects/plan.md", "Vault/journal.txt", "Vault/assets/photo.png"),
                picked.files.map { file -> file.relativePath }.toSet()
            )
            assertEquals(emptyList(), picked.skippedNames)
            assertEquals(listOf("vault.zip"), picked.pickedNames)
        }
    }

    @Test
    fun systemJunkIsLeftOutWithoutBeingReportedAsSkipped() {
        val zip = zipFile(
            "vault.zip",
            mapOf(
                "__MACOSX/Vault/._plan.md" to "junk",
                "Vault/.DS_Store" to "junk",
                ".obsidian/workspace.json" to "{}",
                "Vault/plan.md" to "# Plan"
            )
        )

        collect(zip).use { picked ->
            assertEquals(listOf("Vault/plan.md"), picked.files.map { file -> file.relativePath })
            assertEquals(emptyList(), picked.skippedNames)
        }
    }

    @Test
    fun entriesThatClimbOutOfTheImportAreSkipped() {
        val zip = zipFile(
            "vault.zip",
            mapOf(
                "../evil.md" to "x",
                "Notes/../../evil.md" to "x",
                "Notes/fine.md" to "ok"
            )
        )

        collect(zip).use { picked ->
            assertEquals(listOf("Notes/fine.md"), picked.files.map { file -> file.relativePath })
            assertEquals(listOf("../evil.md", "Notes/../../evil.md"), picked.skippedNames)
        }
    }

    @Test
    fun aLooseMarkdownFileKeepsItsNameAndContent() = runTest {
        collect(looseFile("plan.md", "# Plan")).use { picked ->
            val file = picked.files.single()
            assertEquals("plan.md", file.relativePath)
            assertEquals("# Plan", file.readBytes().decodeToString())
        }
    }

    @Test
    fun aZipEntryCanBeReadAfterPicking() = runTest {
        collect(zipFile("vault.zip", mapOf("Notes/hello.md" to "Hello"))).use { picked ->
            assertEquals("Hello", picked.files.single().readBytes().decodeToString())
        }
    }

    @Test
    fun unsupportedLooseFilesAreSkipped() {
        collect(looseFile("photo.png", "png")).use { picked ->
            assertEquals(emptyList(), picked.files)
            assertEquals(listOf("photo.png"), picked.skippedNames)
        }
    }

    @Test
    fun aBrokenZipIsSkipped() {
        collect(looseFile("notes.zip", "this is not a zip")).use { picked ->
            assertEquals(emptyList(), picked.files)
            assertEquals(listOf("notes.zip"), picked.skippedNames)
        }
    }

    @Test
    fun zipNamesWrittenByOlderToolsAreStillRead() {
        val zip = zipFile("old.zip", mapOf("Café/menu.md" to "Soup"), nameCharset = Charset.forName("IBM437"))

        collect(zip).use { picked ->
            assertEquals(listOf("Café/menu.md"), picked.files.map { file -> file.relativePath })
        }
    }

    @Test
    fun closingReleasesTheZipAndRunsTheCleanUp() = runTest {
        var cleanedUp = false
        val collector = ImportFilesCollector()
        collector.addPickedFile("vault.zip", zipFile("vault.zip", mapOf("Notes/hello.md" to "Hello")))
        val picked = collector.finish(pickedNames = listOf("vault.zip")) { cleanedUp = true }

        picked.close()

        assertTrue(cleanedUp)
        assertFails { picked.files.single().readBytes() }
    }
}
