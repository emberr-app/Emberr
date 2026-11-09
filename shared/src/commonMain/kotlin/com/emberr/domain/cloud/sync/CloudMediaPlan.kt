package com.emberr.domain.cloud.sync

internal data class CloudMediaLockPlace(val spaceRef: Long, val mediaId: String, val noteId: String)

internal class CloudNoteMedia(
    val spaceRef: Long,
    val noteId: String,
    val fileNames: Set<String>,
    val hasUnsentChanges: Boolean
)

internal class CloudMediaLockToAdd(val place: CloudMediaLockPlace, val fileName: String)

internal class CloudMediaPlan(
    val fileNamesToUpload: List<String>,
    val locksToAdd: List<CloudMediaLockToAdd>,
    val locksToRemove: List<CloudMediaLockPlace>,
    val fileNamesToDownload: List<String>
)

internal fun planCloudMedia(
    notes: List<CloudNoteMedia>,
    mediaIdsByFileName: Map<String, String>,
    liveLocks: Set<CloudMediaLockPlace>,
    fileNamesOnDisk: Set<String>
): CloudMediaPlan {
    val mediaIdsWithALiveLock = liveLocks.mapTo(HashSet()) { it.mediaId }
    val liveLocksByNote = liveLocks.groupBy { it.spaceRef to it.noteId }
    val referencedFileNames = notes.flatMapTo(LinkedHashSet()) { it.fileNames }

    val locksToAdd = notes.flatMap { note ->
        note.fileNames
            .map { fileName ->
                CloudMediaLockToAdd(CloudMediaLockPlace(note.spaceRef, mediaIdsByFileName.getValue(fileName), note.noteId), fileName)
            }
            .filter { lockToAdd -> lockToAdd.place !in liveLocks }
    }
    val locksToRemove = notes.filterNot { it.hasUnsentChanges }.flatMap { note ->
        val referencedMediaIds = note.fileNames.mapTo(HashSet(), mediaIdsByFileName::getValue)
        liveLocksByNote[note.spaceRef to note.noteId].orEmpty().filter { it.mediaId !in referencedMediaIds }
    }

    return CloudMediaPlan(
        fileNamesToUpload = referencedFileNames.filter { fileName ->
            fileName in fileNamesOnDisk && mediaIdsByFileName.getValue(fileName) !in mediaIdsWithALiveLock
        },
        locksToAdd = locksToAdd,
        locksToRemove = locksToRemove,
        fileNamesToDownload = referencedFileNames.filter { it !in fileNamesOnDisk }
    )
}
