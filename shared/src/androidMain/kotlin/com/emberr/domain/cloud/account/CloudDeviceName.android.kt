package com.emberr.domain.cloud.account

import android.os.Build

actual fun cloudDeviceName(): String =
    "${Build.MANUFACTURER} ${Build.MODEL}".take(MAX_CLOUD_DEVICE_NAME_LENGTH)
