package com.emberr.presentation.shared.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val DESKTOP_DIALOG_WIDTH_FRACTION = 0.6f
private const val DESKTOP_DIALOG_HEIGHT_FRACTION = 0.8f
private val DesktopDialogMaxWidth = 760.dp
private val DesktopDialogMaxHeight = 720.dp
private const val MOBILE_DIALOG_WIDTH_FRACTION = 0.94f
private const val MOBILE_DIALOG_HEIGHT_FRACTION = 0.92f
private val MobileDialogMaxWidth = 720.dp

fun Modifier.desktopDialogSize(): Modifier = this
    .fillMaxWidth(DESKTOP_DIALOG_WIDTH_FRACTION)
    .widthIn(max = DesktopDialogMaxWidth)
    .fillMaxHeight(DESKTOP_DIALOG_HEIGHT_FRACTION)
    .heightIn(max = DesktopDialogMaxHeight)

fun Modifier.mobileDialogSize(): Modifier = this
    .fillMaxWidth(MOBILE_DIALOG_WIDTH_FRACTION)
    .widthIn(max = MobileDialogMaxWidth)
    .fillMaxHeight(MOBILE_DIALOG_HEIGHT_FRACTION)
