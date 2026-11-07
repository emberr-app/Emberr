package com.emberr.presentation.shared.editor.blockViews.credentials

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emberr.domain.credentials.LONGEST_GENERATED_PASSWORD
import com.emberr.domain.credentials.MOST_REQUIRED_CHARACTERS_OF_ONE_KIND
import com.emberr.domain.credentials.PasswordGeneratorOptions
import com.emberr.domain.credentials.PasswordGeneratorOptionsStore
import com.emberr.domain.credentials.SHORTEST_GENERATED_PASSWORD
import com.emberr.domain.credentials.generatePassword
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.EmberrBottomSheet
import com.emberr.presentation.shared.components.EmberrButtonPrimary
import com.emberr.presentation.shared.components.EmberrButtonSecondary
import com.emberr.presentation.shared.components.EmberrDesktopMenu
import com.emberr.presentation.shared.components.EmberrSwitch
import com.emberr.presentation.shared.components.LocalSitsOnCard
import com.emberr.presentation.shared.components.SmallRoundedCornerShape
import com.emberr.presentation.shared.components.emberrControlContainerColor
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.minus
import emberr.shared.generated.resources.plus
import emberr.shared.generated.resources.refresh_cw
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlin.math.roundToInt

private val DesktopMenuWidth = 300.dp
private val DesktopContentPadding = 14.dp
private val SheetContentPadding = 20.dp
private val ChipHeight = 40.dp
private val SectionGap = 28.dp
private val ButtonTopGap = 12.dp
private val LabelGap = 12.dp
private val RowVerticalPadding = 10.dp

@Composable
internal fun PasswordGeneratorMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onUsePassword: (String) -> Unit
) {
    val optionsStore = koinInject<PasswordGeneratorOptionsStore>()
    var options by remember(expanded) {
        mutableStateOf(if (expanded) optionsStore.load() else PasswordGeneratorOptions())
    }
    var password by remember(expanded) { mutableStateOf(if (expanded) generatePassword(options) else "") }

    fun changeOptions(newOptions: PasswordGeneratorOptions, savesOptions: Boolean) {
        options = newOptions
        password = generatePassword(newOptions)
        if (savesOptions) optionsStore.save(newOptions)
    }

    val content: @Composable (onUse: () -> Unit, onCancel: (() -> Unit)?) -> Unit = { onUse, onCancel ->
        PasswordGeneratorContent(
            options = options,
            password = password,
            onOptionsChange = { newOptions, savesOptions -> changeOptions(newOptions, savesOptions) },
            onLengthChangeFinished = { optionsStore.save(options) },
            onRegenerate = { password = generatePassword(options) },
            onUse = onUse,
            onCancel = onCancel
        )
    }

    if (isDesktopPlatform) {
        EmberrDesktopMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.width(DesktopMenuWidth)
        ) {
            content(
                {
                    onDismiss()
                    onUsePassword(password)
                },
                null
            )
        }
    } else {
        EmberrBottomSheet(
            expanded = expanded,
            onDismiss = onDismiss,
            title = "Generate password",
            contentHorizontalPadding = 0.dp
        ) { closeAnd ->
            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = SheetBottomPadding)) {
                content(
                    {
                        val chosenPassword = password
                        closeAnd { onUsePassword(chosenPassword) }
                    },
                    { closeAnd {} }
                )
            }
        }
    }
}

@Composable
private fun PasswordGeneratorContent(
    options: PasswordGeneratorOptions,
    password: String,
    onOptionsChange: (PasswordGeneratorOptions, savesOptions: Boolean) -> Unit,
    onLengthChangeFinished: () -> Unit,
    onRegenerate: () -> Unit,
    onUse: () -> Unit,
    onCancel: (() -> Unit)?
) {
    val contentPadding = if (isDesktopPlatform) DesktopContentPadding else SheetContentPadding

    fun toggleSet(isOn: Boolean, change: (Boolean) -> PasswordGeneratorOptions) {
        if (isOn && options.enabledCharacterSetCount == 1) return
        onOptionsChange(change(!isOn), true)
    }

    CompositionLocalProvider(LocalSitsOnCard provides false) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(
                start = contentPadding,
                end = contentPadding,
                top = 12.dp,
                bottom = if (isDesktopPlatform) 8.dp else 0.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SmallRoundedCornerShape)
                    .background(emberrControlContainerColor())
                    .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = password,
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp)
                )
                CredentialIconButton(
                    painter = painterResource(Res.drawable.refresh_cw),
                    description = "Generate another",
                    onClick = onRegenerate
                )
            }

            Spacer(modifier = Modifier.height(SectionGap))
            Row(verticalAlignment = Alignment.CenterVertically) {
                GeneratorSectionLabel(text = "Length", modifier = Modifier.weight(1f))
                Text(
                    text = options.shownLength.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = options.shownLength.toFloat(),
                onValueChange = { newLength ->
                    val length = newLength.roundToInt().coerceAtLeast(options.shortestAllowedLength)
                    if (length != options.shownLength) onOptionsChange(options.copy(length = length), false)
                },
                onValueChangeFinished = onLengthChangeFinished,
                valueRange = SHORTEST_GENERATED_PASSWORD.toFloat()..LONGEST_GENERATED_PASSWORD.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = emberrControlContainerColor()
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(SectionGap))
            GeneratorSectionLabel(text = "Characters")
            Spacer(modifier = Modifier.height(LabelGap))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CharacterSetChip(label = "A-Z", isOn = options.usesUppercase, modifier = Modifier.weight(1f)) {
                    toggleSet(options.usesUppercase) { isOn -> options.copy(usesUppercase = isOn) }
                }
                CharacterSetChip(label = "a-z", isOn = options.usesLowercase, modifier = Modifier.weight(1f)) {
                    toggleSet(options.usesLowercase) { isOn -> options.copy(usesLowercase = isOn) }
                }
                CharacterSetChip(label = "0-9", isOn = options.usesNumbers, modifier = Modifier.weight(1f)) {
                    toggleSet(options.usesNumbers) { isOn -> options.copy(usesNumbers = isOn) }
                }
                CharacterSetChip(label = "!@#", isOn = options.usesSymbols, modifier = Modifier.weight(1f)) {
                    toggleSet(options.usesSymbols) { isOn -> options.copy(usesSymbols = isOn) }
                }
            }

            Spacer(modifier = Modifier.height(SectionGap))
            GeneratorSectionLabel(text = "Options")
            Spacer(modifier = Modifier.height(LabelGap - RowVerticalPadding))
            if (options.usesNumbers) {
                GeneratorCountRow(
                    label = "Minimum numbers",
                    count = options.minimumNumbers,
                    onCountChange = { count -> onOptionsChange(options.copy(minimumNumbers = count), true) }
                )
            }
            if (options.usesSymbols) {
                GeneratorCountRow(
                    label = "Minimum symbols",
                    count = options.minimumSymbols,
                    onCountChange = { count -> onOptionsChange(options.copy(minimumSymbols = count), true) }
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SmallRoundedCornerShape)
                    .clickable {
                        onOptionsChange(options.copy(avoidsAmbiguousCharacters = !options.avoidsAmbiguousCharacters), true)
                    }
                    .padding(vertical = RowVerticalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Avoid look-alike characters",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                EmberrSwitch(isOn = options.avoidsAmbiguousCharacters)
            }

            Spacer(modifier = Modifier.height(ButtonTopGap))
            if (onCancel == null) {
                EmberrButtonPrimary(text = "Use password", onClick = onUse, modifier = Modifier.fillMaxWidth())
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EmberrButtonSecondary(text = "Cancel", onClick = onCancel, modifier = Modifier.weight(1f))
                    EmberrButtonPrimary(text = "Use password", onClick = onUse, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GeneratorSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier
    )
}

@Composable
private fun CharacterSetChip(label: String, isOn: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(ChipHeight)
            .clip(SmallRoundedCornerShape)
            .background(if (isOn) MaterialTheme.colorScheme.primary else emberrControlContainerColor())
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun GeneratorCountRow(label: String, count: Int, onCountChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Row(
            modifier = Modifier.clip(SmallRoundedCornerShape).background(emberrControlContainerColor()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CredentialIconButton(
                painter = painterResource(Res.drawable.minus),
                description = "Fewer",
                onClick = { if (count > 0) onCountChange(count - 1) }
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 20.dp)
            )
            CredentialIconButton(
                painter = painterResource(Res.drawable.plus),
                description = "More",
                onClick = { if (count < MOST_REQUIRED_CHARACTERS_OF_ONE_KIND) onCountChange(count + 1) }
            )
        }
    }
}
