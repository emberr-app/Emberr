package com.emberr.presentation.home.note

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emberr.domain.util.media.webImageAddressOrNull
import com.emberr.presentation.shared.editor.ImageLinkFetch
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.link
import org.jetbrains.compose.resources.painterResource

private val CoverLinkFieldShape = RoundedCornerShape(12.dp)

@Composable
internal fun CoverLinkField(
    coverLinkFetch: ImageLinkFetch?,
    onCoverLinkSubmitted: (address: String) -> Unit,
    onCoverLinkApplied: () -> Unit,
    modifier: Modifier = Modifier
) {
    var linkText by remember { mutableStateOf(coverLinkFetch?.address.orEmpty()) }
    var hasEditedLinkSinceLastTry by remember { mutableStateOf(false) }
    var hasSeenFetchRunning by remember { mutableStateOf(false) }
    val isFetching = coverLinkFetch?.hasFailed == false
    val hasFetchFailed = coverLinkFetch?.hasFailed == true && !hasEditedLinkSinceLastTry
    val hintColor = MaterialTheme.colorScheme.outline

    LaunchedEffect(coverLinkFetch) {
        if (isFetching) hasSeenFetchRunning = true
        else if (coverLinkFetch == null && hasSeenFetchRunning) onCoverLinkApplied()
    }

    fun fetchCover(address: String) {
        hasEditedLinkSinceLastTry = false
        onCoverLinkSubmitted(address)
    }

    fun fetchTypedLink() {
        webImageAddressOrNull(linkText)?.let { address -> fetchCover(address) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(CoverLinkFieldShape)
                .background(hintColor.copy(alpha = 0.1f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(painterResource(Res.drawable.link), contentDescription = null, tint = hintColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (linkText.isEmpty()) {
                    Text(
                        text = "Paste an image link",
                        style = MaterialTheme.typography.bodyLarge,
                        color = hintColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = linkText,
                    onValueChange = { newText ->
                        val wasPasted = newText.length - linkText.length > 1
                        linkText = newText
                        hasEditedLinkSinceLastTry = true
                        val address = webImageAddressOrNull(newText)
                        if (wasPasted && address != null) fetchCover(address)
                    },
                    enabled = !isFetching,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { fetchTypedLink() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPreviewKeyEvent { event ->
                            val isEnterPress = event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            if (isEnterPress) fetchTypedLink()
                            isEnterPress
                        }
                )
            }
            if (isFetching) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onSurface,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (hasFetchFailed) {
            Text(
                text = "Couldn't load an image from this link",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp)
            )
        }
    }
}
