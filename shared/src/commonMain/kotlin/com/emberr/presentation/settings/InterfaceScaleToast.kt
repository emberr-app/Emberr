package com.emberr.presentation.settings

import com.emberr.domain.util.system.showNativeToast

fun showInterfaceScaleToast(percent: Int) {
    showNativeToast("Interface scale $percent%")
}
