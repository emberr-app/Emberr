package com.emberr.domain.ai

sealed interface LocalAiSupport {
    data object Supported : LocalAiSupport
    data class Unsupported(val reason: LocalAiUnsupportedReason) : LocalAiSupport
}

sealed interface LocalAiUnsupportedReason {
    data class NoBuildForProcessor(val processorName: String) : LocalAiUnsupportedReason
    data class EngineNotBundledForSystem(val operatingSystemName: String) : LocalAiUnsupportedReason
    data class Needs64BitProcessor(val processorName: String) : LocalAiUnsupportedReason
}

class LocalAiUnsupportedException(val reason: LocalAiUnsupportedReason) :
    IllegalStateException("Local AI cannot run on this device: $reason")

expect fun detectLocalAiSupport(): LocalAiSupport
