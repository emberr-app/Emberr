package com.emberr.domain.util.system

import com.emberr.presentation.desktop.DesktopToastBus

actual fun showNativeToast(message: String) {
    DesktopToastBus.show(message)
}
