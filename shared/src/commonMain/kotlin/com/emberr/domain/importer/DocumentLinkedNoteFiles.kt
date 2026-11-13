package com.emberr.domain.importer

import com.emberr.domain.importer.markdown.WikiLink
import com.emberr.domain.importer.markdown.internalPathOf
import com.emberr.domain.importer.markdown.isExternalDestination

internal object DocumentLinkedNoteFiles {

    private val markdownLinkPattern = Regex("""(!?)\[([^\]\n]*)]\(\s*<?([^)>\n]+?)>?(?:\s+"[^"\n]*")?\s*\)""")
    private val wikiLinkPattern = Regex("""\[\[([^\]\n]+)]]""")

    fun pathsLinkedOnlyAsDocuments(markdownTextsByPath: Map<String, String>): Set<String> {
        val linkTargets = ImportedLinkTargets(noteIdsByPath = markdownTextsByPath.keys.associateWith { it }, attachmentPaths = emptyList())
        val linkedAsDocument = mutableSetOf<String>()
        val linkedAsNote = mutableSetOf<String>()

        for ((notePath, text) in markdownTextsByPath) {
            val resolver = linkTargets.resolverFor(notePath)
            for (link in markdownLinkPattern.findAll(text)) {
                val (embedMarker, label, destination) = link.destructured
                if (isExternalDestination(destination)) continue
                val linkedPath = resolver.noteIdFor(internalPathOf(destination)) ?: continue
                val extension = linkedPath.substringAfterLast('.')
                val looksLikeAFileLink = embedMarker.isEmpty() && label.trim().endsWith(".$extension", ignoreCase = true)
                if (looksLikeAFileLink) linkedAsDocument += linkedPath else linkedAsNote += linkedPath
            }
            for (link in wikiLinkPattern.findAll(text)) {
                val target = WikiLink.fromInnerText(link.groupValues[1], isEmbed = false).target
                resolver.noteIdFor(target)?.let { linkedPath -> linkedAsNote += linkedPath }
            }
        }
        return linkedAsDocument - linkedAsNote
    }
}
