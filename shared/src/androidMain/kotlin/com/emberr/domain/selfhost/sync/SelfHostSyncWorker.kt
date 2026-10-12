package com.emberr.domain.selfhost.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.emberr.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SelfHostSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val selfHostSyncEngine: SelfHostSyncEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val scope = inputData.getString(KEY_SYNC_SCOPE) ?: SCOPE_TEXT
        SelfHostSyncLog.d("Worker: doWork() starting, scope=$scope, runAttemptCount=$runAttemptCount")

        try {
            val outcome = when (scope) {
                SCOPE_MEDIA -> selfHostSyncEngine.syncMediaAfterAnyRunningOne()
                SCOPE_MANUAL -> {
                    showSyncNotification()
                    syncTextThenMedia()
                }
                else -> selfHostSyncEngine.runSyncAfterAnyRunningOne()
            }

            when (outcome) {
                is SelfHostSyncResult.Success -> {
                    SelfHostSyncLog.d(
                        "Worker: scope=$scope succeeded, " +
                                "notesSynced=${outcome.notesSynced}, conflicts=${outcome.conflicts}, " +
                                "notesNotSynced=${outcome.notesNotSynced}"
                    )
                    outcome.notesNotSyncedMessage
                        ?.let { message -> Result.success(workDataOf(KEY_NOTES_NOT_SYNCED_MESSAGE to message)) }
                        ?: Result.success()
                }

                is SelfHostSyncResult.NotConfigured -> {
                    SelfHostSyncLog.d("Worker: self-host sync is not configured, skipping")
                    Result.success()
                }

                is SelfHostSyncResult.AlreadyInProgress -> {
                    SelfHostSyncLog.d("Worker: a sync is already in progress, skipping this run")
                    Result.success()
                }

                is SelfHostSyncResult.WaitingForAllowedNetwork -> {
                    SelfHostSyncLog.d("Worker: this network is not one the user picked for sync, skipping this run")
                    Result.success()
                }

                is SelfHostSyncResult.Cancelled -> {
                    SelfHostSyncLog.d("Worker: sync was cancelled by the user, not retrying")
                    Result.success()
                }

                is SelfHostSyncResult.Failure -> {
                    val message =
                        outcome.cause.localizedMessage ?: outcome.cause.message ?: "Sync failed"
                    SelfHostSyncLog.e("Worker: scope=$scope failed: $message", outcome.cause)
                    if (outcome.cause is SelfHostManifestTooNewException) {
                        Result.failure(workDataOf(KEY_ERROR_MESSAGE to message))
                    } else {
                        retryOrGiveUp(message)
                    }
                }
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Exception) {
            val message = cause.localizedMessage ?: cause.message ?: "Unexpected sync error"
            SelfHostSyncLog.e(
                "Worker: unexpected exception during scope=$scope sync: $message",
                cause
            )
            retryOrGiveUp(message)
        }
    }

    private suspend fun syncTextThenMedia(): SelfHostSyncResult {
        val textOutcome = selfHostSyncEngine.runSyncAfterAnyRunningOne()
        if (textOutcome !is SelfHostSyncResult.Success) return textOutcome
        val mediaOutcome = selfHostSyncEngine.syncMediaAfterAnyRunningOne()
        return if (mediaOutcome is SelfHostSyncResult.Success) textOutcome else mediaOutcome
    }

    private suspend fun showSyncNotification() {
        try {
            ensureNotificationChannel()
            setForeground(createForegroundInfo())
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Exception) {
            SelfHostSyncLog.e("Worker: could not show the sync notification, syncing without it", cause)
        }
    }

    private fun ensureNotificationChannel() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Server sync", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Syncing with your server")
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun retryOrGiveUp(message: String): Result {
        if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
            SelfHostSyncLog.d("Worker: will retry (attempt $runAttemptCount of $MAX_RETRY_ATTEMPTS)")
            return Result.retry()
        }
        SelfHostSyncLog.e("Worker: giving up after $runAttemptCount attempts")
        return Result.failure(workDataOf(KEY_ERROR_MESSAGE to message))
    }

    companion object {
        const val KEY_SYNC_SCOPE = "self_host_sync_scope"
        const val SCOPE_TEXT = "text"
        const val SCOPE_MEDIA = "media"
        const val SCOPE_MANUAL = "manual"
        const val KEY_ERROR_MESSAGE = "self_host_sync_error_message"
        const val KEY_NOTES_NOT_SYNCED_MESSAGE = "self_host_sync_notes_not_synced_message"
        const val MAX_RETRY_ATTEMPTS = 5
        private const val CHANNEL_ID = "emberr_server_sync"
        private const val NOTIFICATION_ID = 4822
    }
}
