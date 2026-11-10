package com.emberr.presentation.widget

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.glance.LocalContext

@Composable
internal fun widgetText(@StringRes phrase: Int, vararg values: Any): String =
    LocalContext.current.getString(phrase, *values)
