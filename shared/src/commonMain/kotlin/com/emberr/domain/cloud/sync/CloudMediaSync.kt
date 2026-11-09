package com.emberr.domain.cloud.sync

import com.emberr.data.local.room.dao.CloudItemDao
import com.emberr.data.local.room.dao.CloudMediaLockDao
import com.emberr.data.local.room.dao.NoteDao
import com.emberr.data.local.room.entity.ACCOUNT_SPACE_ID
import com.emberr.data.local.room.entity.CloudItemEntity
import com.emberr.data.local.room.entity.CloudMediaLockEntity
import com.emberr.domain.cloud.api.CloudApiClient
import com.emberr.domain.cloud.api.CloudApiErrorCode
import com.emberr.domain.cloud.api.CloudApiException
import com.emberr.domain.cloud.api.CloudBlobTransferException
import com.emberr.domain.cloud.api.CloudMediaDownloadUrls
import com.emberr.domain.cloud.api.CloudServerUnreachableException
import com.emberr.domain.cloud.api.toBase64
import com.emberr.domain.cloud.crypto.CloudDecryptionException
import com.emberr.domain.cloud.crypto.CloudEncryptedBytes
import com.emberr.domain.cloud.keys.CloudItemKeyManager
import com.emberr.domain.cloud.keys.CloudItemType
import com.emberr.domain.cloud.keys.CloudMediaCrypto
import com.emberr.domain.cloud.keys.plainMediaSizeOf
import com.emberr.domain.media.MediaReferenceIndex
import com.emberr.domain.selfhost.sync.SelfHostSyncProgress
import com.emberr.domain.selfhost.sync.SelfHostSyncStage
import com.emberr.domain.sync.MediaTransferPhase
import com.emberr.domain.sync.MediaTransferStatusBus
import com.emberr.domain.sync.SyncNetworkRule
import com.emberr.domain.util.media.MediaStorageHelper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Clock

private const val PROGRESS_REPORT_STEP_BYTES = 256L * 1024

data class CloudMediaFileInTransfer(
    val fileName: String,
    val noteTitle: String,
    val isUpload: Boolean,
    val bytesDone: Long,
    val sizeBytes: Long
)

data class CloudMediaPassDetails(
    val filesSent: Int = 0,
    val filesToSend: Int = 0,
    val filesReceived: Int = 0,
    val filesToReceive: Int = 0,
    val filesWaitingForUpload: Int = 0,
    val fileInTransfer: CloudMediaFileInTransfer? = null
)

data class CloudMediaPassResult(
    val finishedAtMillis: Long,
    val filesTooLargeCount: Int,
    val filesNotSentCount: Int,
    val filesNotReceivedCount: Int,
    val filesWaitingForUploadCount: Int,
    val stoppedBeforeTheEnd: Boolean,
    val lastProblem: String?
)

internal fun describeMediaFailure(error: Exception): String = when (error) {
    is CloudApiException -> "Emberr Cloud answered ${error.code} (HTTP ${error.httpStatus})"
    is CloudBlobTransferException -> "file storage answered HTTP ${error.httpStatus}"
    is CloudServerUnreachableException -> "the server could not be reached"
    is CloudDecryptionException -> "the file could not be decrypted"
    is CloudKeyNotFoundException -> "the note's key is missing on this device"
    is CloudWaitingForAllowedNetworkException -> "this network is not one picked for sync"
    else -> error::class.simpleName ?: "unknown error"
}

private class LocalNoteMedia(val media: CloudNoteMedia, val title: String)

private class MediaPass(
    val sessionToken: String,
    val accountKey: ByteArray,
    val streamsBySpaceRef: Map<Long, CloudStream>,
    val mediaIdsByFileName: Map<String, String>,
    val noteTitlesByFileName: Map<String, String>,
    val fileNamesOnDisk: Set<String>,
    val committedMediaIds: MutableSet<String>
) {
    var uploadsAreStopped = false
    var tooLargeCount = 0
    var notSentCount = 0
    var notReceivedCount = 0
    var lastProblem: String? = null
    var filesWaitingForUpload = 0
    var bytesOfFinishedUploads = 0L
    var bytesOfFinishedDownloads = 0L
}

private class FileToDownload(val fileName: String, val mediaId: String, val urls: CloudMediaDownloadUrls) {
    val sizeBytes = plainMediaSizeOf(urls.pieces.map { it.sizeBytes })
}

private enum class UploadOutcome { SENT, TOO_LARGE, REFUSED_BY_PLAN, FAILED }

class CloudMediaSync(
    private val apiClient: CloudApiClient,
    private val fileTransfer: CloudMediaFileTransfer,
    private val mediaCrypto: CloudMediaCrypto,
    private val itemKeyManager: CloudItemKeyManager,
    cloudItemDao: CloudItemDao,
    private val cloudMediaLockDao: CloudMediaLockDao,
    private val noteDao: NoteDao,
    private val mediaReferenceIndex: MediaReferenceIndex,
    private val mediaStorageHelper: MediaStorageHelper,
    private val syncNetworkRule: SyncNetworkRule
) {

    private val streamRecords = CloudStreamRecords(itemKeyManager, cloudItemDao)

    private val mutableProgress = MutableStateFlow<SelfHostSyncProgress?>(null)
    val progress: StateFlow<SelfHostSyncProgress?> = mutableProgress.asStateFlow()

    private val mutableDetails = MutableStateFlow<CloudMediaPassDetails?>(null)
    val details: StateFlow<CloudMediaPassDetails?> = mutableDetails.asStateFlow()

    internal suspend fun syncMedia(
        sessionToken: String,
        streams: List<CloudStream>,
        beforeTransferringFiles: suspend () -> Unit
    ): CloudMediaPassResult {
        stopUnlessOnAllowedNetwork()
        val accountStream = streams.firstOrNull { it.isAccountStream }
            ?: return CloudMediaPassResult(
                finishedAtMillis = nowMillis(),
                filesTooLargeCount = 0,
                filesNotSentCount = 0,
                filesNotReceivedCount = 0,
                filesWaitingForUploadCount = 0,
                stoppedBeforeTheEnd = false,
                lastProblem = "This device has no account stream yet. Sync notes first."
            )
        mutableProgress.value = SelfHostSyncProgress()
        try {
            val accountKey = streamRecords.openKeyOf(accountStream, ACCOUNT_SPACE_ID)
            val localNotes = notesWithMedia(streams)
            val notes = localNotes.map { it.media }
            val referencedFileNames = notes.flatMapTo(LinkedHashSet()) { it.fileNames }
            val mediaIdsByFileName = referencedFileNames.associateWith { fileName -> mediaCrypto.mediaId(accountKey, fileName) }
            val fileNamesOnDisk = withContext(Dispatchers.IO) { referencedFileNames.filterTo(HashSet()) { mediaFileOf(it).exists() } }
            val liveLocks = cloudMediaLockDao.getLiveLocks().mapTo(HashSet()) { CloudMediaLockPlace(it.spaceRef, it.mediaId, it.noteId) }
            val plan = planCloudMedia(notes, mediaIdsByFileName, liveLocks, fileNamesOnDisk)
            val pass = MediaPass(
                sessionToken = sessionToken,
                accountKey = accountKey,
                streamsBySpaceRef = streams.associateBy { it.spaceRef },
                mediaIdsByFileName = mediaIdsByFileName,
                noteTitlesByFileName = noteTitlesByFileName(localNotes),
                fileNamesOnDisk = fileNamesOnDisk,
                committedMediaIds = liveLocks.mapTo(HashSet()) { it.mediaId }
            )

            val filesToDownload = findFilesToDownload(pass, plan.fileNamesToDownload)
            mutableProgress.value = SelfHostSyncProgress(
                stage = SelfHostSyncStage.TRANSFERRING,
                toSend = withContext(Dispatchers.IO) { plan.fileNamesToUpload.sumOf { mediaFileOf(it).length() } },
                toReceive = filesToDownload.sumOf { it.sizeBytes }
            )
            mutableDetails.value = CloudMediaPassDetails(
                filesToSend = plan.fileNamesToUpload.size,
                filesToReceive = filesToDownload.size,
                filesWaitingForUpload = pass.filesWaitingForUpload
            )
            if (plan.fileNamesToUpload.isNotEmpty() || filesToDownload.isNotEmpty()) beforeTransferringFiles()

            for (fileName in plan.fileNamesToUpload) {
                if (pass.uploadsAreStopped) break
                uploadTracked(pass, fileName)
            }
            for (lockToAdd in plan.locksToAdd) {
                if (pass.uploadsAreStopped) break
                if (lockToAdd.place.mediaId in pass.committedMediaIds) addLockUnlessRefused(pass, lockToAdd)
            }
            plan.locksToRemove.forEach { place -> removeLockUnlessRefused(pass, place) }
            filesToDownload.forEach { fileToDownload -> downloadTracked(pass, fileToDownload) }
            return CloudMediaPassResult(
                finishedAtMillis = nowMillis(),
                filesTooLargeCount = pass.tooLargeCount,
                filesNotSentCount = pass.notSentCount,
                filesNotReceivedCount = pass.notReceivedCount,
                filesWaitingForUploadCount = pass.filesWaitingForUpload,
                stoppedBeforeTheEnd = false,
                lastProblem = pass.lastProblem
            )
        } finally {
            mutableProgress.value = null
            mutableDetails.value = null
        }
    }

    private suspend fun notesWithMedia(streams: List<CloudStream>): List<LocalNoteMedia> {
        val fileNamesByNoteId = mediaReferenceIndex.loadReferencedFileNamesByNoteId()
        val localNotesById = noteDao.getAllNotesForBackup().associateBy { it.noteId }
        return streams.flatMap { stream ->
            stream.rowsByItemId.values.filter { it.isLiveNoteRow() }.mapNotNull { row ->
                val note = localNotesById[row.localId] ?: return@mapNotNull null
                val noteBelongsInThisRow = streamSpaceIdOf(note) == stream.appSpaceId &&
                    note.isDaily == (row.itemType == CloudItemType.DAILY.name)
                if (!noteBelongsInThisRow) return@mapNotNull null
                LocalNoteMedia(
                    media = CloudNoteMedia(
                        spaceRef = stream.spaceRef,
                        noteId = row.itemId,
                        fileNames = fileNamesByNoteId[note.noteId].orEmpty(),
                        hasUnsentChanges = note.updatedAt > note.cloudSyncedAt
                    ),
                    title = when {
                        note.title.isNotBlank() -> note.title
                        note.isDaily && note.dateString != null -> note.dateString
                        else -> "Untitled"
                    }
                )
            }
        }
    }

    private fun noteTitlesByFileName(localNotes: List<LocalNoteMedia>): Map<String, String> {
        val titlesByFileName = HashMap<String, String>()
        localNotes.forEach { note -> note.media.fileNames.forEach { fileName -> titlesByFileName.putIfAbsent(fileName, note.title) } }
        return titlesByFileName
    }

    private suspend fun findFilesToDownload(pass: MediaPass, fileNames: List<String>): List<FileToDownload> =
        fileNames.mapNotNull { fileName ->
            val mediaId = pass.mediaIdsByFileName.getValue(fileName)
            try {
                FileToDownload(fileName, mediaId, apiClient.requestMediaDownloadUrls(pass.sessionToken, mediaId))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                if (endsTheWholePass(error)) throw error
                if (error is CloudApiException && error.code == CloudApiErrorCode.NOT_FOUND) {
                    MediaTransferStatusBus.markWaitingForUpload(fileName)
                    pass.filesWaitingForUpload++
                } else {
                    MediaTransferStatusBus.markFinished(fileName, succeeded = false)
                    pass.notReceivedCount++
                    pass.lastProblem = "Could not ask for a file to download: ${describeMediaFailure(error)}"
                }
                null
            }
        }

    private suspend fun uploadTracked(pass: MediaPass, fileName: String): Boolean {
        stopUnlessOnAllowedNetwork()
        val mediaId = pass.mediaIdsByFileName.getValue(fileName)
        val sourceFile = mediaFileOf(fileName)
        val fileSizeBytes = withContext(Dispatchers.IO) { sourceFile.length() }
        MediaTransferStatusBus.markStarted(fileName, MediaTransferPhase.UPLOADING)
        showFileInTransfer(pass, fileName, isUpload = true, sizeBytes = fileSizeBytes)
        val reportFileBytes = fileBytesReporter(fileName, fileSizeBytes)
        val outcome = try {
            fileTransfer.upload(
                sessionToken = pass.sessionToken,
                mediaId = mediaId,
                mediaKey = mediaKeyOf(pass, mediaId),
                sourceFile = sourceFile,
                beforeEachPiece = ::stopUnlessOnAllowedNetwork
            ) { bytesDone ->
                reportBytesSent(pass.bytesOfFinishedUploads + bytesDone)
                reportFileBytes(bytesDone)
            }
            UploadOutcome.SENT
        } catch (cancellation: CancellationException) {
            MediaTransferStatusBus.markDeferred(fileName)
            throw cancellation
        } catch (error: Exception) {
            if (endsTheWholePass(error)) {
                MediaTransferStatusBus.markDeferred(fileName)
                throw error
            }
            pass.lastProblem = when (uploadOutcomeOf(error)) {
                UploadOutcome.REFUSED_BY_PLAN -> "Uploads stopped: ${describeMediaFailure(error)}"
                else -> "Could not send a file: ${describeMediaFailure(error)}"
            }
            uploadOutcomeOf(error)
        }
        pass.bytesOfFinishedUploads += fileSizeBytes
        reportBytesSent(pass.bytesOfFinishedUploads)
        mutableDetails.update { details -> details?.copy(filesSent = details.filesSent + 1, fileInTransfer = null) }
        when (outcome) {
            UploadOutcome.SENT -> {
                pass.committedMediaIds += mediaId
                MediaTransferStatusBus.markFinished(fileName, succeeded = true)
            }
            UploadOutcome.TOO_LARGE -> {
                pass.tooLargeCount++
                MediaTransferStatusBus.markFinished(fileName, succeeded = false)
            }
            UploadOutcome.REFUSED_BY_PLAN -> {
                pass.uploadsAreStopped = true
                pass.notSentCount++
                MediaTransferStatusBus.markDeferred(fileName)
            }
            UploadOutcome.FAILED -> {
                pass.notSentCount++
                MediaTransferStatusBus.markFinished(fileName, succeeded = false)
            }
        }
        return outcome == UploadOutcome.SENT
    }

    private suspend fun addLockUnlessRefused(pass: MediaPass, lockToAdd: CloudMediaLockToAdd) {
        try {
            addLock(pass, lockToAdd)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            if (endsTheWholePass(error)) throw error
            if (uploadOutcomeOf(error) == UploadOutcome.REFUSED_BY_PLAN) pass.uploadsAreStopped = true
            pass.lastProblem = "Could not link a file to its note: ${describeMediaFailure(error)}"
        }
    }

    private suspend fun addLock(pass: MediaPass, lockToAdd: CloudMediaLockToAdd) {
        val place = lockToAdd.place
        val noteKey = streamRecords.openKeyOf(pass.streamsBySpaceRef.getValue(place.spaceRef), place.noteId)
        val lockedKey = itemKeyManager.lockChildKey(mediaKeyOf(pass, place.mediaId), place.mediaId, noteKey, place.noteId)
        val revision = try {
            apiClient.addMediaLock(pass.sessionToken, place.spaceRef, place.mediaId, place.noteId, lockedKey)
        } catch (error: CloudApiException) {
            if (error.code != CloudApiErrorCode.NOT_FOUND || lockToAdd.fileName !in pass.fileNamesOnDisk) throw error
            pass.committedMediaIds -= place.mediaId
            val fileSizeBytes = withContext(Dispatchers.IO) { mediaFileOf(lockToAdd.fileName).length() }
            mutableProgress.update { progress -> progress?.copy(toSend = progress.toSend + fileSizeBytes) }
            mutableDetails.update { details -> details?.copy(filesToSend = details.filesToSend + 1) }
            if (!uploadTracked(pass, lockToAdd.fileName)) return
            apiClient.addMediaLock(pass.sessionToken, place.spaceRef, place.mediaId, place.noteId, lockedKey)
        }
        saveLock(place, lockedKey, revision)
    }

    private suspend fun removeLockUnlessRefused(pass: MediaPass, place: CloudMediaLockPlace) {
        try {
            val revision = apiClient.removeMediaLock(pass.sessionToken, place.spaceRef, place.mediaId, place.noteId)
            saveLock(place, lockedKey = null, revision = revision)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            if (endsTheWholePass(error)) throw error
            pass.lastProblem = "Could not unlink a removed file: ${describeMediaFailure(error)}"
        }
    }

    private suspend fun saveLock(place: CloudMediaLockPlace, lockedKey: CloudEncryptedBytes?, revision: Long) {
        cloudMediaLockDao.upsertLocks(
            listOf(
                CloudMediaLockEntity(
                    spaceRef = place.spaceRef,
                    mediaId = place.mediaId,
                    noteId = place.noteId,
                    encryptedKey = lockedKey?.cipherText?.toBase64(),
                    encryptedKeyNonce = lockedKey?.nonce?.toBase64(),
                    revision = revision,
                    isDeleted = lockedKey == null
                )
            )
        )
    }

    private suspend fun downloadTracked(pass: MediaPass, fileToDownload: FileToDownload) {
        stopUnlessOnAllowedNetwork()
        val fileName = fileToDownload.fileName
        MediaTransferStatusBus.markStarted(fileName, MediaTransferPhase.DOWNLOADING)
        showFileInTransfer(pass, fileName, isUpload = false, sizeBytes = fileToDownload.sizeBytes)
        val reportFileBytes = fileBytesReporter(fileName, fileToDownload.sizeBytes)
        val succeeded = try {
            fileTransfer.download(
                sessionToken = pass.sessionToken,
                mediaId = fileToDownload.mediaId,
                mediaKey = mediaKeyOf(pass, fileToDownload.mediaId),
                firstAnswer = fileToDownload.urls,
                destinationFile = mediaFileOf(fileName),
                beforeEachPiece = ::stopUnlessOnAllowedNetwork
            ) { bytesDone ->
                reportBytesReceived(pass.bytesOfFinishedDownloads + bytesDone)
                reportFileBytes(bytesDone)
            }
            true
        } catch (cancellation: CancellationException) {
            MediaTransferStatusBus.markDeferred(fileName)
            throw cancellation
        } catch (error: Exception) {
            if (endsTheWholePass(error)) {
                MediaTransferStatusBus.markDeferred(fileName)
                throw error
            }
            pass.notReceivedCount++
            pass.lastProblem = "Could not receive a file: ${describeMediaFailure(error)}"
            false
        }
        MediaTransferStatusBus.markFinished(fileName, succeeded)
        pass.bytesOfFinishedDownloads += fileToDownload.sizeBytes
        reportBytesReceived(pass.bytesOfFinishedDownloads)
        mutableDetails.update { details -> details?.copy(filesReceived = details.filesReceived + 1, fileInTransfer = null) }
    }

    private suspend fun mediaKeyOf(pass: MediaPass, mediaId: String): ByteArray =
        mediaCrypto.mediaKey(pass.accountKey, mediaId)

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private fun mediaFileOf(fileName: String): File = File(mediaStorageHelper.getAbsoluteMediaPath(fileName))

    private fun stopUnlessOnAllowedNetwork() {
        if (!syncNetworkRule.isOnAllowedNetwork()) throw CloudWaitingForAllowedNetworkException()
    }

    private fun endsTheWholePass(error: Exception): Boolean =
        error is CloudServerUnreachableException ||
            error is CloudWaitingForAllowedNetworkException ||
            (error is CloudApiException && error.code in setOf(CloudApiErrorCode.INVALID_TOKEN, CloudApiErrorCode.UPGRADE_REQUIRED))

    private fun uploadOutcomeOf(error: Exception): UploadOutcome = when ((error as? CloudApiException)?.code) {
        CloudApiErrorCode.REQUEST_TOO_LARGE -> UploadOutcome.TOO_LARGE
        CloudApiErrorCode.NO_ACTIVE_SUBSCRIPTION, CloudApiErrorCode.STORAGE_LIMIT_EXCEEDED -> UploadOutcome.REFUSED_BY_PLAN
        else -> UploadOutcome.FAILED
    }

    private fun reportBytesSent(bytesSent: Long) {
        mutableProgress.update { progress ->
            if (progress == null || !isWorthReporting(progress.sent, bytesSent, progress.toSend)) progress
            else progress.copy(sent = bytesSent)
        }
    }

    private fun reportBytesReceived(bytesReceived: Long) {
        mutableProgress.update { progress ->
            if (progress == null || !isWorthReporting(progress.received, bytesReceived, progress.toReceive)) progress
            else progress.copy(received = bytesReceived)
        }
    }

    private fun showFileInTransfer(pass: MediaPass, fileName: String, isUpload: Boolean, sizeBytes: Long) {
        val fileInTransfer = CloudMediaFileInTransfer(
            fileName = fileName,
            noteTitle = pass.noteTitlesByFileName[fileName] ?: "Untitled",
            isUpload = isUpload,
            bytesDone = 0L,
            sizeBytes = sizeBytes
        )
        mutableDetails.update { details -> details?.copy(fileInTransfer = fileInTransfer) }
    }

    private fun fileBytesReporter(fileName: String, fileSizeBytes: Long): (Long) -> Unit {
        var reportedBytes = 0L
        return { bytesDone ->
            if (isWorthReporting(reportedBytes, bytesDone, fileSizeBytes)) {
                reportedBytes = bytesDone
                MediaTransferStatusBus.reportBytes(fileName, bytesDone, fileSizeBytes)
                mutableDetails.update { details ->
                    details?.copy(fileInTransfer = details.fileInTransfer?.copy(bytesDone = bytesDone))
                }
            }
        }
    }

    private fun isWorthReporting(reportedBytes: Long, newBytes: Long, totalBytes: Long): Boolean =
        newBytes > reportedBytes &&
            (newBytes - reportedBytes >= PROGRESS_REPORT_STEP_BYTES || newBytes >= totalBytes)

    private fun CloudItemEntity.isLiveNoteRow(): Boolean =
        !isDeleted && (itemType == CloudItemType.NOTE.name || itemType == CloudItemType.DAILY.name)
}
