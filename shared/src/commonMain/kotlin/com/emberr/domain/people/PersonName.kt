package com.emberr.domain.people

fun personDisplayName(title: String): String = title.trim().ifEmpty { "Untitled" }
