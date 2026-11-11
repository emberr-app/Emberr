package com.emberr.domain.util.voice

class DesktopVoiceRecognizer : VoiceRecognizer {

    override fun startListening(
        onPartial: (String) -> Unit,
        onSegment: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (VoiceRecognitionError) -> Unit,
        onPermissionNeeded: () -> Unit
    ) {
        // Desktop platforms don't have a native built-in speech recognizer API
        // like Android does. We immediately return an error so the UI handles it gracefully.
        onError(VoiceRecognitionError.NOT_SUPPORTED_ON_DESKTOP)
    }

    override fun stopListening() {
        // No-op
    }

    override fun destroy() {
        // No-op
    }
}