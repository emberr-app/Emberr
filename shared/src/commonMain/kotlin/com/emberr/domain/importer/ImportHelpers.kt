package com.emberr.domain.importer

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal val whitespaceRun = Regex("\\s+")

@OptIn(ExperimentalUuidApi::class)
internal fun newImportId(): String = Uuid.random().toString()
