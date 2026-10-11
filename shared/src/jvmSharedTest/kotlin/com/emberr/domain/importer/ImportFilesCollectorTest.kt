package com.emberr.domain.importer

import kotlinx.coroutines.test.runTest
import okio.buffer
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val FILE_DATE = 1_709_634_030_000L

class ImportFilesCollectorTest {

    private val workingDirectory = Files.createTempDirectory("emberr-import-files").toFile()

    @AfterTest
    fun deleteWorkingDirectory() {
        workingDirectory.deleteRecursively()
    }

    private fun zipFile(
        name: String,
        entries: Map<String, String>,
        nameCharset: Charset = Charsets.UTF_8,
        entryDate: Long? = null
    ): File {
        val zip = File(workingDirectory, name)
        ZipOutputStream(zip.outputStream(), nameCharset).use { output ->
            entries.forEach { (path, content) ->
                output.putNextEntry(ZipEntry(path).apply { if (entryDate != null) time = entryDate })
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
    fun zipEntriesAndLooseFilesCanBeStreamed() {
        val zip = zipFile("vault.zip", mapOf("Notes/hello.md" to "Hello from the zip"))

        collect(zip, looseFile("plan.md", "Hello from a file")).use { picked ->
            assertEquals(
                listOf("Hello from the zip", "Hello from a file"),
                picked.files.map { file -> file.openSource().buffer().use { source -> source.readUtf8() } }
            )
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
    fun excalidrawDrawingsAreSkippedInZipsAndAsLooseFiles() {
        val zip = zipFile(
            "vault.zip",
            mapOf(
                "Vault/Drawings/Sketch.excalidraw.md" to "drawing",
                "Vault/Drawings/Old.excalidraw" to "{}",
                "Vault/plan.md" to "# Plan"
            )
        )

        collect(zip, looseFile("Loose.excalidraw.md", "drawing")).use { picked ->
            assertEquals(listOf("Vault/plan.md"), picked.files.map { file -> file.relativePath })
            assertEquals(
                listOf("Vault/Drawings/Sketch.excalidraw.md", "Vault/Drawings/Old.excalidraw", "Loose.excalidraw.md"),
                picked.skippedNames
            )
        }
    }

    @Test
    fun zipEntriesAndLooseFilesKeepTheirLastModifiedDate() {
        val zip = zipFile("vault.zip", mapOf("Notes/hello.md" to "Hello"), entryDate = FILE_DATE)
        val planFile = looseFile("plan.md", "# Plan").apply { setLastModified(FILE_DATE) }

        collect(zip, planFile).use { picked ->
            assertEquals(listOf(FILE_DATE, FILE_DATE), picked.files.map { file -> file.lastModifiedAt })
        }
    }

    @Test
    fun aLooseFileWithoutADateHasNoLastModifiedDate() {
        val missingFile = File(workingDirectory, "gone.md")

        collect(missingFile).use { picked ->
            assertNull(picked.files.single().lastModifiedAt)
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
