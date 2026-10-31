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
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    val textField = rememberTextFieldStateFor(value, onValueChange)
    val hidesText = visualTransformation is PasswordVisualTransformation
    val isSingleLine = singleLine || hidesText
    val submit = onSubmit?.let { submitAction ->
        {
            textField.reportTypedTextNow()
            submitAction()
        }
    }
    val keyboardActionHandler = submitOnDoneAction(submit, keyboardOptions, isSingleLine)
    val focusRequester = remember { FocusRequester() }
    val fieldFocus = remember { FieldFocus() }
    val wasFocusedBeforeSwap = remember(hidesText) { fieldFocus.isFocused }
    LaunchedEffect(hidesText) {
        if (wasFocusedBeforeSwap) runCatching { focusRequester.requestFocus() }
    }
    val fieldModifier = modifier
        .focusRequester(focusRequester)
        .onFocusChanged { focusState -> fieldFocus.isFocused = focusState.isFocused }
        .submitOnEnter(submit, isSingleLine)
    val shownTextStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface)

    if (!hasBackground) {
        PlainEmberrTextField(
            state = textField.state,
            modifier = fieldModifier,
            placeholder = placeholder,
            isSingleLine = isSingleLine,
            keyboardOptions = keyboardOptions,
            hidesText = hidesText,
            trailingIcon = trailingIcon,
            textStyle = textStyle,
            keyboardActionHandler = keyboardActionHandler
        )
        return
    }

    if (hidesText) {
        OutlinedSecureTextField(
            state = textField.state,
            placeholder = { EmberrTextFieldPlaceholder(placeholder, textStyle) },
            textObfuscationMode = TextObfuscationMode.Hidden,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = keyboardActionHandler,
            trailingIcon = trailingIcon,
            textStyle = shownTextStyle,
            shape = RoundedCornerShape(12.dp),
            colors = emberrTextFieldColors(),
            modifier = fieldModifier
        )
    } else {
        OutlinedTextField(
            state = textField.state,
            placeholder = { EmberrTextFieldPlaceholder(placeholder, textStyle) },
            lineLimits = if (isSingleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.Default,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = keyboardActionHandler,
            trailingIcon = trailingIcon,
            textStyle = shownTextStyle,
            shape = RoundedCornerShape(12.dp),
            colors = emberrTextFieldColors(),
            modifier = fieldModifier
        )
    }
}

private class FieldFocus {
    var isFocused = false
}

@Composable
private fun PlainEmberrTextField(
    state: TextFieldState,
    modifier: Modifier,
    placeholder: String,
    isSingleLine: Boolean,
    keyboardOptions: KeyboardOptions,
    hidesText: Boolean,
    trailingIcon: @Composable (() -> Unit)?,
    textStyle: TextStyle,
    keyboardActionHandler: KeyboardActionHandler
) {
    val shownTextStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface)
    val cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
    val decorator = TextFieldDecorator { innerTextField ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                if (state.text.isEmpty()) EmberrTextFieldPlaceholder(placeholder, textStyle)
                innerTextField()
            }
            trailingIcon?.invoke()
        }
    }

    if (hidesText) {
        BasicSecureTextField(
            state = state,
            modifier = modifier,
            textStyle = shownTextStyle,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = keyboardActionHandler,
            cursorBrush = cursorBrush,
            decorator = decorator,
            textObfuscationMode = TextObfuscationMode.Hidden
        )
    } else {
        BasicTextField(
            state = state,
            modifier = modifier,
            textStyle = shownTextStyle,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = keyboardActionHandler,
            lineLimits = if (isSingleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.Default,
            cursorBrush = cursorBrush,
            decorator = decorator
        )
    }
}

private fun submitOnDoneAction(
    onSubmit: (() -> Unit)?,
    keyboardOptions: KeyboardOptions,
    isSingleLine: Boolean
): KeyboardActionHandler = KeyboardActionHandler { performDefaultAction ->
    val imeAction = keyboardOptions.imeAction
    val isDoneAction = imeAction == ImeAction.Done || (imeAction == ImeAction.Default && isSingleLine)
    if (isDoneAction) onSubmit?.invoke() else performDefaultAction()
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
    val searchField = rememberTextFieldStateFor(value, onValueChange)
    val submit = onSubmit?.let { submitAction ->
        {
            searchField.reportTypedTextNow()
            submitAction()
        }
    }
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
                state = searchField.state,
                onKeyboardAction = submitOnDoneAction(submit, keyboardOptions, isSingleLine = true),
                keyboardOptions = keyboardOptions,
                lineLimits = TextFieldLineLimits.SingleLine,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.primary
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f).then(textFieldModifier).submitOnEnter(submit, singleLine = true),
                decorator = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (searchField.state.text.isEmpty()) {
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
