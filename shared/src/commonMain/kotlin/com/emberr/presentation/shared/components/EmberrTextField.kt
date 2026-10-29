package com.emberr.presentation.shared.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.search
import emberr.shared.generated.resources.x
import org.jetbrains.compose.resources.painterResource

@Composable
fun EmberrTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    hasBackground: Boolean = true,
    onSubmit: (() -> Unit)? = null
) {
    if (!hasBackground) {
        PlainEmberrTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            placeholder = placeholder,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            textStyle = textStyle,
            onSubmit = onSubmit
        )
        return
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { EmberrTextFieldPlaceholder(placeholder, textStyle) },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        textStyle = textStyle.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        colors = emberrTextFieldColors(),
        modifier = modifier.submitOnEnter(onSubmit, singleLine)
    )
}

@Composable
private fun PlainEmberrTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    placeholder: String,
    singleLine: Boolean,
    keyboardOptions: KeyboardOptions,
    visualTransformation: VisualTransformation,
    trailingIcon: @Composable (() -> Unit)?,
    textStyle: TextStyle,
    onSubmit: (() -> Unit)?
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
        visualTransformation = visualTransformation,
        textStyle = textStyle.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier.submitOnEnter(onSubmit, singleLine),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) EmberrTextFieldPlaceholder(placeholder, textStyle)
                    innerTextField()
                }
                trailingIcon?.invoke()
            }
        }
    )
}

private fun Modifier.submitOnEnter(onSubmit: (() -> Unit)?, singleLine: Boolean): Modifier =
    onPreviewKeyEvent { event ->
        val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
        if (onSubmit != null && singleLine && isEnter && event.type == KeyEventType.KeyDown) {
            onSubmit()
            true
        } else {
            false
        }
    }

@Composable
fun EmberrTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { EmberrTextFieldPlaceholder(placeholder) },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(12.dp),
        colors = emberrTextFieldColors(),
        modifier = modifier
    )
}

@Composable
fun EmberrSearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    hazeState: HazeState?,
    modifier: Modifier = Modifier,
    textFieldModifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onSubmit: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .emberrBorder(CircleShape)
            .customEmberrShadow(CircleShape)
            .clip(CircleShape)
            .emberrBlur(hazeState, EmberrBlur.Regular)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = KeyboardActions(onDone = { onSubmit?.invoke() }),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).then(textFieldModifier).submitOnEnter(onSubmit, singleLine = true),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            actions()
            if (value.isNotEmpty()) {
                EmberrSearchTextFieldAction(
                    icon = painterResource(Res.drawable.x),
                    contentDescription = "Clear search",
                    onClick = { onValueChange("") }
                )
            }
        }
    }
}

@Composable
fun EmberrSearchTextFieldAction(
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconTooltip(text = contentDescription) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun EmberrTextFieldPlaceholder(placeholder: String, textStyle: TextStyle = MaterialTheme.typography.bodyLarge) {
    Text(
        text = placeholder,
        style = textStyle,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    )
}

@Composable
private fun emberrTextFieldColors(): TextFieldColors {
    val containerColor = emberrControlContainerColor()
    return OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = Color.Transparent,
        focusedBorderColor = Color.Transparent,
        unfocusedContainerColor = containerColor,
        focusedContainerColor = containerColor,
        cursorColor = MaterialTheme.colorScheme.primary
    )
}
