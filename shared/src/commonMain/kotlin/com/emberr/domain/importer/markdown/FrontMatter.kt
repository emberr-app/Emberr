package com.emberr.domain.importer.markdown

private val frontMatterPattern = Regex("^---[ \\t]*\\n(.*?\\n)?(?:---|\\.\\.\\.)[ \\t]*(?:\\n|$)", RegexOption.DOT_MATCHES_ALL)
private val frontMatterKeyLine = Regex("^[A-Za-z0-9_\"'][^:]*:(\\s|$)")
private val keyValueLine = Regex("^([^:#\\s][^:]*?)\\s*:(?:\\s+(.*))?$")
private val listItemLine = Regex("^\\s*-(?:\\s+(.*))?$")

internal sealed interface FrontMatterValue {
    data class Single(val text: String) : FrontMatterValue
    data class Many(val items: List<String>) : FrontMatterValue
}

internal data class FrontMatterField(val key: String, val value: FrontMatterValue)

internal class MarkdownWithFrontMatter(val frontMatter: List<FrontMatterField>, val body: String)

internal object FrontMatter {

    fun split(markdown: String): MarkdownWithFrontMatter {
        val match = frontMatterPattern.find(markdown) ?: return MarkdownWithFrontMatter(emptyList(), markdown)
        val frontMatterText = match.groupValues[1]
        val firstContentLine = frontMatterText.lineSequence().firstOrNull { it.isNotBlank() }
        if (firstContentLine != null && !frontMatterKeyLine.containsMatchIn(firstContentLine.trim())) {
            return MarkdownWithFrontMatter(emptyList(), markdown)
        }
        return MarkdownWithFrontMatter(parse(frontMatterText), markdown.substring(match.range.last + 1))
    }

    fun parse(frontMatterText: String): List<FrontMatterField> {
        val lines = frontMatterText.lines()
        val fields = mutableListOf<FrontMatterField>()
        var index = 0
        while (index < lines.size) {
            val line = lines[index].trimEnd()
            index++
            if (line.isBlank() || line.startsWith('#') || line.first().isWhitespace()) continue
            val keyValue = keyValueLine.matchEntire(line) ?: continue

            val followingLines = mutableListOf<String>()
            while (index < lines.size && belongsToPreviousKey(lines[index])) {
                followingLines += lines[index]
                index++
            }

            val key = unquoted(keyValue.groupValues[1].trim())
            val value = valueOf(keyValue.groupValues[2].trim(), followingLines) ?: continue
            if (fields.none { it.key.equals(key, ignoreCase = true) }) fields += FrontMatterField(key, value)
        }
        return fields
    }

    private fun belongsToPreviousKey(line: String): Boolean =
        line.isBlank() || line.first().isWhitespace() || listItemLine.matches(line)

    private fun valueOf(inlineValue: String, followingLines: List<String>): FrontMatterValue? {
        val meaningfulLines = followingLines.filter { it.isNotBlank() }
        val value = when {
            inlineValue.startsWith('[') && inlineValue.endsWith(']') ->
                FrontMatterValue.Many(flowListItems(inlineValue.substring(1, inlineValue.length - 1)))
            inlineValue.startsWith('|') || inlineValue.startsWith('>') ->
                FrontMatterValue.Single(meaningfulLines.joinToString(if (inlineValue.startsWith('|')) "\n" else " ") { it.trim() })
            inlineValue.isNotEmpty() -> FrontMatterValue.Single(unquoted(inlineValue))
            meaningfulLines.isNotEmpty() && meaningfulLines.all { listItemLine.matches(it) } ->
                FrontMatterValue.Many(
                    meaningfulLines.mapNotNull { listItemLine.matchEntire(it)?.groupValues?.get(1)?.trim() }
                        .map(::unquoted)
                        .filter { it.isNotEmpty() }
                )
            meaningfulLines.isNotEmpty() -> FrontMatterValue.Single(meaningfulLines.joinToString("\n") { it.trim() })
            else -> null
        }
        return when (value) {
            is FrontMatterValue.Single -> value.takeIf { it.text.isNotBlank() }
            is FrontMatterValue.Many -> value.takeIf { it.items.isNotEmpty() }
            null -> null
        }
    }

    private fun flowListItems(listContent: String): List<String> {
        val items = mutableListOf<String>()
        val currentItem = StringBuilder()
        var openQuote: Char? = null
        for (character in listContent) {
            when {
                openQuote != null -> {
                    if (character == openQuote) openQuote = null
                    currentItem.append(character)
                }
                character == '"' || character == '\'' -> {
                    openQuote = character
                    currentItem.append(character)
                }
                character == ',' -> {
                    items += currentItem.toString()
                    currentItem.clear()
                }
                else -> currentItem.append(character)
            }
        }
        items += currentItem.toString()
        return items.map { unquoted(it.trim()) }.filter { it.isNotEmpty() }
    }

    private fun unquoted(text: String): String = when {
        text.length >= 2 && text.startsWith('"') && text.endsWith('"') -> text.substring(1, text.length - 1).replace("\\\"", "\"")
        text.length >= 2 && text.startsWith('\'') && text.endsWith('\'') -> text.substring(1, text.length - 1).replace("''", "'")
        else -> text
    }
}
