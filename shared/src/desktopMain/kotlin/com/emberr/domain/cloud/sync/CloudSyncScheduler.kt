package com.emberr.domain.cloud.sync

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
import kotlin.time.Duration.Companion.seconds

actual class CloudSyncScheduler(
    private val cloudSyncEngine: CloudSyncEngine,
    private val cloudAccountRepository: CloudAccountRepository,
    private val cloudSyncPoller: CloudSyncPoller
) {

    private val schedulerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var signedInScope: CoroutineScope? = null

    actual fun start() {
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
        newScope.launch { cloudSyncEngine.syncAutomatically() }
        cloudSyncPoller.start()
    }

    private fun stopSignedInWork() {
        signedInScope?.cancel()
        signedInScope = null
        cloudSyncPoller.stop()
        cloudSyncEngine.forgetStatus()
    }

    private companion object {
        val WAIT_AFTER_LAST_EDIT = 5.seconds
    }
}
