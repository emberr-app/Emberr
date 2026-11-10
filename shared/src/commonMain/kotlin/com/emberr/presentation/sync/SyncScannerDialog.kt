package com.emberr.presentation.sync

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emberr.domain.sync.SyncPairingData
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.camera
import emberr.shared.generated.resources.common_close
import emberr.shared.generated.resources.lan_sync_allow_camera
import emberr.shared.generated.resources.lan_sync_camera_access_needed
import emberr.shared.generated.resources.lan_sync_camera_allow_message
import emberr.shared.generated.resources.lan_sync_camera_turn_on_message
import emberr.shared.generated.resources.lan_sync_open_app_settings
import emberr.shared.generated.resources.lan_sync_scanner_desktop_hint
import emberr.shared.generated.resources.lan_sync_scanner_instruction
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val SCANNER_WIDTH_TO_HEIGHT = 0.8f
private val ScannerShade = Color.Black.copy(alpha = 0.55f)

@Composable
fun SyncScannerDialog(
    onDismiss: () -> Unit,
    onScanned: (SyncPairingData) -> Unit
) {
    val lenientJson = remember { Json { ignoreUnknownKeys = true } }
    val cameraPermission = rememberCameraPermissionState()

    EmberrBottomSheet(
        expanded = true,
        onDismiss = onDismiss,
        contentHorizontalPadding = 0.dp,
        scrollsContent = false,
        floats = true
    ) { closeAnd ->
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(SCANNER_WIDTH_TO_HEIGHT)) {
            if (cameraPermission.isGranted) {
                QrScannerView(
                    onQrScanned = { resultString ->
                        try {
                            val parsedData = lenientJson.decodeFromString<SyncPairingData>(resultString)
                            onScanned(parsedData)
                        } catch (e: Exception) {
                            println("Invalid QR Code ignored: $resultString")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                Text(
                    text = stringResource(Res.string.lan_sync_scanner_instruction),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(ScannerShade, Color.Transparent)))
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 40.dp)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, ScannerShade)))
                        .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.lan_sync_scanner_desktop_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    EmberrButtonPrimary(
                        text = stringResource(Res.string.common_close),
                        onClick = { closeAnd {} },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                CameraPermissionMissing(
                    canShowPermissionPopup = cameraPermission.canShowPermissionPopup,
                    onAllowCamera = cameraPermission::request,
                    onClose = { closeAnd {} }
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionMissing(
    canShowPermissionPopup: Boolean,
    onAllowCamera: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(Res.drawable.camera),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.lan_sync_camera_access_needed),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (canShowPermissionPopup) {
                    stringResource(Res.string.lan_sync_camera_allow_message)
                } else {
                    stringResource(Res.string.lan_sync_camera_turn_on_message)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            EmberrButtonPrimary(
                text = if (canShowPermissionPopup) {
                    stringResource(Res.string.lan_sync_allow_camera)
                } else {
                    stringResource(Res.string.lan_sync_open_app_settings)
                },
                onClick = onAllowCamera,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            EmberrButtonPrimary(
                text = stringResource(Res.string.common_close),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
