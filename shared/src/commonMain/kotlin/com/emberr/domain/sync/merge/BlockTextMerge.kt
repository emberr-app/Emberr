package com.emberr.domain.sync.merge

import com.emberr.domain.model.InlineSpan
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.editableTextOrNull
import com.emberr.domain.model.inlineSpansOrEmpty
import com.emberr.domain.model.withEditableText

object BlockTextMerge {

    private const val NEWER_SIDE = 0
    private const val OLDER_SIDE = 1
    private const val MAX_EDIT_DISTANCE = 1000

    fun mergeTextBlock(base: NoteBlock?, local: NoteBlock, remote: NoteBlock): NoteBlock? {
        if (base == null || local.isDeleted || remote.isDeleted) return null
        val baseText = base.editableTextOrNull() ?: return null
        val localText = local.editableTextOrNull() ?: return null
        val remoteText = remote.editableTextOrNull() ?: return null
        if (localText == remoteText) return null
        val bothChangedSinceBase = local.updatedAt > base.updatedAt && remote.updatedAt > base.updatedAt
        if (!bothChangedSinceBase) return null

        val localIsNewer = if (local.updatedAt != remote.updatedAt) {
            local.updatedAt > remote.updatedAt
        } else {
            localText > remoteText
        }
        val newer = if (localIsNewer) local else remote
        val older = if (localIsNewer) remote else local
        val newerTextWithSpans = TextWithSpans(if (localIsNewer) localText else remoteText, newer.inlineSpansOrEmpty())
        val merged = mergeText(
            base = TextWithSpans(baseText, base.inlineSpansOrEmpty()),
            newer = newerTextWithSpans,
            older = TextWithSpans(if (localIsNewer) remoteText else localText, older.inlineSpansOrEmpty())
        ) ?: return null
        if (merged == newerTextWithSpans) return null
        return newer.withEditableText(merged.text, merged.spans, maxOf(local.updatedAt, remote.updatedAt) + 1)
    }

    private fun mergeText(base: TextWithSpans, newer: TextWithSpans, older: TextWithSpans): TextWithSpans? {
        val baseTokens = tokenize(base.text)
        val newerDiff = diffAgainstBase(baseTokens, tokenize(newer.text), NEWER_SIDE) ?: return null
        val olderDiff = diffAgainstBase(baseTokens, tokenize(older.text), OLDER_SIDE) ?: return null
        val diffs = listOf(newerDiff, olderDiff)

        val mergedTokens = mutableListOf<MergedToken>()
        var lostBaseTokenCount = 0
        var baseCursor = 0
        for (group in overlappingGroups(newerDiff.hunks + olderDiff.hunks)) {
            addBaseTokens(mergedTokens, baseTokens, baseCursor, group.baseStart)
            lostBaseTokenCount += resolveGroup(group, baseTokens, diffs, mergedTokens)
            baseCursor = group.baseEnd
        }
        addBaseTokens(mergedTokens, baseTokens, baseCursor, baseTokens.size)
        if (lostBaseTokenCount * 2 > baseTokens.size) return null

        val mergedText = mergedTokens.joinToString("") { it.text }
        val positions = MergedPositions(mergedTokens, diffs, baseTokens.size, mergedText.length)
        return TextWithSpans(mergedText, mergedSpans(base, newer, older, positions))
    }

    private data class TextWithSpans(val text: String, val spans: List<InlineSpan>)

    private class TextHunk(
        val side: Int,
        val baseStart: Int,
        val baseEnd: Int,
        val sideStart: Int,
        val sideEnd: Int
    ) {
        val insertsText get() = sideEnd > sideStart
        val deletesText get() = baseEnd > baseStart
    }

    private class SideDiff(
        val tokens: List<String>,
        val baseIndexOfToken: IntArray,
        val hunks: List<TextHunk>
    )

    private class HunkGroup(val baseStart: Int, val baseEnd: Int, val hunks: List<TextHunk>)

    private class MergedToken(val text: String, val baseIndex: Int, val side: Int, val sideIndex: Int)

    private fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            val end = tokenEnd(text, start)
            tokens += text.substring(start, end)
            start = end
        }
        return tokens
    }

    private fun tokenEnd(text: String, start: Int): Int {
        val first = text[start]
        var end = start + 1
        when {
            first.isWhitespace() -> while (end < text.length && text[end].isWhitespace()) end++
            isSpacedWordCharacter(first) -> while (end < text.length && isSpacedWordCharacter(text[end])) end++
            first.isHighSurrogate() && end < text.length && text[end].isLowSurrogate() -> end++
        }
        return end
    }

    private fun isSpacedWordCharacter(character: Char): Boolean {
        val isWordPart = character.isLetterOrDigit() ||
            character.category == CharCategory.NON_SPACING_MARK ||
            character.category == CharCategory.COMBINING_SPACING_MARK ||
            character.category == CharCategory.ENCLOSING_MARK
        return isWordPart && !isUnspacedScriptCharacter(character)
    }

    private fun isUnspacedScriptCharacter(character: Char): Boolean {
        val code = character.code
        return code in 0x3040..0x30FF ||
            code in 0x3400..0x4DBF ||
            code in 0x4E00..0x9FFF ||
            code in 0xF900..0xFAFF ||
            code in 0xFF66..0xFF9F
    }

    private fun diffAgainstBase(baseTokens: List<String>, sideTokens: List<String>, side: Int): SideDiff? {
        val baseIndexOfToken = matchTokens(baseTokens, sideTokens) ?: return null
        slideHunksLeft(baseTokens, sideTokens, baseIndexOfToken, hunksFromMatches(side, baseTokens.size, baseIndexOfToken))
        return SideDiff(sideTokens, baseIndexOfToken, hunksFromMatches(side, baseTokens.size, baseIndexOfToken))
    }

    private fun matchTokens(baseTokens: List<String>, sideTokens: List<String>): IntArray? {
        val baseIndexOfToken = IntArray(sideTokens.size) { -1 }
        var prefixLength = 0
        while (prefixLength < baseTokens.size && prefixLength < sideTokens.size &&
            baseTokens[prefixLength] == sideTokens[prefixLength]
        ) {
            baseIndexOfToken[prefixLength] = prefixLength
            prefixLength++
        }
        var suffixLength = 0
        while (suffixLength < baseTokens.size - prefixLength && suffixLength < sideTokens.size - prefixLength &&
            baseTokens[baseTokens.size - 1 - suffixLength] == sideTokens[sideTokens.size - 1 - suffixLength]
        ) {
            baseIndexOfToken[sideTokens.size - 1 - suffixLength] = baseTokens.size - 1 - suffixLength
            suffixLength++
        }
        val middleMatches = shortestEditMatches(
            baseTokens.subList(prefixLength, baseTokens.size - suffixLength),
            sideTokens.subList(prefixLength, sideTokens.size - suffixLength)
        ) ?: return null
        middleMatches.forEach { (baseIndex, sideIndex) ->
            baseIndexOfToken[prefixLength + sideIndex] = prefixLength + baseIndex
        }
        return baseIndexOfToken
    }

    private fun shortestEditMatches(baseTokens: List<String>, sideTokens: List<String>): List<Pair<Int, Int>>? {
        val baseSize = baseTokens.size
        val sideSize = sideTokens.size
        if (baseSize == 0 || sideSize == 0) return emptyList()
        val offset = baseSize + sideSize + 1
        val furthestBaseIndex = IntArray(2 * offset + 1)
        val history = ArrayList<IntArray>()
        for (editCount in 0..minOf(baseSize + sideSize, MAX_EDIT_DISTANCE)) {
            history += furthestBaseIndex.copyOfRange(offset - editCount - 1, offset + editCount + 2)
            for (diagonal in -editCount..editCount step 2) {
                val comesFromAbove = diagonal == -editCount || (diagonal != editCount &&
                    furthestBaseIndex[offset + diagonal - 1] < furthestBaseIndex[offset + diagonal + 1])
                var baseIndex = if (comesFromAbove) {
                    furthestBaseIndex[offset + diagonal + 1]
                } else {
                    furthestBaseIndex[offset + diagonal - 1] + 1
                }
                var sideIndex = baseIndex - diagonal
                while (baseIndex < baseSize && sideIndex < sideSize && baseTokens[baseIndex] == sideTokens[sideIndex]) {
                    baseIndex++
                    sideIndex++
                }
                furthestBaseIndex[offset + diagonal] = baseIndex
                if (baseIndex >= baseSize && sideIndex >= sideSize) {
                    return matchesFromHistory(history, baseSize, sideSize)
                }
            }
        }
        return null
    }

    private fun matchesFromHistory(history: List<IntArray>, baseSize: Int, sideSize: Int): List<Pair<Int, Int>> {
        val matches = ArrayList<Pair<Int, Int>>()
        var baseIndex = baseSize
        var sideIndex = sideSize
        for (editCount in history.indices.reversed()) {
            val furthest = history[editCount]
            val diagonal = baseIndex - sideIndex
            val cameFromAbove = diagonal == -editCount || (diagonal != editCount &&
                furthest[diagonal - 1 + editCount + 1] < furthest[diagonal + 1 + editCount + 1])
            val previousDiagonal = if (cameFromAbove) diagonal + 1 else diagonal - 1
            val previousBaseIndex = furthest[previousDiagonal + editCount + 1]
            val previousSideIndex = previousBaseIndex - previousDiagonal
            while (baseIndex > previousBaseIndex && sideIndex > previousSideIndex) {
                baseIndex--
                sideIndex--
                matches += baseIndex to sideIndex
            }
            if (editCount > 0) {
                baseIndex = previousBaseIndex
                sideIndex = previousSideIndex
            }
        }
        return matches
    }

    private fun slideHunksLeft(
        baseTokens: List<String>,
        sideTokens: List<String>,
        baseIndexOfToken: IntArray,
        hunks: List<TextHunk>
    ) {
        for (hunk in hunks) {
            if (hunk.insertsText && !hunk.deletesText) {
                var start = hunk.sideStart
                var end = hunk.sideEnd
                var basePosition = hunk.baseStart
                while (start > 0 && basePosition > 0 &&
                    baseIndexOfToken[start - 1] == basePosition - 1 &&
                    sideTokens[start - 1] == sideTokens[end - 1] &&
                    (if (start == 1) basePosition == 1 else baseIndexOfToken[start - 2] == basePosition - 2)
                ) {
                    baseIndexOfToken[end - 1] = basePosition - 1
                    baseIndexOfToken[start - 1] = -1
                    start--
                    end--
                    basePosition--
                }
            }
            if (hunk.deletesText && !hunk.insertsText) {
                var start = hunk.baseStart
                var end = hunk.baseEnd
                var sidePosition = hunk.sideStart
                while (start > 0 && sidePosition > 0 &&
                    baseIndexOfToken[sidePosition - 1] == start - 1 &&
                    baseTokens[start - 1] == baseTokens[end - 1] &&
                    (if (sidePosition == 1) start == 1 else baseIndexOfToken[sidePosition - 2] == start - 2)
                ) {
                    baseIndexOfToken[sidePosition - 1] = end - 1
                    start--
                    end--
                    sidePosition--
                }
            }
        }
    }

    private fun hunksFromMatches(side: Int, baseSize: Int, baseIndexOfToken: IntArray): List<TextHunk> {
        val hunks = mutableListOf<TextHunk>()
        var previousBaseIndex = -1
        var previousSideIndex = -1
        for (sideIndex in 0..baseIndexOfToken.size) {
            val baseIndex = if (sideIndex == baseIndexOfToken.size) baseSize else baseIndexOfToken[sideIndex]
            if (baseIndex < 0) continue
            if (baseIndex > previousBaseIndex + 1 || sideIndex > previousSideIndex + 1) {
                hunks += TextHunk(side, previousBaseIndex + 1, baseIndex, previousSideIndex + 1, sideIndex)
            }
            previousBaseIndex = baseIndex
            previousSideIndex = sideIndex
        }
        return hunks
    }

    private fun overlappingGroups(hunks: List<TextHunk>): List<HunkGroup> {
        val sortedHunks = hunks.sortedWith(compareBy({ it.baseStart }, { it.baseEnd }, { it.side }))
        val groups = mutableListOf<HunkGroup>()
        var currentHunks = mutableListOf<TextHunk>()
        var groupStart = 0
        var groupEnd = 0
        for (hunk in sortedHunks) {
            if (currentHunks.isNotEmpty() && rangesOverlap(groupStart, groupEnd, hunk.baseStart, hunk.baseEnd)) {
                currentHunks += hunk
                groupEnd = maxOf(groupEnd, hunk.baseEnd)
            } else {
                if (currentHunks.isNotEmpty()) groups += HunkGroup(groupStart, groupEnd, currentHunks)
                currentHunks = mutableListOf(hunk)
                groupStart = hunk.baseStart
                groupEnd = hunk.baseEnd
            }
        }
        if (currentHunks.isNotEmpty()) groups += HunkGroup(groupStart, groupEnd, currentHunks)
        return groups
    }

    private fun rangesOverlap(firstStart: Int, firstEnd: Int, secondStart: Int, secondEnd: Int): Boolean {
        val firstIsInsert = firstStart == firstEnd
        val secondIsInsert = secondStart == secondEnd
        return when {
            firstIsInsert && secondIsInsert -> firstStart == secondStart
            firstIsInsert -> secondStart < firstStart && firstStart < secondEnd
            secondIsInsert -> firstStart < secondStart && secondStart < firstEnd
            else -> firstStart < secondEnd && secondStart < firstEnd
        }
    }

    private fun resolveGroup(
        group: HunkGroup,
        baseTokens: List<String>,
        diffs: List<SideDiff>,
        mergedTokens: MutableList<MergedToken>
    ): Int {
        val newerHunks = group.hunks.filter { it.side == NEWER_SIDE }
        val olderHunks = group.hunks.filter { it.side == OLDER_SIDE }
        val newerVersion = sideVersion(group, newerHunks, baseTokens, diffs[NEWER_SIDE])
        val olderVersion = sideVersion(group, olderHunks, baseTokens, diffs[OLDER_SIDE])
        val newerText = newerVersion.joinToString("") { it.text }
        val olderText = olderVersion.joinToString("") { it.text }
        val newerInsertsText = newerHunks.any { it.insertsText }
        val olderInsertsText = olderHunks.any { it.insertsText }
        when {
            olderHunks.isEmpty() || newerText == olderText -> mergedTokens += newerVersion
            newerHunks.isEmpty() -> mergedTokens += olderVersion
            !olderInsertsText -> mergedTokens += newerVersion.withoutBaseTokensDeletedBy(olderHunks)
            !newerInsertsText -> mergedTokens += olderVersion.withoutBaseTokensDeletedBy(newerHunks)
            newerText.contains(olderText) -> mergedTokens += newerVersion
            olderText.contains(newerText) -> mergedTokens += olderVersion
            group.baseStart == group.baseEnd -> {
                mergedTokens += olderVersion
                mergedTokens += newerVersion
            }
            else -> {
                mergedTokens += newerVersion
                return group.baseEnd - group.baseStart
            }
        }
        return 0
    }

    private fun sideVersion(
        group: HunkGroup,
        sideHunks: List<TextHunk>,
        baseTokens: List<String>,
        diff: SideDiff
    ): List<MergedToken> {
        val version = mutableListOf<MergedToken>()
        var baseCursor = group.baseStart
        for (hunk in sideHunks) {
            addBaseTokens(version, baseTokens, baseCursor, hunk.baseStart)
            for (sideIndex in hunk.sideStart until hunk.sideEnd) {
                version += MergedToken(diff.tokens[sideIndex], baseIndex = -1, side = hunk.side, sideIndex = sideIndex)
            }
            baseCursor = hunk.baseEnd
        }
        addBaseTokens(version, baseTokens, baseCursor, group.baseEnd)
        return version
    }

    private fun List<MergedToken>.withoutBaseTokensDeletedBy(hunks: List<TextHunk>): List<MergedToken> =
        filter { token -> token.baseIndex < 0 || hunks.none { token.baseIndex >= it.baseStart && token.baseIndex < it.baseEnd } }

    private fun addBaseTokens(destination: MutableList<MergedToken>, baseTokens: List<String>, from: Int, until: Int) {
        for (baseIndex in from until until) {
            destination += MergedToken(baseTokens[baseIndex], baseIndex = baseIndex, side = -1, sideIndex = -1)
        }
    }

    private class MergedPositions(
        mergedTokens: List<MergedToken>,
        private val diffs: List<SideDiff>,
        baseSize: Int,
        private val mergedLength: Int
    ) {
        private val mergedStartOfSideToken: List<IntArray> = diffs.map { IntArray(it.tokens.size) { -1 } }
        private val sideCharacterStarts: List<IntArray> = diffs.map { diff ->
            val starts = IntArray(diff.tokens.size)
            var position = 0
            diff.tokens.forEachIndexed { index, token ->
                starts[index] = position
                position += token.length
            }
            starts
        }

        init {
            val mergedStartOfBaseToken = IntArray(baseSize) { -1 }
            var position = 0
            for (token in mergedTokens) {
                if (token.baseIndex >= 0) {
                    mergedStartOfBaseToken[token.baseIndex] = position
                } else {
                    mergedStartOfSideToken[token.side][token.sideIndex] = position
                }
                position += token.text.length
            }
            diffs.forEachIndexed { side, diff ->
                diff.baseIndexOfToken.forEachIndexed { sideIndex, baseIndex ->
                    if (baseIndex >= 0) mergedStartOfSideToken[side][sideIndex] = mergedStartOfBaseToken[baseIndex]
                }
            }
        }

        fun mapSpan(side: Int, span: InlineSpan, sideTextLength: Int): InlineSpan? {
            if (span.start < 0 || span.end > sideTextLength || span.start >= span.end) return null
            val start = mapStart(side, span.start)
            val end = mapEnd(side, span.end - 1)
            return if (end > start) span.copy(start = start, end = end) else null
        }

        private fun tokenIndexAt(side: Int, characterOffset: Int): Int {
            val starts = sideCharacterStarts[side]
            var index = 0
            while (index + 1 < starts.size && starts[index + 1] <= characterOffset) index++
            return index
        }

        private fun mapStart(side: Int, characterOffset: Int): Int {
            val containingToken = tokenIndexAt(side, characterOffset)
            for (tokenIndex in containingToken until diffs[side].tokens.size) {
                val mergedStart = mergedStartOfSideToken[side][tokenIndex]
                if (mergedStart < 0) continue
                return if (tokenIndex == containingToken) {
                    mergedStart + characterOffset - sideCharacterStarts[side][tokenIndex]
                } else {
                    mergedStart
                }
            }
            return mergedLength
        }

        private fun mapEnd(side: Int, lastCharacterOffset: Int): Int {
            val containingToken = tokenIndexAt(side, lastCharacterOffset)
            for (tokenIndex in containingToken downTo 0) {
                val mergedStart = mergedStartOfSideToken[side][tokenIndex]
                if (mergedStart < 0) continue
                return if (tokenIndex == containingToken) {
                    mergedStart + lastCharacterOffset - sideCharacterStarts[side][tokenIndex] + 1
                } else {
                    mergedStart + diffs[side].tokens[tokenIndex].length
                }
            }
            return 0
        }
    }

    private fun mergedSpans(
        base: TextWithSpans,
        newer: TextWithSpans,
        older: TextWithSpans,
        positions: MergedPositions
    ): List<InlineSpan> {
        val baseSpanSignatures = base.spans.mapNotNull { spanSignature(base.text, it) }.toSet()
        val spansAddedByOlder = older.spans.filter { spanSignature(older.text, it) !in baseSpanSignatures }
        val newerSpans = newer.spans.mapNotNull { positions.mapSpan(NEWER_SIDE, it, newer.text.length) }
        val olderSpans = spansAddedByOlder.mapNotNull { positions.mapSpan(OLDER_SIDE, it, older.text.length) }
        return (newerSpans + olderSpans).distinct().sortedWith(compareBy({ it.start }, { it.end }))
    }

    private fun spanSignature(text: String, span: InlineSpan): Pair<String, InlineSpan>? {
        if (span.start < 0 || span.end > text.length || span.start >= span.end) return null
        return text.substring(span.start, span.end) to span.copy(start = 0, end = 0)
    }
}
