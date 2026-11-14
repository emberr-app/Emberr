package com.emberr.domain.importer

import com.emberr.domain.importer.markdown.WikiLink
import com.emberr.domain.importer.markdown.internalPathOf
import com.emberr.domain.importer.markdown.isExternalDestination

internal object DocumentLinkedNoteFiles {

    private val markdownLinkPattern = Regex("""(!?)\[([^\]\n]*)]\(\s*<?([^)>\n]+?)>?(?:\s+"[^"\n]*")?\s*\)""")
    private val wikiLinkPattern = Regex("""\[\[([^\]\n]+)]]""")
    private val fencedCodeBlockPattern = Regex("""(?ms)^ {0,3}(```|~~~).*?^ {0,3}\1""")
    private val inlineCodePattern = Regex("""`[^`\n]*`""")

    fun pathsLinkedOnlyAsDocuments(markdownTextsByPath: Map<String, String>): Set<String> {
        val notePathFinder = NotePathFinder(markdownTextsByPath.keys)
        val linkedAsDocument = mutableSetOf<String>()
        val linkedAsNote = mutableSetOf<String>()

        for ((notePath, text) in markdownTextsByPath) {
            val textWithoutCode = withoutCode(text)
            for (link in markdownLinkPattern.findAll(textWithoutCode)) {
                val (embedMarker, label, destination) = link.destructured
                if (isExternalDestination(destination)) continue
                val linkedPath = notePathFinder.notePathLinkedFrom(notePath, internalPathOf(destination)) ?: continue
                val extension = linkedPath.substringAfterLast('.')
                val looksLikeAFileLink = embedMarker.isEmpty() && label.trim().endsWith(".$extension", ignoreCase = true)
                if (looksLikeAFileLink) linkedAsDocument += linkedPath else linkedAsNote += linkedPath
            }
            for (link in wikiLinkPattern.findAll(textWithoutCode)) {
                val target = WikiLink.fromInnerText(link.groupValues[1], isEmbed = false).target
                notePathFinder.notePathLinkedFrom(notePath, target)?.let { linkedPath -> linkedAsNote += linkedPath }
            }
        }
        return linkedAsDocument - linkedAsNote
    }

    private fun withoutCode(text: String): String = text
        .replace(fencedCodeBlockPattern, "")
        .replace(inlineCodePattern, "")
}
