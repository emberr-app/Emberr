package com.emberr.presentation.applock

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface SystemUnlockResult {
    data object Unlocked : SystemUnlockResult
    data object Cancelled : SystemUnlockResult
    data class Failed(val message: String) : SystemUnlockResult
}

fun Context.phoneHasScreenLock(): Boolean =
    getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true

private const val fingerprintFaceOrScreenLock =
    Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL

private val errorCodesThatMeanCancelled = setOf(
    BiometricPrompt.ERROR_USER_CANCELED,
    BiometricPrompt.ERROR_CANCELED
)

class SystemUnlockPrompt(
    activity: FragmentActivity,
    private val onUnlockedWithNobodyWaiting: () -> Unit
) {
    private var waitingCaller: CancellableContinuation<SystemUnlockResult>? = null

    private val biometricPrompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (waitingCaller == null) onUnlockedWithNobodyWaiting() else finishWith(SystemUnlockResult.Unlocked)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val outcome = if (errorCode in errorCodesThatMeanCancelled) {
                    SystemUnlockResult.Cancelled
                } else {
                    SystemUnlockResult.Failed(errString.toString())
                }
                finishWith(outcome)
            }
        }
    )

    val isShowing: Boolean get() = waitingCaller != null

    suspend fun ask(title: String): SystemUnlockResult {
        if (waitingCaller != null) return SystemUnlockResult.Cancelled

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("Use your fingerprint, face, or screen lock")
            .setAllowedAuthenticators(fingerprintFaceOrScreenLock)
            .build()

        return suspendCancellableCoroutine { continuation ->
            waitingCaller = continuation
            continuation.invokeOnCancellation {
                waitingCaller = null
                biometricPrompt.cancelAuthentication()
            }
            biometricPrompt.authenticate(promptInfo)
        }
    }

    private fun finishWith(result: SystemUnlockResult) {
        val caller = waitingCaller ?: return
        waitingCaller = null
        caller.resume(result)
    }
}
