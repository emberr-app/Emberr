package com.emberr.domain.util.format

fun formatByteSize(bytes: Long): String = when {
    bytes >= 1_000_000_000L -> "${decimalText((bytes / 100_000_000L) / 10.0, minimumFractionDigits = 1, maximumFractionDigits = 1)} GB"
    bytes >= 1_000_000L -> "${bytes / 1_000_000L} MB"
    bytes >= 1_000L -> "${bytes / 1_000L} KB"
    else -> "$bytes B"
}
