package com.emberr.domain.cloud.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.emberr.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val cloudSyncEngine: CloudSyncEngine
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val outcome = cloudSyncEngine.syncAutomaticallyIfThereIsWork(
            waitForRunningPass = true,
            beforeSyncing = { showSyncNotification() }
        )
        when {
            outcome !is CloudSyncResult.Failed -> Result.success()
            runAttemptCount < MAX_RETRY_ATTEMPTS -> Result.retry()
            else -> Result.failure()
        }
    }

    private suspend fun showSyncNotification() {
        try {
            ensureNotificationChannel()
            setForeground(createForegroundInfo())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
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
            .setContentTitle("Syncing with Emberr Cloud…")
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private companion object {
        const val CHANNEL_ID = "emberr_server_sync"
        const val NOTIFICATION_ID = 4823
        const val MAX_RETRY_ATTEMPTS = 5
    }
}
