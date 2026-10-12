package com.emberr.domain.selfhost.sync

import com.emberr.data.local.room.entity.NoteMetadataEntity

internal fun serverEntriesToPull(
    serverTextEntries: List<SelfHostManifestEntry>,
    localRowForEntry: (SelfHostManifestEntry) -> NoteMetadataEntity?,
    localDeletedAtByEntryId: Map<String, Long>
): List<SelfHostManifestEntry> =
    serverTextEntries.filter { entry ->
        val localRow = localRowForEntry(entry)
        val changedSinceLastSync = entry.updatedAt > (localRow?.selfHostSyncedAt ?: 0L)
        val deletionOfNoteAlreadyGoneHere = entry.isDeleted && localRow == null
        changedSinceLastSync &&
            !deletionOfNoteAlreadyGoneHere &&
            !wasDeletedHereAfterServerVersion(entry, localDeletedAtByEntryId[entry.entryId])
    }

private fun wasDeletedHereAfterServerVersion(serverEntry: SelfHostManifestEntry, localDeletedAt: Long?): Boolean =
    !serverEntry.isDeleted && localDeletedAt != null && serverEntry.updatedAt <= localDeletedAt
