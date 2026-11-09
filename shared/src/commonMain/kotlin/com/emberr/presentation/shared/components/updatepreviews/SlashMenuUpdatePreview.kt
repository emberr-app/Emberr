package com.emberr.presentation.shared.components.updatepreviews

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.emberr.domain.util.system.isDesktopPlatform
import com.emberr.presentation.shared.components.customEmberrShadow
import com.emberr.presentation.shared.components.emberrBorder
import com.emberr.presentation.shared.components.emberrBorderWidth
import com.emberr.presentation.shared.components.emberrSoftBorderColor
import com.emberr.presentation.shared.editor.slash.SlashMenuList
import com.emberr.presentation.shared.editor.slash.SlashMenuSectionData
import com.emberr.presentation.shared.editor.slash.buildSlashMenuSections
import com.emberr.presentation.shared.editor.slash.filteredSlashMenuSections
import com.emberr.presentation.shared.editor.toolbar.DefaultKeyboardToolbarItems
import com.emberr.presentation.shared.editor.toolbar.KeyboardToolbarItem
import com.emberr.presentation.shared.editor.toolbar.KeyboardToolbarItemIcon
import com.emberr.presentation.shared.editor.toolbar.MenuDragHandle
import com.emberr.presentation.shared.editor.toolbar.ToolbarButton
import com.emberr.presentation.shared.editor.toolbar.ToolbarDivider
import com.emberr.ui.theme.LocalAppIsDark
import com.emberr.ui.theme.LocalUsesTwentyFourHourClock
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.keyboard
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

private val DesktopSlashPopupShape = RoundedCornerShape(18.dp)
private val DesktopSlashPopupWidth = 290.dp
private val DesktopSlashPopupMaxHeight = 190.dp
private val MobileToolbarShape = RoundedCornerShape(12.dp)
private val MobileSlashListMaxHeight = 150.dp
private val SlashPopupGrowsFromTopStart = TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0f)
private val KeyboardKeyShape = RoundedCornerShape(6.dp)
private val LightKeyboardColors = Color(0xFFE4E4E7) to Color(0xFFFFFFFF)
private val DarkKeyboardColors = Color(0xFF1B1B1D) to Color(0xFF3A3A3D)
private val KeyboardRowKeyCounts = listOf(10, 9, 7)
private val KeyboardHeight = 124.dp
private const val TYPING_DELAY_MILLIS = 180L
private const val SHOWN_TOOLBAR_ITEM_COUNT = 12

private fun slashQueryCanStillMatch(query: String, slashMenuSearchTexts: List<String>): Boolean =
    !query.contains(" ") && (query.length <= 2 || slashMenuSearchTexts.any { it.contains(query, ignoreCase = true) })

@Composable
internal fun SlashMenuUpdatePreview() {
    val usesTwentyFourHourClock = LocalUsesTwentyFourHourClock.current
    val slashMenuSearchTexts = remember {
        buildSlashMenuSections(
            onChangeBlockType = {},
            onToggleFormat = {},
            onAdjustIndentation = {},
            onSetAlignment = {},
            onInsertMediaBlock = {},
            onInsertText = {},
            usesTwentyFourHourClock = false
        ).flatMap { section -> section.items.flatMap { item -> listOf(item.label) + item.searchWords } }
    }
    var typedText by remember { mutableStateOf("") }
    var isSlashKilled by remember { mutableStateOf(false) }
    var shownMenuQuery by remember { mutableStateOf("") }
    var isHeading by remember { mutableStateOf(false) }
    var isFirstItemPicked by remember { mutableStateOf(false) }
    var pressedKey by remember { mutableStateOf("Backspace") }
    var isKeyShown by remember { mutableStateOf(false) }

    val slashIndex = typedText.lastIndexOf('/')
    val isMenuOpen = slashIndex != -1 && !isSlashKilled

    LaunchedEffect(Unit) {
        fun showTypedText(newText: String) {
            val newSlashIndex = newText.lastIndexOf('/')
            if (newSlashIndex == -1) {
                isSlashKilled = false
            } else {
                val query = newText.substring(newSlashIndex + 1)
                if (!slashQueryCanStillMatch(query, slashMenuSearchTexts)) {
                    isSlashKilled = true
                } else if (newText.length < typedText.length) {
                    isSlashKilled = false
                }
                if (!isSlashKilled) shownMenuQuery = query
            }
            typedText = newText
        }

        suspend fun typeSlowly(addedText: String) {
            addedText.forEach { character ->
                showTypedText(typedText + character)
                delay(TYPING_DELAY_MILLIS)
            }
        }

        suspend fun pressBackspace() {
            pressedKey = "Backspace"
            isKeyShown = true
            showTypedText(typedText.dropLast(1))
            delay(200)
            isKeyShown = false
            delay(450)
        }

        while (true) {
            isHeading = false
            isSlashKilled = false
            typedText = ""
            delay(900)
            typeSlowly("Agenda")
            delay(400)
            typeSlowly("/")
            delay(1100)
            typeSlowly("he")
            delay(1300)
            typeSlowly("zz")
            delay(1300)
            pressBackspace()
            pressBackspace()
            delay(1200)
            if (isDesktopPlatform) {
                pressedKey = "Enter"
                isKeyShown = true
                delay(350)
                isKeyShown = false
            } else {
                isFirstItemPicked = true
                delay(450)
                isFirstItemPicked = false
            }
            typedText = "Agenda"
            isHeading = true
            delay(2400)
        }
    }

    val menuSections = remember(shownMenuQuery, usesTwentyFourHourClock) {
        filteredSlashMenuSections(
            query = shownMenuQuery,
            onChangeBlockType = {},
            onToggleFormat = {},
            onAdjustIndentation = {},
            onSetAlignment = {},
            onInsertMediaBlock = {},
            onInsertText = {},
            usesTwentyFourHourClock = usesTwentyFourHourClock
        )
    }

    UpdatePreviewFrame {
        SlashPreviewNote(
            textBeforeSlash = if (slashIndex == -1) typedText else typedText.substring(0, slashIndex),
            textFromSlash = if (slashIndex == -1) "" else typedText.substring(slashIndex),
            isHeading = isHeading,
            isMenuOpen = isMenuOpen,
            menuSections = menuSections
        )
        if (isDesktopPlatform) {
            UpdatePreviewKeyCaps(
                keys = listOf(pressedKey),
                isShown = isKeyShown,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 14.dp)
            )
        } else {
            Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                MobileToolbarCopy(
                    isMenuOpen = isMenuOpen,
                    menuSections = menuSections,
                    selectedIndex = if (isFirstItemPicked) 0 else -1,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                )
                PreviewKeyboard()
            }
        }
    }
}

@Composable
private fun SlashPreviewNote(
    textBeforeSlash: String,
    textFromSlash: String,
    isHeading: Boolean,
    isMenuOpen: Boolean,
    menuSections: List<SlashMenuSectionData>
) {
    var slashStartPx by remember { mutableIntStateOf(0) }
    var typedLineHeightPx by remember { mutableIntStateOf(0) }
    val typedTextStyle = if (isHeading) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge
    val typedTextWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (isDesktopPlatform) 20.dp else 34.dp)
    ) {
        UpdatePreviewNoteTopButtons(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(18.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text(
                text = "Team meeting",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Thursday at 10, room 2.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.onSizeChanged { size -> typedLineHeightPx = size.height }
                ) {
                    Text(
                        text = textBeforeSlash,
                        style = typedTextStyle,
                        fontWeight = typedTextWeight,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.onSizeChanged { size -> slashStartPx = size.width }
                    )
                    Text(
                        text = textFromSlash,
                        style = typedTextStyle,
                        fontWeight = typedTextWeight,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    BlinkingCaret(height = if (isHeading) 28.dp else 22.dp)
                }
                if (isDesktopPlatform) {
                    DesktopSlashPopupCopy(
                        isMenuOpen = isMenuOpen,
                        menuSections = menuSections,
                        modifier = Modifier.layout { measurable, constraints ->
                            val popup = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity))
                            layout(0, 0) { popup.place(slashStartPx, typedLineHeightPx + 4.dp.roundToPx()) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BlinkingCaret(height: Dp) {
    var isCaretShown by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            isCaretShown = !isCaretShown
        }
    }

    Box(
        modifier = Modifier
            .padding(start = 1.dp)
            .width(1.5.dp)
            .height(height)
            .alpha(if (isCaretShown) 1f else 0f)
            .background(MaterialTheme.colorScheme.onBackground)
    )
}

@Composable
private fun DesktopSlashPopupCopy(
    isMenuOpen: Boolean,
    menuSections: List<SlashMenuSectionData>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isMenuOpen,
        enter = fadeIn(tween(120)) + scaleIn(tween(120), initialScale = 0.96f, transformOrigin = SlashPopupGrowsFromTopStart),
        exit = fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.96f, transformOrigin = SlashPopupGrowsFromTopStart),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .width(DesktopSlashPopupWidth)
                .customEmberrShadow(DesktopSlashPopupShape)
                .clip(DesktopSlashPopupShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(emberrBorderWidth, emberrSoftBorderColor, DesktopSlashPopupShape)
                .padding(horizontal = 6.dp)
        ) {
            SlashMenuList(sections = menuSections, selectedIndex = 0, maxHeight = DesktopSlashPopupMaxHeight)
        }
    }
}

@Composable
private fun MobileToolbarCopy(
    isMenuOpen: Boolean,
    menuSections: List<SlashMenuSectionData>,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    val tint = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxWidth()
            .emberrBorder(MobileToolbarShape)
            .customEmberrShadow(MobileToolbarShape)
            .clip(MobileToolbarShape)
            .background(MaterialTheme.colorScheme.surface)
            .animateContentSize()
    ) {
        if (isMenuOpen) {
            Box(modifier = Modifier.fillMaxWidth()) {
                MenuDragHandle(onClose = {}, modifier = Modifier.align(Alignment.TopCenter).zIndex(1f))
                SlashMenuList(sections = menuSections, selectedIndex = selectedIndex, maxHeight = MobileSlashListMaxHeight)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    DefaultKeyboardToolbarItems.take(SHOWN_TOOLBAR_ITEM_COUNT).forEach { item ->
                        if (item == KeyboardToolbarItem.DIVIDER) {
                            ToolbarDivider(tint)
                        } else {
                            ToolbarButton(onClick = {}) { KeyboardToolbarItemIcon(item, tint) }
                        }
                    }
                }
                ToolbarDivider(tint)
                ToolbarButton(onClick = {}) {
                    Icon(painterResource(Res.drawable.keyboard), contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PreviewKeyboard() {
    val (keyboardColor, keyColor) = if (LocalAppIsDark.current) DarkKeyboardColors else LightKeyboardColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(KeyboardHeight)
            .background(keyboardColor)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        KeyboardRowKeyCounts.forEach { keyCount ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally)
            ) {
                repeat(keyCount) {
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .fillMaxHeight()
                            .clip(KeyboardKeyShape)
                            .background(keyColor)
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .clip(KeyboardKeyShape)
                    .background(keyColor)
            )
        }
    }
}
