package com.emberr.presentation.shared.editor.blockViews

import androidx.compose.ui.graphics.Color
import com.emberr.domain.model.CalloutType
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.`abstract`
import emberr.shared.generated.resources.bolt_lightning
import emberr.shared.generated.resources.bug
import emberr.shared.generated.resources.check_read
import emberr.shared.generated.resources.check_square
import emberr.shared.generated.resources.danger2
import emberr.shared.generated.resources.doc_add
import emberr.shared.generated.resources.fire
import emberr.shared.generated.resources.help_square
import emberr.shared.generated.resources.info
import emberr.shared.generated.resources.list_square
import emberr.shared.generated.resources.quote_down2
import emberr.shared.generated.resources.x
import org.jetbrains.compose.resources.DrawableResource

private val CalloutBlue = Color(0xFF086DDD)
private val CalloutCyan = Color(0xFF00BFBC)
private val CalloutGreen = Color(0xFF08B94E)
private val CalloutOrange = Color(0xFFEC7500)
private val CalloutRed = Color(0xFFE93147)
private val CalloutPurple = Color(0xFF7852EE)
private val CalloutGray = Color(0xFF9E9E9E)
private const val CalloutTintedBackgroundAlpha = 0.12f

fun CalloutType.accentColor(): Color = when (this) {
    CalloutType.INFO, CalloutType.TODO -> CalloutBlue
    CalloutType.ABSTRACT, CalloutType.TIP -> CalloutCyan
    CalloutType.SUCCESS -> CalloutGreen
    CalloutType.QUESTION, CalloutType.WARNING -> CalloutOrange
    CalloutType.FAILURE, CalloutType.DANGER, CalloutType.BUG -> CalloutRed
    CalloutType.EXAMPLE -> CalloutPurple
    CalloutType.NOTE, CalloutType.QUOTE -> CalloutGray
}

fun CalloutType.backgroundColor(surfaceColor: Color): Color = when (this) {
    CalloutType.NOTE, CalloutType.QUOTE -> surfaceColor
    else -> accentColor().copy(alpha = CalloutTintedBackgroundAlpha)
}

fun CalloutType.icon(): DrawableResource = when (this) {
    CalloutType.NOTE -> Res.drawable.doc_add
    CalloutType.ABSTRACT -> Res.drawable.`abstract`
    CalloutType.INFO -> Res.drawable.info
    CalloutType.TODO -> Res.drawable.check_square
    CalloutType.TIP -> Res.drawable.fire
    CalloutType.SUCCESS -> Res.drawable.check_read
    CalloutType.QUESTION -> Res.drawable.help_square
    CalloutType.WARNING -> Res.drawable.danger2
    CalloutType.FAILURE -> Res.drawable.x
    CalloutType.DANGER -> Res.drawable.bolt_lightning
    CalloutType.BUG -> Res.drawable.bug
    CalloutType.EXAMPLE -> Res.drawable.list_square
    CalloutType.QUOTE -> Res.drawable.quote_down2
}
