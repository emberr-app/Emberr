package com.emberr.domain.selfhost.sync

import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.data.local.room.entity.SelfHostDeletedNoteEntity

internal class NoteManifestEntries(
    val entries: List<SelfHostManifestEntry>,
    val replacedLocalTombstones: List<SelfHostDeletedNoteEntity>,
    val activeLocalTombstones: List<SelfHostDeletedNoteEntity>
)

internal fun buildNoteManifestEntries(
    serverEntries: List<SelfHostManifestEntry>,
    localNotes: List<NoteMetadataEntity>,
    localTombstones: List<SelfHostDeletedNoteEntity>,
    mediaFileNamesByNoteId: Map<String, Set<String>>,
    noteIdsStillWaitingToUpload: Set<String>,
    oldSpaceDailyVersionsMergedHere: Map<String, Long>
): NoteManifestEntries {
    val previousTombstones = serverEntries.filter { it.isDeleted } +
        retiredOldSpaceDailyEntries(serverEntries, oldSpaceDailyVersionsMergedHere)
    val localEntries = localNotes.map { note ->
        SelfHostManifestEntry(
            entryId = manifestEntryIdFor(note),
            entryType = if (note.isDaily) SelfHostEntryType.DAILY else SelfHostEntryType.NOTE,
            spaceId = note.spaceId,
            updatedAt = note.updatedAt,
            dateString = note.dateString.takeIf { note.isDaily },
            mediaFileNames = mediaFileNamesByNoteId[note.noteId].orEmpty()
        )
    }
    val entryIdsStillWaitingToUpload = localNotes
        .filter { it.noteId in noteIdsStillWaitingToUpload }
        .map { manifestEntryIdFor(it) }
        .toSet()

    val localTombstoneEntriesByNoteId = localTombstones.associate { it.noteId to manifestEntryForTombstone(it) }
    val replacedTombstoneIds = tombstoneIdsReplacedByNewerNotes(
        tombstones = previousTombstones + localTombstoneEntriesByNoteId.values,
        serverEntries = serverEntries,
        localEntries = localEntries,
        entryIdsStillWaitingToUpload = entryIdsStillWaitingToUpload
    )
    val (replacedLocalTombstones, activeLocalTombstones) = localTombstones.partition { tombstone ->
        localTombstoneEntriesByNoteId.getValue(tombstone.noteId).entryId in replacedTombstoneIds
    }

    val mergedTombstonesById = LinkedHashMap<String, SelfHostManifestEntry>()
    previousTombstones
        .filter { it.entryId !in replacedTombstoneIds }
        .forEach { mergedTombstonesById[it.entryId] = it }
    activeLocalTombstones.map { localTombstoneEntriesByNoteId.getValue(it.noteId) }.forEach { entry ->
        val existing = mergedTombstonesById[entry.entryId]
        if (existing == null || entry.updatedAt > existing.updatedAt) {
            mergedTombstonesById[entry.entryId] = entry
        }
    }
    val noteEntries = mergeNoteManifestEntries(
        serverEntries = serverEntries,
        localEntries = localEntries,
        entryIdsStillWaitingToUpload = entryIdsStillWaitingToUpload,
        tombstoneIds = mergedTombstonesById.keys
    )
    return NoteManifestEntries(
        entries = noteEntries + mergedTombstonesById.values,
        replacedLocalTombstones = replacedLocalTombstones,
        activeLocalTombstones = activeLocalTombstones
    )
}

internal fun isLiveOldSpaceDailyEntry(entry: SelfHostManifestEntry): Boolean =
    entry.entryType == SelfHostEntryType.DAILY && entry.spaceId != ACCOUNT_SPACE_ID && !entry.isDeleted

private fun retiredOldSpaceDailyEntries(
    serverEntries: List<SelfHostManifestEntry>,
    oldSpaceDailyVersionsMergedHere: Map<String, Long>
): List<SelfHostManifestEntry> =
    serverEntries
        .filter { entry ->
            val mergedVersion = oldSpaceDailyVersionsMergedHere[entry.entryId]
            isLiveOldSpaceDailyEntry(entry) && mergedVersion != null && entry.updatedAt <= mergedVersion
        }
        .map { entry ->
            SelfHostManifestEntry(
                entryId = entry.entryId,
                entryType = SelfHostEntryType.DAILY,
                spaceId = entry.spaceId,
                updatedAt = entry.updatedAt,
                dateString = entry.dateString,
                isDeleted = true
            )
        }

internal fun manifestEntryForTombstone(tombstone: SelfHostDeletedNoteEntity): SelfHostManifestEntry {
    val isDailyTombstone = tombstone.isDaily && tombstone.dateString != null
    return SelfHostManifestEntry(
        entryId = if (isDailyTombstone) {
            dailyManifestEntryId(tombstone.spaceId, tombstone.dateString.orEmpty())
        } else {
            tombstone.noteId
        },
        entryType = if (tombstone.isDaily) SelfHostEntryType.DAILY else SelfHostEntryType.NOTE,
        spaceId = tombstone.spaceId,
        updatedAt = tombstone.deletedAt,
        dateString = tombstone.dateString,
        isDeleted = true
    )
}

internal fun dailyManifestEntryId(spaceId: String, dateString: String): String =
    "daily_${spaceId}_$dateString"

internal fun manifestEntryIdFor(note: NoteMetadataEntity): String =
    if (note.isDaily && note.dateString != null) {
        dailyManifestEntryId(note.spaceId, note.dateString)
    } else {
        note.noteId
    }
