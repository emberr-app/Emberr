package com.emberr.domain.people

import com.emberr.domain.model.PersonDetailKind

class ContactToImport(
    val name: String,
    val details: List<ContactDetailToImport>,
    val note: String,
    val photoBytes: ByteArray?
)

data class ContactDetailToImport(
    val kind: PersonDetailKind,
    val label: String?,
    val value: String
)

private val dateWithDashes = Regex("""(\d{4})-(\d{2})-(\d{2})""")
private val dateWithoutDashes = Regex("""(\d{4})(\d{2})(\d{2})""")
private val dateWithoutYearWithDashes = Regex("""--(\d{2})-?(\d{2})""")
private val whitespace = Regex("""\s+""")
private const val PHONE_DIGITS_COMPARED = 9
private val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)

fun nameNotTakenYet(wantedName: String, takenNames: Collection<String>): String {
    val name = wantedName.trim()
    if (name.isEmpty()) return name
    val takenLowercaseNames = takenNames.map { it.trim().lowercase() }.toSet()
    if (name.lowercase() !in takenLowercaseNames) return name
    var number = 1
    while ("$name ($number)".lowercase() in takenLowercaseNames) number++
    return "$name ($number)"
}

fun contactAddsNothingNewTo(contact: ContactToImport, personName: String, personDetails: List<ContactDetailToImport>): Boolean {
    if (!hasSamePersonName(contact.name, personName)) return false
    return contact.details.all { detail -> personDetails.any { savedDetail -> isSameDetail(detail, savedDetail) } }
}

fun hasSamePersonName(first: String, second: String): Boolean = comparableName(first) == comparableName(second)

private fun comparableName(name: String): String = name.trim().lowercase().split(whitespace).joinToString(" ")

private fun isSameDetail(first: ContactDetailToImport, second: ContactDetailToImport): Boolean {
    if (first.kind != second.kind) return false
    if (first.kind != PersonDetailKind.PHONE) return comparableName(first.value) == comparableName(second.value)
    val firstDigits = first.value.filter { it.isDigit() }
    val secondDigits = second.value.filter { it.isDigit() }
    return if (firstDigits.length >= PHONE_DIGITS_COMPARED && secondDigits.length >= PHONE_DIGITS_COMPARED) {
        firstDigits.takeLast(PHONE_DIGITS_COMPARED) == secondDigits.takeLast(PHONE_DIGITS_COMPARED)
    } else {
        firstDigits == secondDigits
    }
}

fun contactDateToBirthdayText(text: String): String? {
    val dateOnly = text.trim().substringBefore('T')
    val birthday = dateWithDashes.matchEntire(dateOnly)?.let { readPersonBirthday(dateOnly) }
        ?: dateWithoutDashes.matchEntire(dateOnly)?.destructured?.let { (year, month, day) -> readPersonBirthday("$year-$month-$day") }
        ?: dateWithoutYearWithDashes.matchEntire(dateOnly)?.destructured?.let { (month, day) -> readPersonBirthday("--$month-$day") }
    return birthday?.toIsoText()
}

fun contactPhotoExtensionAndMimeType(photoBytes: ByteArray): Pair<String, String> {
    val isPng = photoBytes.size >= pngSignature.size && pngSignature.indices.all { index -> photoBytes[index] == pngSignature[index] }
    return if (isPng) "png" to "image/png" else "jpg" to "image/jpeg"
}
