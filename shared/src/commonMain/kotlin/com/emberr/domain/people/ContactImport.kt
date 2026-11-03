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

fun isExactCopyOfPerson(contact: ContactToImport, personName: String, personDetails: List<ContactDetailToImport>): Boolean =
    contact.name.trim().equals(personName.trim(), ignoreCase = true) &&
        contact.details.map(::comparableDetail).toSet() == personDetails.map(::comparableDetail).toSet()

private fun comparableDetail(detail: ContactDetailToImport): Triple<PersonDetailKind, String, String> {
    val value = detail.value.trim().lowercase()
    val comparableValue = if (detail.kind == PersonDetailKind.PHONE) value.filter { it.isDigit() || it == '+' } else value
    return Triple(detail.kind, detail.label?.trim()?.lowercase().orEmpty(), comparableValue)
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
