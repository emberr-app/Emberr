package com.emberr.domain.movie

import com.emberr.domain.model.MovieBlock
import com.emberr.domain.model.NoteBlock
import com.emberr.domain.model.WatchState
import com.emberr.domain.model.titleKey
import com.emberr.domain.model.withWatchState

fun List<NoteBlock>.withWatchStateOnAllCopiesOf(blockId: String, state: WatchState, now: Long): List<NoteBlock> {
    val titleKey = titleKeyOfBlock(blockId)
    return map { block ->
        val isSameTitle = block is MovieBlock && !block.isDeleted && titleKey != null && block.titleKey() == titleKey
        if (block is MovieBlock && (block.id == blockId || isSameTitle)) block.withWatchState(state, now) else block
    }
}

fun List<NoteBlock>.titleKeyOfBlock(blockId: String): String? =
    (firstOrNull { it.id == blockId } as? MovieBlock)?.titleKey()

fun List<NoteBlock>.idsOfBlocksShowingTitle(titleKey: String): Set<String> =
    filter { it is MovieBlock && it.titleKey() == titleKey }.mapTo(HashSet()) { it.id }
