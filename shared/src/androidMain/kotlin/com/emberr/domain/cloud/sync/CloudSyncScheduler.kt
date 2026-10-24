package com.emberr.domain.cloud.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.emberr.domain.cloud.account.CloudAccountRepository
import com.emberr.domain.cloud.account.CloudSignInState
import com.emberr.domain.sync.AutoSyncTrigger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

actual class CloudSyncScheduler(
    private val context: Context,
    private val cloudSyncEngine: CloudSyncEngine,
    private val cloudAccountRepository: CloudAccountRepository,
    private val cloudSyncPoller: CloudSyncPoller
) {

    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var signedInScope: CoroutineScope? = null

    @Volatile
    private var isAppInForeground = false

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            isAppInForeground = true
            val scope = signedInScope ?: return
            cloudSyncPoller.start()
            scope.launch { cloudSyncEngine.syncAutomatically() }
            scope.launch { updateBackgroundSyncFromSessionCount() }
        }

        override fun onStop(owner: LifecycleOwner) {
            isAppInForeground = false
            cloudSyncPoller.stop()
            if (signedInScope == null || cloudSyncEngine.isPausedUntilAppRestart) return
            scheduleSyncAfterLeavingApp()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (!isAppInForeground) return
            signedInScope?.launch { cloudSyncEngine.syncAutomatically() }
        }
    }

    actual fun start() {
        mainHandler.post { ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver) }
        context.getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(networkCallback)
        schedulerScope.launch {
            try {
                cloudAccountRepository.loadSignedInState()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
            }
            cloudAccountRepository.signInState
                .map { it is CloudSignInState.SignedIn }
                .distinctUntilChanged()
                .collect { isSignedIn -> if (isSignedIn) startSignedInWork() else stopSignedInWork() }
        }
    }

    actual fun syncNow() {
        schedulerScope.launch { cloudSyncEngine.syncNow() }
    }

    private fun startSignedInWork() {
        signedInScope?.cancel()
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        signedInScope = newScope

        AutoSyncTrigger.syncRequests
            .debounce(WAIT_AFTER_LAST_EDIT)
            .onEach { cloudSyncEngine.syncAutomatically() }
            .launchIn(newScope)
        if (isAppInForeground) {
            cloudSyncPoller.start()
            newScope.launch { cloudSyncEngine.syncAutomatically() }
        }
        newScope.launch { updateBackgroundSyncFromSessionCount() }
    }

    private fun stopSignedInWork() {
        signedInScope?.cancel()
        signedInScope = null
        cloudSyncPoller.stop()
        cloudSyncEngine.forgetStatus()
        WorkManager.getInstance(context).apply {
            cancelUniqueWork(WORK_NAME_PERIODIC)
            cancelUniqueWork(WORK_NAME_AFTER_LEAVING_APP)
        }
    }

    private suspend fun updateBackgroundSyncFromSessionCount() {
        val sessionCount = try {
            cloudAccountRepository.listSessions().size
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            return
        }
        if (shouldSyncInBackground(sessionCount)) {
            schedulePeriodicSync()
        } else {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
        }
    }

    private fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(PERIODIC_SYNC_MINUTES, TimeUnit.MINUTES)
            .setConstraints(anyNetworkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    private fun scheduleSyncAfterLeavingApp() {
        val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
            .setConstraints(anyNetworkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME_AFTER_LEAVING_APP, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    private fun anyNetworkConstraints(): Constraints =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    private companion object {
        const val WORK_NAME_PERIODIC = "EmberrCloudPeriodicSync"
        const val WORK_NAME_AFTER_LEAVING_APP = "EmberrCloudSyncAfterLeavingApp"
        const val PERIODIC_SYNC_MINUTES = 30L
        val WAIT_AFTER_LAST_EDIT = 5.seconds
    }
}
