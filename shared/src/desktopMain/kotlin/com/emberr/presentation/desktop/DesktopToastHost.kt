package com.emberr.presentation.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun DesktopToastHost() {
    val toast by DesktopToastBus.currentToast.collectAsState()
    var isShowing by remember { mutableStateOf(false) }

    LaunchedEffect(toast) {
        if (toast == null) return@LaunchedEffect
        isShowing = true
        delay(2.seconds)
        isShowing = false
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = isShowing,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Text(
                text = toast?.message.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}
