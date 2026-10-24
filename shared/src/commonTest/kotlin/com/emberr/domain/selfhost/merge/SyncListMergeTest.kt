package com.emberr.domain.selfhost.merge

import com.emberr.data.local.room.entity.CalendarEventExceptionEntity
import com.emberr.data.local.room.entity.FolderEntity
import com.emberr.data.local.room.entity.SelfHostDeletedApiConfigEntity
import com.emberr.domain.ai.external.ExternalAiProvider
import com.emberr.domain.ai.external.ExternalAiProviderConfig
import com.emberr.domain.selfhost.sync.ApiConfigSyncEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SyncListMergeTest {

    private fun folder(name: String, updatedAt: Long) =
        FolderEntity(folderId = "recipes", name = name, parentFolderId = null, createdAt = 1, updatedAt = updatedAt)

    private fun skippedOccurrence(occurrenceDate: String) =
        CalendarEventExceptionEntity(blockId = "standup", occurrenceDate = occurrenceDate, isCancelled = true, updatedAt = 1000)

    private fun openAiEntry(updatedAt: Long, isDeleted: Boolean = false) = ApiConfigSyncEntry(
        provider = ExternalAiProvider.OPENAI.name,
        apiKey = if (isDeleted) "" else "remote-key",
        model = if (isDeleted) "" else "gpt-4o-mini",
        updatedAt = updatedAt,
        isDeleted = isDeleted
    )

    @Test
    fun aFolderRenamedOnBothDevicesAtTheSameMomentTakesTheRemoteName() {
        val merged = mergeFolders(
            localFolders = listOf(folder("Recipes", updatedAt = 1000)),
            remoteFolders = listOf(folder("Cooking", updatedAt = 1000))
        )

        assertEquals(listOf("Cooking"), merged.map { it.name })
    }

    @Test
    fun aFolderRenamedLaterOnThisDeviceKeepsTheLocalName() {
        val merged = mergeFolders(
            localFolders = listOf(folder("Recipes", updatedAt = 1005)),
            remoteFolders = listOf(folder("Cooking", updatedAt = 1000))
        )

        assertEquals(listOf("Recipes"), merged.map { it.name })
    }

    @Test
    fun twoSkippedDatesOfTheSameEventStaySeparate() {
        val merged = mergeEventExceptions(
            localExceptions = listOf(skippedOccurrence("2026-10-20")),
            remoteExceptions = listOf(skippedOccurrence("2026-10-27"))
        )

        assertEquals(listOf("2026-10-20", "2026-10-27"), merged.map { it.occurrenceDate })
    }

    @Test
    fun aKeyDeletedLaterOnTheOtherDeviceIsDeletedHere() {
        val remoteDeletion = openAiEntry(updatedAt = 2000, isDeleted = true)

        val merge = mergeApiConfig(
            provider = ExternalAiProvider.OPENAI,
            localConfig = ExternalAiProviderConfig(apiKey = "local-key", model = "gpt-4o-mini", updatedAt = 1000),
            localTombstone = null,
            remoteEntry = remoteDeletion
        )

        assertEquals(remoteDeletion, merge.remoteEntryToApply)
        assertEquals(remoteDeletion, merge.mergedEntry)
    }

    @Test
    fun aKeySavedOnBothDevicesAtTheSameMomentKeepsTheLocalKey() {
        val merge = mergeApiConfig(
            provider = ExternalAiProvider.OPENAI,
            localConfig = ExternalAiProviderConfig(apiKey = "local-key", model = "gpt-4o-mini", updatedAt = 1000),
            localTombstone = null,
            remoteEntry = openAiEntry(updatedAt = 1000)
        )

        assertNull(merge.remoteEntryToApply)
        assertEquals("local-key", merge.mergedEntry?.apiKey)
    }

    @Test
    fun aKeyDeletedHereAfterTheOtherDeviceSavedItStaysDeleted() {
        val merge = mergeApiConfig(
            provider = ExternalAiProvider.OPENAI,
            localConfig = null,
            localTombstone = SelfHostDeletedApiConfigEntity(provider = ExternalAiProvider.OPENAI.name, deletedAt = 2000),
            remoteEntry = openAiEntry(updatedAt = 1000)
        )

        assertNull(merge.remoteEntryToApply)
        assertEquals(openAiEntry(updatedAt = 2000, isDeleted = true), merge.mergedEntry)
    }
}
