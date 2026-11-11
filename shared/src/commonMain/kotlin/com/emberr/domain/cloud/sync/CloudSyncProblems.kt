package com.emberr.domain.cloud.sync

sealed interface CloudSyncProblem {
    data object SignedOutButRecordsNotCleared : CloudSyncProblem
    data class CloudRefusedSync(val errorCode: String) : CloudSyncProblem
    data object ServerUnreachable : CloudSyncProblem
    data class StorageLinkRefused(val httpStatus: Int) : CloudSyncProblem
    data object CouldNotDecrypt : CloudSyncProblem
    data object NoteUnreadable : CloudSyncProblem
    data object SomethingWentWrong : CloudSyncProblem
}

enum class CloudMediaStep {
    WHOLE_PASS,
    ASKING_FOR_DOWNLOAD,
    SENDING,
    LINKING_TO_NOTE,
    UNLINKING_REMOVED_FILE,
    RECEIVING
}

sealed interface CloudMediaFailure {
    data class CloudAnswered(val errorCode: String, val httpStatus: Int) : CloudMediaFailure
    data class StorageAnswered(val httpStatus: Int) : CloudMediaFailure
    data object ServerUnreachable : CloudMediaFailure
    data object NotDecrypted : CloudMediaFailure
    data object KeyMissing : CloudMediaFailure
    data object NetworkNotPicked : CloudMediaFailure
    data class Other(val errorName: String?) : CloudMediaFailure
}

sealed interface CloudMediaProblem {
    data object NoAccountStream : CloudMediaProblem
    data class StepFailed(val step: CloudMediaStep, val failure: CloudMediaFailure) : CloudMediaProblem
}
