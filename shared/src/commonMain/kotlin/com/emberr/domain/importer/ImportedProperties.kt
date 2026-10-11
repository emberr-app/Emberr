package com.emberr.domain.importer

import com.emberr.domain.importer.markdown.FrontMatterField
import com.emberr.domain.importer.markdown.FrontMatterValue
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.PropertyType
import com.emberr.domain.model.PropertyValueType
import com.emberr.domain.model.cleanPropertyTagName
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

private val keysForCreatedDate = setOf("created", "date")
private val keysLeftOut = setOf("title", "updated", "modified", "cssclasses", "cssclass")
private val keysForTags = setOf("tags", "tag")
private val keysForAliases = setOf("aliases", "alias")
private val builtInPropertiesByLabel: Map<String, PropertyType> = PropertyType.entries
    .filterNot { it == PropertyType.DATE || it == PropertyType.TAGS || it == PropertyType.FORMULA }
    .associateBy { it.label.lowercase() }

private val dateAndTimePattern = Regex("""^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?""")
private val timeZoneEnding = Regex("""^(?:\.\d+)?(?:Z|[+-]\d{2}:?\d{2})?$""")
private val numberPattern = Regex("""^-?\d+(?:\.\d+)?$""")
private val tagSeparators = Regex("""[,\s]+""")

internal sealed interface PropertyDestination {
    data class BuiltIn(val propertyType: PropertyType) : PropertyDestination
    data class Custom(val name: String, val valueType: PropertyValueType) : PropertyDestination
}

internal data class ImportedProperty(val destination: PropertyDestination, val value: FrontMatterValue)

internal data class ImportedNoteDetails(val createdAt: Long?, val properties: List<ImportedProperty>)

internal object ImportedProperties {

    fun detailsOf(frontMatter: List<FrontMatterField>, inlineTags: List<String>): ImportedNoteDetails {
        var createdAt: Long? = null
        var hasTagsProperty = false
        val properties = mutableListOf<ImportedProperty>()

        for (field in frontMatter) {
            val key = comparableKey(field.key)
            val builtInProperty = builtInPropertiesByLabel[key]
            when {
                key in keysForCreatedDate -> if (createdAt == null) createdAt = epochMillisOf(textOf(field.value))
                key in keysLeftOut -> Unit
                key in keysForTags -> if (!hasTagsProperty) {
                    tagsProperty(tagNamesOf(field.value) + inlineTags)?.let { properties += it }
                    hasTagsProperty = true
                }
                key in keysForAliases -> properties += ImportedProperty(
                    PropertyDestination.Custom(field.key.trim(), PropertyValueType.TEXT),
                    FrontMatterValue.Single(textOf(field.value))
                )
                builtInProperty != null -> properties += ImportedProperty(PropertyDestination.BuiltIn(builtInProperty), field.value)
                else -> properties += ImportedProperty(
                    PropertyDestination.Custom(field.key.trim(), valueTypeOf(field.value)),
                    field.value
                )
            }
        }

        if (!hasTagsProperty) tagsProperty(inlineTags)?.let { properties.add(0, it) }
        return ImportedNoteDetails(createdAt = createdAt, properties = properties)
    }

    private fun tagsProperty(tagNames: List<String>): ImportedProperty? {
        val cleanedTagNames = cleanedTags(tagNames)
        if (cleanedTagNames.isEmpty()) return null
        return ImportedProperty(PropertyDestination.BuiltIn(PropertyType.TAGS), FrontMatterValue.Many(cleanedTagNames))
    }

    private fun valueTypeOf(value: FrontMatterValue): PropertyValueType {
        val text = when (value) {
            is FrontMatterValue.Many -> return PropertyValueType.TAGS
            is FrontMatterValue.Single -> value.text.trim()
        }
        return when {
            dateAndTimeOf(text) != null -> PropertyValueType.DATE
            text.equals("true", ignoreCase = true) || text.equals("false", ignoreCase = true) -> PropertyValueType.CHECKBOX
            numberPattern.matches(text) -> PropertyValueType.NUMBER
            else -> PropertyValueType.TEXT
        }
    }

    private fun comparableKey(key: String): String =
        key.trim().lowercase().replace('_', ' ').replace('-', ' ').replace(whitespaceRun, " ")
}

internal fun PropertyBlock.withImportedValue(value: FrontMatterValue): PropertyBlock? {
    val text = textOf(value).trim()
    return when {
        valueType.holdsTags -> {
            val tagNames = if (valueType.allowsManyTags) {
                cleanedTags(tagNamesOf(value))
            } else {
                cleanedTags(listOfNotNull(firstTextOf(value)))
            }
            if (tagNames.isEmpty()) null else copy(tags = tagNames)
        }
        valueType.holdsDate -> dateAndTimeOf(text)?.let { (date, time) -> copy(date = date, time = time) }
        valueType.holdsCheck -> booleanOf(text)?.let { isChecked -> copy(isChecked = isChecked) }
        valueType.holdsFormula -> null
        text.isEmpty() -> null
        else -> copy(text = text)
    }
}

private fun textOf(value: FrontMatterValue): String = when (value) {
    is FrontMatterValue.Single -> value.text
    is FrontMatterValue.Many -> value.items.joinToString(", ")
}

private fun firstTextOf(value: FrontMatterValue): String? = when (value) {
    is FrontMatterValue.Single -> value.text
    is FrontMatterValue.Many -> value.items.firstOrNull()
}

private fun tagNamesOf(value: FrontMatterValue): List<String> = when (value) {
    is FrontMatterValue.Single -> value.text.split(tagSeparators)
    is FrontMatterValue.Many -> value.items
}

private fun cleanedTags(tagNames: List<String>): List<String> =
    tagNames
        .map { cleanPropertyTagName(it.trim().removePrefix("#")) }
        .filter { it.isNotEmpty() }
        .distinctBy { it.lowercase() }

private fun booleanOf(text: String): Boolean? = when (text.lowercase()) {
    "true", "yes" -> true
    "false", "no" -> false
    else -> null
}

private fun dateAndTimeOf(text: String): Pair<LocalDate, LocalTime?>? {
    val match = dateAndTimePattern.find(text) ?: return null
    if (!timeZoneEnding.matches(text.substring(match.range.last + 1))) return null
    val numbers = match.groupValues.drop(1).map { it.toIntOrNull() }
    val year = numbers[0] ?: return null
    val month = numbers[1] ?: return null
    val day = numbers[2] ?: return null
    val hour = numbers[3]
    val minute = numbers[4]
    return try {
        val date = LocalDate(year, month, day)
        val time = if (hour != null && minute != null) LocalTime(hour, minute, numbers[5] ?: 0) else null
        date to time
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun epochMillisOf(text: String): Long? {
    val (date, time) = dateAndTimeOf(text.trim()) ?: return null
    return LocalDateTime(date, time ?: LocalTime(0, 0)).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
}
