package com.emberr.domain.importer

sealed interface ImportProgress {
    data object NotStarted : ImportProgress
    data class Importing(
        val handledNoteCount: Int,
        val totalNoteCount: Int,
        val doneFraction: Float,
        val copyingFileName: String? = null
    ) : ImportProgress
    data object Stopping : ImportProgress
    data object Stopped : ImportProgress
    data class Finished(val result: ImportResult) : ImportProgress
}

data class ImportResult(
    val spaceId: String,
    val importedNoteCount: Int,
    val createdFolderCount: Int,
    val copiedAttachmentCount: Int,
    val failedNames: List<String>
)

internal class ImportProgressTracker(
    private val totalNoteCount: Int,
    private val totalWorkBytes: Long,
    private val publish: (ImportProgress.Importing) -> Unit
) {
    private var doneWorkBytes = 0L
    private var handledNoteCount = 0
    private var lastPublished: ImportProgress.Importing? = null

    fun noteHandled(noteBytes: Long) {
        handledNoteCount++
        doneWorkBytes += noteBytes
        publishIfVisiblyChanged(copyingFileName = null, bytesInProgress = 0L)
    }

    fun attachmentCopying(fileName: String, copiedBytes: Long) {
        publishIfVisiblyChanged(copyingFileName = fileName, bytesInProgress = copiedBytes)
    }

    fun attachmentFinished(attachmentBytes: Long) {
        doneWorkBytes += attachmentBytes
    }

    private fun publishIfVisiblyChanged(copyingFileName: String?, bytesInProgress: Long) {
        val doneFraction = when {
            totalWorkBytes > 0 -> ((doneWorkBytes + bytesInProgress).toFloat() / totalWorkBytes).coerceIn(0f, 1f)
            totalNoteCount > 0 -> handledNoteCount.toFloat() / totalNoteCount
            else -> 0f
        }
        val progress = ImportProgress.Importing(handledNoteCount, totalNoteCount, doneFraction, copyingFileName)
        val previous = lastPublished
        val looksTheSame = previous != null &&
            previous.handledNoteCount == progress.handledNoteCount &&
            previous.copyingFileName == progress.copyingFileName &&
            wholePercentOf(previous.doneFraction) == wholePercentOf(progress.doneFraction)
        if (looksTheSame) return
        lastPublished = progress
        publish(progress)
    }

    private fun wholePercentOf(fraction: Float): Int = (fraction * 100).toInt()
}
