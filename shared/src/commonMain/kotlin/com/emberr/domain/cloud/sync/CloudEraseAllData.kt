package com.emberr.domain.cloud.sync

import com.emberr.domain.cloud.account.CloudAccountRepository
import com.emberr.domain.cloud.api.CloudApiErrorCode
import com.emberr.domain.cloud.api.CloudApiException
import com.emberr.domain.util.system.eraseAllAppData
import kotlinx.coroutines.CancellationException

sealed interface CloudEraseResult {
    data object Erased : CloudEraseResult
    data object ThisDeviceNotCleared : CloudEraseResult
    class NothingErased(val error: Exception) : CloudEraseResult
}

suspend fun eraseCloudDataAndThisDevice(accountRepository: CloudAccountRepository, password: String): CloudEraseResult {
    CloudSyncPassLock.mutex.lock()
    try {
        accountRepository.eraseAccountData(password)
    } catch (cancellation: CancellationException) {
        CloudSyncPassLock.mutex.unlock()
        throw cancellation
    } catch (error: Exception) {
        CloudSyncPassLock.mutex.unlock()
        if (error is CloudApiException && error.code == CloudApiErrorCode.INVALID_TOKEN) {
            accountRepository.clearLocalAccount()
        }
        return CloudEraseResult.NothingErased(error)
    }

    val thisDeviceWasCleared = try {
        eraseAllAppData(keepCloudAccount = true)
    } catch (cancellation: CancellationException) {
        CloudSyncPassLock.mutex.unlock()
        throw cancellation
    } catch (_: Exception) {
        false
    }
    if (!thisDeviceWasCleared) {
        CloudSyncPassLock.mutex.unlock()
        return CloudEraseResult.ThisDeviceNotCleared
    }
    return CloudEraseResult.Erased
}
