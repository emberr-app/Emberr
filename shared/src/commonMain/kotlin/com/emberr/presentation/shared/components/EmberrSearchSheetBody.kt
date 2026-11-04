package com.emberr.presentation.shared.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emberr.presentation.shared.editor.hidesEditorToolbarWhileFocused
import com.emberr.presentation.shared.fullKeyboardHeightPx
import com.emberr.presentation.shared.heightWhileKeyboardMoves
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

private val SearchSheetListMaxHeight = 460.dp
private val SearchSheetListMinHeight = 280.dp
private val SearchBarSpace = 78.dp
private val SearchBarSidePadding = 20.dp

@Composable
fun EmberrSearchSheetBody(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    placeholder: String,
    contentHorizontalPadding: Dp = 0.dp,
    onSubmit: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val focusManager = LocalFocusManager.current
    val listHazeState = remember { HazeState() }
    val density = LocalDensity.current
    val keyboardHeightNow = WindowInsets.ime.getBottom(density)
    val fullKeyboardHeight = fullKeyboardHeightPx()
    val sheetHeaderHeight = LocalEmberrSheetHeaderSpace.current?.height ?: 0.dp
    val listScrollState = rememberScrollState()
    ReportSheetContentScrolled(listScrollState)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val listMaxHeight = with(density) {
            heightWhileKeyboardMoves(SearchSheetListMaxHeight + sheetHeaderHeight, maxHeight, keyboardHeightNow.toDp(), fullKeyboardHeight.toDp())
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = (SearchSheetListMinHeight + sheetHeaderHeight).coerceAtMost(listMaxHeight), max = listMaxHeight)
                .hazeSource(listHazeState)
                .verticalScroll(listScrollState)
                .padding(start = contentHorizontalPadding, end = contentHorizontalPadding, top = sheetHeaderHeight + 8.dp, bottom = SearchBarSpace)
                .navigationBarsPadding(),
            content = content
        )
        EmberrSearchTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            placeholder = placeholder,
            hazeState = listHazeState,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onSubmit = onSubmit ?: { focusManager.clearFocus() },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = SearchBarSidePadding, vertical = 14.dp)
                .hidesEditorToolbarWhileFocused()
        )
    }
}
