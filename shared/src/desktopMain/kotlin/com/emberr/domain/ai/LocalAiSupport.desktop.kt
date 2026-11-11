package com.emberr.domain.ai

import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.message_ai_engine_not_bundled
import emberr.shared.generated.resources.message_ai_needs_64_bit_processor
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

private val BUNDLED_PROCESSOR_NAMES = setOf("amd64", "x86_64", "x86-64")

private fun nativeEngineDirectoryFor(operatingSystemName: String): String? = when {
    operatingSystemName.contains("linux") -> "linux"
    operatingSystemName.contains("win") -> "windows"
    operatingSystemName.contains("mac") -> "macos"
    else -> null
}

private fun isNativeEngineBundledIn(directoryName: String): Boolean =
    LocalAiSupport::class.java.classLoader
        .getResource("native/$directoryName/native-libs.txt") != null

actual fun detectLocalAiSupport(): LocalAiSupport {
    val operatingSystemName = System.getProperty("os.name").orEmpty()
    val processorName = System.getProperty("os.arch").orEmpty().lowercase()
    val nativeEngineDirectory = nativeEngineDirectoryFor(operatingSystemName.lowercase())

    return when {
        nativeEngineDirectory == null || !isNativeEngineBundledIn(nativeEngineDirectory) ->
            LocalAiSupport.Unsupported(
                runBlocking { getString(Res.string.message_ai_engine_not_bundled, operatingSystemName) }
            )

        processorName !in BUNDLED_PROCESSOR_NAMES -> LocalAiSupport.Unsupported(
            runBlocking { getString(Res.string.message_ai_needs_64_bit_processor, processorName) }
        )

        else -> LocalAiSupport.Supported
    }
}
