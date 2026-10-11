package com.emberr.domain.importer

private val noteFileExtensions = ImportPaths.markdownNoteExtensions.map { extension -> ".$extension" }

class ImportedLinkTargets(
    private val noteIdsByPath: Map<String, String>,
    attachmentPaths: Collection<String>
) {

    private val notePaths = PathLookup(noteIdsByPath.keys)
    private val attachmentLookup = PathLookup(attachmentPaths)

    fun resolverFor(notePath: String): ImportLinkResolver {
        val noteFolder = ImportPaths.folderOf(notePath)
        return object : ImportLinkResolver {
            override fun noteIdFor(linkTarget: String): String? =
                notePaths.find(noteFolder, linkTarget, extensionsToTry = noteFileExtensions)?.let(noteIdsByPath::get)

            override fun attachmentPathFor(linkTarget: String): String? =
                attachmentLookup.find(noteFolder, linkTarget, extensionsToTry = emptyList())
        }
    }
}

private class PathLookup(paths: Collection<String>) {

    private val pathsByLowercasePath = paths.groupBy { it.lowercase() }
    private val pathsByLowercaseName = paths.groupBy { fileNameOf(it).lowercase() }
    private val pathsByLowercaseNameWithoutExtension = paths.groupBy { fileNameOf(it).substringBeforeLast('.').lowercase() }

    fun find(fromFolder: String, linkTarget: String, extensionsToTry: List<String>): String? {
        val target = linkTarget.trim()
        if (target.isEmpty()) return null

        val joinedPaths = listOfNotNull(joinPaths(fromFolder, target), joinPaths("", target)).distinct()
        for (joinedPath in joinedPaths) {
            val candidates = listOf(joinedPath) + extensionsToTry.map { extension -> joinedPath + extension }
            candidates.firstNotNullOfOrNull { candidate -> pathsByLowercasePath[candidate.lowercase()]?.firstOrNull() }
                ?.let { return it }
        }

        val targetName = fileNameOf(target).lowercase()
        val sameNamePaths = pathsByLowercaseName[targetName]
            ?: pathsByLowercaseNameWithoutExtension[targetName]
                ?.filter { path -> extensionsToTry.any { extension -> path.endsWith(extension, ignoreCase = true) } }
                ?.takeIf { it.isNotEmpty() }
            ?: return null
        return sameNamePaths.minWithOrNull(
            compareBy<String>({ ImportPaths.folderOf(it) != fromFolder }, { it.length }, { it })
        )
    }
}

private fun fileNameOf(path: String): String = path.substringAfterLast('/')

private fun joinPaths(folder: String, target: String): String? {
    val segments = mutableListOf<String>()
    if (!target.startsWith('/')) segments += folder.split('/').filter { it.isNotEmpty() }
    for (segment in target.split('/')) {
        when (segment) {
            "", "." -> Unit
            ".." -> if (segments.isEmpty()) return null else segments.removeAt(segments.lastIndex)
            else -> segments += segment
        }
    }
    return segments.joinToString("/").takeIf { it.isNotEmpty() }
}
