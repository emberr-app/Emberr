package com.emberr.domain.importer

private val noteFileExtensions = setOf("md", "markdown", "txt")
private const val ZIP_FILE_EXTENSION = "zip"
private val ignoredNames = setOf("__MACOSX", "Thumbs.db", "desktop.ini")
private val windowsDriveAtStart = Regex("^[A-Za-z]:")

object ImportPaths {

    fun isNoteFile(path: String): Boolean = extensionOf(path) in noteFileExtensions

    fun isZipFile(path: String): Boolean = extensionOf(path) == ZIP_FILE_EXTENSION

    fun isSupportedPickedFile(name: String): Boolean = isNoteFile(name) || isZipFile(name)

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
