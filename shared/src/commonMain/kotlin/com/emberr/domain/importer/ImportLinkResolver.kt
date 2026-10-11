package com.emberr.domain.importer

interface ImportLinkResolver {
    fun noteIdFor(linkTarget: String): String?
    fun attachmentPathFor(linkTarget: String): String?
}
