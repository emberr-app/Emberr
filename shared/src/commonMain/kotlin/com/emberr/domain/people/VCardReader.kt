package com.emberr.domain.people

import com.emberr.domain.importer.TextFileDecoder
import com.emberr.domain.model.PersonDetailKind
import kotlin.io.encoding.Base64

private val phoneLabels = mapOf("CELL" to "Mobile", "MOBILE" to "Mobile", "HOME" to "Home", "WORK" to "Work", "MAIN" to "Main")
private val emailLabels = mapOf("HOME" to "Personal", "WORK" to "Work")
private val addressLabels = mapOf("HOME" to "Home", "WORK" to "Work")
private val lenientBase64 = Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL)

private class VCardLine(
    val group: String?,
    val name: String,
    val parameters: List<Pair<String, String>>,
    val rawValue: String
) {
    val types: Set<String> = parameters
        .filter { (key, _) -> key == "TYPE" || key.isEmpty() }
        .flatMap { (_, value) -> value.split(',') }
        .map { type -> type.trim().uppercase() }
        .toSet()

    val isQuotedPrintable: Boolean = parameters.any { (key, value) ->
        (key == "ENCODING" && value.equals("QUOTED-PRINTABLE", ignoreCase = true)) || (key.isEmpty() && value.equals("QUOTED-PRINTABLE", ignoreCase = true))
    }

    val isBase64: Boolean = parameters.any { (key, value) ->
        val encoding = if (key == "ENCODING" || key.isEmpty()) value.uppercase() else ""
        encoding == "B" || encoding == "BASE64"
    }
}

private class VCardBuilder {
    var formattedName = ""
    var structuredName = ""
    var company = ""
    val note = StringBuilder()
    var photoBytes: ByteArray? = null
    val customLabelsByGroup = mutableMapOf<String, String>()
    private val details = mutableListOf<ContactDetailToImport>()
    private val groupsByDetailIndex = mutableMapOf<Int, String>()

    fun add(kind: PersonDetailKind, label: String?, value: String) {
        if (value.isNotBlank()) details += ContactDetailToImport(kind = kind, label = label, value = value.trim())
    }

    fun addWithLabel(line: VCardLine, knownLabels: Map<String, String>, kind: PersonDetailKind, value: String) {
        if (value.isBlank()) return
        line.group?.let { group -> groupsByDetailIndex[details.size] = group }
        add(kind, knownLabels.labelFor(line.types), value)
    }

    fun build(): ContactToImport {
        val labeledDetails = details.mapIndexed { index, detail ->
            val customLabel = groupsByDetailIndex[index]?.let { group -> customLabelsByGroup[group] }
            if (customLabel != null) detail.copy(label = customLabel) else detail
        }
        val name = formattedName.ifBlank { structuredName }.ifBlank { company }
            .ifBlank { labeledDetails.firstOrNull { it.kind == PersonDetailKind.PHONE || it.kind == PersonDetailKind.EMAIL }?.value.orEmpty() }
        return ContactToImport(name = name.trim(), details = labeledDetails, note = note.toString().trim(), photoBytes = photoBytes)
    }
}

fun readContactsFromVCardText(text: String): List<ContactToImport> {
    val contacts = mutableListOf<ContactToImport>()
    var currentContact: VCardBuilder? = null
    for (line in unfoldVCardLines(text)) {
        val parsedLine = parseVCardLine(line) ?: continue
        when {
            parsedLine.name == "BEGIN" && parsedLine.rawValue.equals("VCARD", ignoreCase = true) -> currentContact = VCardBuilder()
            parsedLine.name == "END" && parsedLine.rawValue.equals("VCARD", ignoreCase = true) -> {
                currentContact?.let { contact -> contacts += contact.build() }
                currentContact = null
            }
            else -> currentContact?.let { contact -> addVCardLine(contact, parsedLine) }
        }
    }
    return contacts
}

private fun addVCardLine(contact: VCardBuilder, line: VCardLine) {
    when (line.name) {
        "FN" -> contact.formattedName = decodedText(line)
        "N" -> contact.structuredName = nameFromParts(decodedParts(line))
        "TEL" -> contact.addWithLabel(line, phoneLabels, PersonDetailKind.PHONE, decodedText(line).removePrefix("tel:"))
        "EMAIL" -> contact.addWithLabel(line, emailLabels, PersonDetailKind.EMAIL, decodedText(line).removePrefix("mailto:"))
        "ADR" -> contact.addWithLabel(line, addressLabels, PersonDetailKind.ADDRESS, addressFromParts(decodedParts(line)))
        "BDAY" -> {
            val date = decodedText(line)
            val birthdayText = contactDateToBirthdayText(date)
            if (birthdayText != null) contact.add(PersonDetailKind.BIRTHDAY, null, birthdayText) else contact.add(PersonDetailKind.CUSTOM, "Birthday", date)
        }
        "ANNIVERSARY", "X-ANNIVERSARY" -> contact.add(PersonDetailKind.CUSTOM, "Anniversary", decodedText(line))
        "ORG" -> {
            contact.company = decodedParts(line).filter { it.isNotBlank() }.joinToString(", ")
            contact.add(PersonDetailKind.COMPANY, null, contact.company)
        }
        "TITLE" -> contact.add(PersonDetailKind.ROLE, null, decodedText(line))
        "URL" -> contact.add(PersonDetailKind.WEBSITE, null, decodedText(line))
        "X-SOCIALPROFILE" -> contact.add(PersonDetailKind.SOCIAL, line.types.firstOrNull()?.lowercase()?.replaceFirstChar { it.uppercase() }, decodedText(line))
        "NICKNAME" -> contact.add(PersonDetailKind.CUSTOM, "Nickname", decodedText(line))
        "NOTE" -> {
            if (contact.note.isNotEmpty()) contact.note.append('\n')
            contact.note.append(decodedText(line))
        }
        "PHOTO" -> if (contact.photoBytes == null) contact.photoBytes = photoBytesOrNull(line)
        "X-ABLABEL" -> line.group?.let { group -> contact.customLabelsByGroup[group] = appleLabel(decodedText(line)) }
    }
}

private fun unfoldVCardLines(text: String): List<String> {
    val lines = mutableListOf<String>()
    for (physicalLine in text.split("\r\n", "\n", "\r")) {
        val previousLine = lines.lastOrNull()
        when {
            previousLine != null && (physicalLine.startsWith(' ') || physicalLine.startsWith('\t')) ->
                lines[lines.lastIndex] = previousLine + physicalLine.substring(1)
            previousLine != null && previousLine.endsWith('=') && previousLine.contains("QUOTED-PRINTABLE", ignoreCase = true) ->
                lines[lines.lastIndex] = previousLine.dropLast(1) + physicalLine
            physicalLine.isNotBlank() -> lines += physicalLine
        }
    }
    return lines
}

private fun parseVCardLine(line: String): VCardLine? {
    var isInsideQuotes = false
    var colonIndex = -1
    for (index in line.indices) {
        when (line[index]) {
            '"' -> isInsideQuotes = !isInsideQuotes
            ':' -> if (!isInsideQuotes) {
                colonIndex = index
                break
            }
        }
    }
    if (colonIndex <= 0) return null
    val nameAndParameters = line.substring(0, colonIndex).split(';')
    val fullName = nameAndParameters.first()
    val parameters = nameAndParameters.drop(1).map { parameter ->
        val equalsIndex = parameter.indexOf('=')
        if (equalsIndex < 0) "" to parameter.trim()
        else parameter.substring(0, equalsIndex).trim().uppercase() to parameter.substring(equalsIndex + 1).trim().removeSurrounding("\"")
    }
    return VCardLine(
        group = fullName.substringBeforeLast('.', missingDelimiterValue = "").ifEmpty { null }?.uppercase(),
        name = fullName.substringAfterLast('.').trim().uppercase(),
        parameters = parameters,
        rawValue = line.substring(colonIndex + 1)
    )
}

private fun decodedText(line: VCardLine): String = unescape(quotedPrintableDecoded(line))

private fun decodedParts(line: VCardLine): List<String> = splitOnUnescapedSemicolons(quotedPrintableDecoded(line)).map(::unescape)

private fun quotedPrintableDecoded(line: VCardLine): String {
    if (!line.isQuotedPrintable) return line.rawValue
    val bytes = mutableListOf<Byte>()
    var index = 0
    val value = line.rawValue
    while (index < value.length) {
        val character = value[index]
        val hexCode = if (character == '=' && index + 2 < value.length) value.substring(index + 1, index + 3).toIntOrNull(16) else null
        if (hexCode != null) {
            bytes += hexCode.toByte()
            index += 3
        } else {
            character.toString().encodeToByteArray().forEach { byte -> bytes += byte }
            index++
        }
    }
    return TextFileDecoder.decode(bytes.toByteArray())
}

private fun splitOnUnescapedSemicolons(value: String): List<String> {
    val parts = mutableListOf<String>()
    val currentPart = StringBuilder()
    var index = 0
    while (index < value.length) {
        val character = value[index]
        when {
            character == '\\' && index + 1 < value.length -> {
                currentPart.append(character).append(value[index + 1])
                index++
            }
            character == ';' -> {
                parts += currentPart.toString()
                currentPart.clear()
            }
            else -> currentPart.append(character)
        }
        index++
    }
    parts += currentPart.toString()
    return parts
}

private fun unescape(value: String): String {
    val result = StringBuilder()
    var index = 0
    while (index < value.length) {
        val character = value[index]
        if (character == '\\' && index + 1 < value.length) {
            val nextCharacter = value[index + 1]
            result.append(if (nextCharacter == 'n' || nextCharacter == 'N') '\n' else nextCharacter)
            index += 2
        } else {
            result.append(character)
            index++
        }
    }
    return result.toString().trim()
}

private fun nameFromParts(parts: List<String>): String {
    val familyName = parts.getOrNull(0).orEmpty()
    val givenName = parts.getOrNull(1).orEmpty()
    val middleName = parts.getOrNull(2).orEmpty()
    val prefix = parts.getOrNull(3).orEmpty()
    val suffix = parts.getOrNull(4).orEmpty()
    return listOf(prefix, givenName, middleName, familyName, suffix).filter { it.isNotBlank() }.joinToString(" ")
}

private fun addressFromParts(parts: List<String>): String {
    val poBox = parts.getOrNull(0).orEmpty()
    val extendedAddress = parts.getOrNull(1).orEmpty()
    val street = parts.getOrNull(2).orEmpty()
    val city = parts.getOrNull(3).orEmpty()
    val region = parts.getOrNull(4).orEmpty()
    val postalCode = parts.getOrNull(5).orEmpty()
    val country = parts.getOrNull(6).orEmpty()
    val regionAndPostalCode = listOf(region, postalCode).filter { it.isNotBlank() }.joinToString(" ")
    return listOf(poBox, extendedAddress, street, city, regionAndPostalCode, country)
        .map { part -> part.replace('\n', ' ').trim() }
        .filter { it.isNotBlank() }
        .joinToString(", ")
}

private fun photoBytesOrNull(line: VCardLine): ByteArray? {
    val value = line.rawValue.trim()
    val base64Text = when {
        value.startsWith("data:", ignoreCase = true) && value.contains(";base64,", ignoreCase = true) -> value.substringAfter(',')
        line.isBase64 -> value
        else -> return null
    }
    return try {
        lenientBase64.decode(base64Text.filterNot { it.isWhitespace() }).takeIf { it.isNotEmpty() }
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun appleLabel(label: String): String = label.removePrefix("_\$!<").removeSuffix(">!\$_")

private fun Map<String, String>.labelFor(types: Set<String>): String? = types.firstNotNullOfOrNull { type -> this[type] }
