package com.emberr.domain.util.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import java.util.Locale

private const val SEGMENTED_SESSION_SILENCE_MILLIS = 10_000L

class NativeVoiceRecognizer(private val context: Context) : VoiceRecognizer {
    private var speechRecognizer: SpeechRecognizer? = null

    private var onPartialCallback: ((String) -> Unit)? = null
    private var onSegmentCallback: ((String) -> Unit)? = null
    private var onResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((VoiceRecognitionError) -> Unit)? = null

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            val recognitionError = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceRecognitionError.NO_SPEECH_HEARD
                SpeechRecognizer.ERROR_AUDIO -> VoiceRecognitionError.UNCLEAR_AUDIO
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceRecognitionError.MICROPHONE_PERMISSION_NEEDED
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceRecognitionError.NETWORK
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    speechRecognizer?.cancel()
                    VoiceRecognitionError.SERVICE_BUSY
                }
                SpeechRecognizer.ERROR_CLIENT,
                SpeechRecognizer.ERROR_SERVER -> VoiceRecognitionError.SERVICE_DISCONNECTED
                else -> VoiceRecognitionError.UNKNOWN
            }
            onErrorCallback?.invoke(recognitionError)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onResultCallback?.invoke(removeFillerWords(matches[0]))
            } else {
                onErrorCallback?.invoke(VoiceRecognitionError.NO_SPEECH_HEARD)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onPartialCallback?.invoke(removeFillerWords(matches[0]))
            }
        }

        override fun onSegmentResults(segmentResults: Bundle) {
            val matches = segmentResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                onSegmentCallback?.invoke(removeFillerWords(matches[0]))
            }
        }

        override fun onEndOfSegmentedSession() {
            onResultCallback?.invoke("")
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun initRecognizerIfNeeded() {
        if (speechRecognizer != null) return

        val isStrictOfflineAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

        speechRecognizer = if (isStrictOfflineAvailable) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    private fun removeFillerWords(text: String): String {
        val regex = Regex("(?i)\\b(um|uh|ahs?|ahh|oh|hmm)\\b[\\s,]*")
        return text.replace(regex, " ").replace(Regex("\\s+"), " ").trim()
    }

    override fun startListening(
        onPartial: (String) -> Unit,
        onSegment: (String) -> Unit,
        onResult: (String) -> Unit,
        onError: (VoiceRecognitionError) -> Unit,
        onPermissionNeeded: () -> Unit
    ) {
        Handler(Looper.getMainLooper()).post {
            val permissionCheck = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            )

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                onPermissionNeeded()
                return@post
            }

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError(VoiceRecognitionError.UNAVAILABLE)
                return@post
            }

            onPartialCallback = onPartial
            onSegmentCallback = onSegment
            onResultCallback = onResult
            onErrorCallback = onError

            initRecognizerIfNeeded()

            try {
                speechRecognizer?.setRecognitionListener(null)
                speechRecognizer?.cancel()
                speechRecognizer?.setRecognitionListener(recognitionListener)
            } catch (e: Exception) {}

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    putExtra(
                        RecognizerIntent.EXTRA_SEGMENTED_SESSION,
                        RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS
                    )
                    putExtra(
                        RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                        SEGMENTED_SESSION_SILENCE_MILLIS
                    )
                }
            }

            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                onError(VoiceRecognitionError.COULD_NOT_START)
            }
        }
    }

    override fun stopListening() {
        Handler(Looper.getMainLooper()).post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {}
        }
    }

    override fun destroy() {
        Handler(Looper.getMainLooper()).post {
            try {
                speechRecognizer?.setRecognitionListener(null)
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {}

            speechRecognizer = null
            onPartialCallback = null
            onSegmentCallback = null
            onResultCallback = null
            onErrorCallback = null
        }
    }
}