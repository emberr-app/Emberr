package com.emberr.domain.importer

import com.emberr.domain.util.media.isImageExtension

private const val CANVAS_FILE_EXTENSION = "canvas"
private const val ZIP_FILE_EXTENSION = "zip"
private const val UNTITLED_NOTE_TITLE = "Untitled"
private val unsupportedObsidianFileEndings = listOf(".excalidraw", ".excalidraw.md", ".base")
private val ignoredNames = setOf("__MACOSX", "Thumbs.db", "desktop.ini")
private val windowsDriveAtStart = Regex("^[A-Za-z]:")

object ImportPaths {

    val markdownNoteExtensions = listOf("md", "markdown", "txt")

    fun isNoteFile(path: String): Boolean = extensionOf(path) in markdownNoteExtensions || isCanvasFile(path)

    fun isCanvasFile(path: String): Boolean = extensionOf(path) == CANVAS_FILE_EXTENSION

    fun isZipFile(path: String): Boolean = extensionOf(path) == ZIP_FILE_EXTENSION

    fun isImageFile(path: String): Boolean = isImageExtension(extensionOf(path))

    fun folderOf(path: String): String = path.substringBeforeLast('/', missingDelimiterValue = "")

    fun noteTitleOf(path: String): String =
        path.substringAfterLast('/').substringBeforeLast('.').trim().ifEmpty { UNTITLED_NOTE_TITLE }

    fun isUnsupportedObsidianFile(path: String): Boolean =
        unsupportedObsidianFileEndings.any { ending -> path.endsWith(ending, ignoreCase = true) }

    fun isSupportedPickedFile(name: String): Boolean = !isUnsupportedObsidianFile(name) && (isNoteFile(name) || isZipFile(name))

    fun isIgnoredEntry(rawPath: String): Boolean {
        val normalizedPath = rawPath.replace('\\', '/')
        if (normalizedPath.endsWith('/')) return true
        return normalizedPath.split('/').any { segment -> segment in ignoredNames || isHiddenName(segment) }
    }

    fun safeRelativePath(rawPath: String): String? {
        val normalizedPath = rawPath.replace('\\', '/')
        if (normalizedPath.startsWith('/') || windowsDriveAtStart.containsMatchIn(normalizedPath)) return null
        val segments = normalizedPath.split('/').filter { segment -> segment.isNotEmpty() && segment != "." }
        if (segments.isEmpty() || ".." in segments) return null
        return segments.joinToString("/")
    }

    fun folderPathsHoldingNotes(relativePaths: List<String>): Set<String> =
        relativePaths
            .filter(::isNoteFile)
            .flatMap(::parentFolderPaths)
            .toSet()

    private fun parentFolderPaths(path: String): List<String> {
        val folderNames = path.split('/').dropLast(1)
        return folderNames.indices.map { index -> folderNames.take(index + 1).joinToString("/") }
    }

    private fun isHiddenName(segment: String): Boolean =
        segment.startsWith('.') && segment != "." && segment != ".."

    private fun extensionOf(path: String): String =
        path.substringAfterLast('/').substringAfterLast('.', missingDelimiterValue = "").lowercase()
}
