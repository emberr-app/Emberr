package com.emberr.domain.ai.models

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
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_model_download_did_not_complete
import emberr.shared.generated.resources.message_model_download_missing_name
import emberr.shared.generated.resources.message_model_download_paused
import emberr.shared.generated.resources.message_model_download_stopped_before_finish
import emberr.shared.generated.resources.message_model_download_stopped_unexpectedly
import emberr.shared.generated.resources.message_model_download_unknown_model
import org.jetbrains.compose.resources.getString

class ModelDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val modelDownloadManager: ModelDownloadManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val fileName = inputData.getString(KEY_FILE_NAME)
        if (fileName == null) {
            ModelDownloadLog.e("Worker started without a model file name")
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to getString(Res.string.message_model_download_missing_name)))
        }
        val initialFraction = resumeProgressFraction(fileName)
        showDownloadNotification(fileName, initialFraction)
        setProgress(workDataOf(KEY_PROGRESS_FRACTION to initialFraction))

        val downloadFlow = when (fileName) {
            ModelFileNames.EMBEDDER -> modelDownloadManager.downloadEmbeddingModel()
            ModelFileNames.GENERATOR -> modelDownloadManager.downloadGeneratorModel()
            else -> {
                ModelDownloadLog.e("Worker was asked for an unknown model file name $fileName")
                return Result.failure(workDataOf(KEY_ERROR_MESSAGE to getString(Res.string.message_model_download_unknown_model)))
            }
        }

        var outcome: ModelDownloadProgress = ModelDownloadProgress.Failed(getString(Res.string.message_model_download_did_not_complete))
        var lastNotifiedPercent = (initialFraction * 100).toInt()
        try {
            downloadFlow.collect { progress ->
                outcome = progress
                if (progress is ModelDownloadProgress.Downloading) {
                    setProgress(workDataOf(KEY_PROGRESS_FRACTION to progress.fraction))
                    val percent = (progress.fraction * 100).toInt()
                    if (percent != lastNotifiedPercent) {
                        lastNotifiedPercent = percent
                        showDownloadNotification(fileName, progress.fraction)
                    }
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (cause: Exception) {
            ModelDownloadLog.e("Worker threw while downloading $fileName", cause)
            return Result.failure(workDataOf(KEY_ERROR_MESSAGE to getString(Res.string.message_model_download_stopped_unexpectedly)))
        }

        return when (val result = outcome) {
            ModelDownloadProgress.Completed -> Result.success()

            is ModelDownloadProgress.Failed -> {
                ModelDownloadLog.e("Worker reported a failed $fileName download: ${result.message}")
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to result.message))
            }

            is ModelDownloadProgress.Downloading -> {
                ModelDownloadLog.e("Worker stopped mid download of $fileName at ${result.fraction}")
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to getString(Res.string.message_model_download_stopped_before_finish)))
            }

            ModelDownloadProgress.Paused ->
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to getString(Res.string.message_model_download_paused)))
        }
    }

    private suspend fun showDownloadNotification(fileName: String, fraction: Float) {
        try {
            ensureNotificationChannel()
            setForeground(createForegroundInfo(fileName, fraction))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (cause: Exception) {
            ModelDownloadLog.e("Could not show the download notification for $fileName", cause)
        }
    }

    private fun ensureNotificationChannel() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, applicationContext.getString(R.string.notification_channel_model_downloads), NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun createForegroundInfo(fileName: String, fraction: Float): ForegroundInfo {
        val title = applicationContext.getString(
            if (fileName == ModelFileNames.EMBEDDER) R.string.notification_downloading_embedding_model else R.string.notification_downloading_ai_model
        )
        val percent = (fraction * 100).toInt()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(applicationContext.getString(R.string.notification_percent, percent))
            .setSmallIcon(R.drawable.ic_notification)
            .setProgress(100, percent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val KEY_FILE_NAME = "file_name"
        const val KEY_PROGRESS_FRACTION = "progress_fraction"
        const val KEY_ERROR_MESSAGE = "error_message"
        private const val CHANNEL_ID = "emberr_model_downloads"
        private const val NOTIFICATION_ID = 4821
    }
}
