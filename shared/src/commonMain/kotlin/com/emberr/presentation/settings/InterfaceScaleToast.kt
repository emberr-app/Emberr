package com.emberr.presentation.settings

import com.emberr.domain.util.system.showNativeToast
import emberr.shared.generated.resources.Res
import emberr.shared.generated.resources.settings_interface_scale_toast
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

private val interfaceScaleToastScope = MainScope()

fun showInterfaceScaleToast(percent: Int) {
    interfaceScaleToastScope.launch {
        showNativeToast(getString(Res.string.settings_interface_scale_toast, percent))
    }
}
