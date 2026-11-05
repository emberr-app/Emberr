package com.emberr.presentation.sync

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.util.concurrent.Executors

actual @Composable fun QrCodeDisplay(data: String, size: Int, modifier: Modifier) {
    Box(modifier.size(size.dp))
}

@Composable
actual fun rememberCameraPermissionState(): CameraPermissionState {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isGrantedState = remember { mutableStateOf(context.hasCameraPermission()) }
    val canShowPermissionPopupState = remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            isGrantedState.value = granted
            canShowPermissionPopupState.value = activity != null &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
        }
    )

    val appSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { isGrantedState.value = context.hasCameraPermission() }
    )

    LaunchedEffect(Unit) {
        if (!isGrantedState.value) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(lifecycleOwner) {
        val resumeObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) isGrantedState.value = context.hasCameraPermission()
        }
        lifecycleOwner.lifecycle.addObserver(resumeObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(resumeObserver) }
    }

    return remember(permissionLauncher, appSettingsLauncher) {
        object : CameraPermissionState {
            override val isGranted: Boolean get() = isGrantedState.value
            override val canShowPermissionPopup: Boolean get() = canShowPermissionPopupState.value

            override fun request() {
                if (canShowPermissionPopupState.value) {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                    return
                }
                val appSettingsIntent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                runCatching { appSettingsLauncher.launch(appSettingsIntent) }
                    .onFailure { Log.e("QrScanner", "Could not open app settings", it) }
            }
        }
    }
}

actual @Composable fun QrScannerView(
    onQrScanned: (String) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val frameDecoder = QrCodeFrameDecoder()
                    val mainThreadExecutor = ContextCompat.getMainExecutor(ctx)

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        try {
                            val luminancePlane = imageProxy.planes[0]
                            val rowStride = luminancePlane.rowStride
                            val luminanceBytes = ByteArray(rowStride * imageProxy.height)
                            luminancePlane.buffer.get(luminanceBytes, 0, luminancePlane.buffer.remaining())
                            frameDecoder.decode(luminanceBytes, rowStride, imageProxy.width, imageProxy.height)
                                ?.let { scannedData ->
                                    mainThreadExecutor.execute { onQrScanned(scannedData) }
                                }
                        } catch (frameFailure: Exception) {
                            Log.e("QrScanner", "Could not read a camera frame", frameFailure)
                        } finally {
                            imageProxy.close()
                        }
                    }

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (startFailure: Exception) {
                    Log.e("QrScanner", "Could not start the camera scanner", startFailure)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        onRelease = { _ ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    cameraProviderFuture.get().unbindAll()
                } catch (releaseFailure: Exception) {
                    Log.e("QrScanner", "Could not release the camera", releaseFailure)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

private fun android.content.Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED