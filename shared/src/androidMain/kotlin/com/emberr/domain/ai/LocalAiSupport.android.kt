package com.emberr.domain.ai

import android.os.Build
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_ai_no_processor_build
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

private val BUNDLED_ANDROID_ABIS = setOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

actual fun detectLocalAiSupport(): LocalAiSupport {
    val deviceAbis = Build.SUPPORTED_ABIS?.toList().orEmpty()

    if (deviceAbis.any { it in BUNDLED_ANDROID_ABIS }) return LocalAiSupport.Supported

    val reportedAbi = deviceAbis.firstOrNull() ?: "unknown"
    return LocalAiSupport.Unsupported(
        runBlocking { getString(Res.string.message_ai_no_processor_build, reportedAbi) }
    )
}
