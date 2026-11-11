package com.emberr.domain.util.voice

enum class VoiceRecognitionError {
    NO_SPEECH_HEARD,
    UNCLEAR_AUDIO,
    MICROPHONE_PERMISSION_NEEDED,
    NETWORK,
    SERVICE_BUSY,
    SERVICE_DISCONNECTED,
    UNAVAILABLE,
    NOT_SUPPORTED_ON_DESKTOP,
    COULD_NOT_START,
    UNKNOWN
}

/**
 * Multiplatform contract for voice-to-text recognition.
 */
interface VoiceRecognizer {
    fun startListening(
        onPartial: (String) -> Unit,
        onSegment: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (VoiceRecognitionError) -> Unit,
        onPermissionNeeded: () -> Unit
    )
    fun stopListening()
    fun destroy()
}