package com.emberr.domain.cloud.account

actual fun cloudDeviceName(): String =
    "Emberr Desktop (${System.getProperty("os.name")})".take(MAX_CLOUD_DEVICE_NAME_LENGTH)
