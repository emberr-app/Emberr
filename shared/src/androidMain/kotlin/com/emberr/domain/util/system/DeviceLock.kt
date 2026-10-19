package com.emberr.domain.util.system

import android.Manifest
import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager.Authenticators
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
actual fun rememberDeviceLockPrompt(): DeviceLockPrompt {
    val context = LocalContext.current

    return remember(context) {
        object : DeviceLockPrompt {
            override suspend fun confirm(title: String, description: String): DeviceLockConfirmation {
                val keyguardManager = context.getSystemService(KeyguardManager::class.java)
                if (keyguardManager?.isDeviceSecure != true) return DeviceLockConfirmation.NO_DEVICE_LOCK
                val mayUseDeviceLock =
                    context.checkSelfPermission(Manifest.permission.USE_BIOMETRIC) == PackageManager.PERMISSION_GRANTED
                if (!mayUseDeviceLock) return DeviceLockConfirmation.NO_DEVICE_LOCK
                val wasConfirmed = context.showDeviceLockPrompt(title, description)
                return if (wasConfirmed) DeviceLockConfirmation.CONFIRMED else DeviceLockConfirmation.NOT_CONFIRMED
            }
        }
    }
}

@SuppressLint("MissingPermission")
private suspend fun Context.showDeviceLockPrompt(title: String, description: String): Boolean =
    suspendCancellableCoroutine { continuation ->
        val cancellationSignal = CancellationSignal()
        continuation.invokeOnCancellation { cancellationSignal.cancel() }

        val prompt = BiometricPrompt.Builder(this)
            .setTitle(title)
            .setDescription(description)
            .setAllowedAuthenticators(Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(
            cancellationSignal,
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence) {
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        )
    }
