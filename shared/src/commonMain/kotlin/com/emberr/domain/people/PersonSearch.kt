package com.emberr.domain.people

import com.emberr.data.local.room.entity.NoteMetadataEntity
import com.emberr.domain.model.PersonDetailBlock
import com.emberr.domain.model.PersonDetailKind
import com.emberr.domain.util.search.containsForSearch

data class PersonSearchResult(
    val person: NoteMetadataEntity,
    val shownDetail: PersonDetailBlock?
)

private val phonePunctuation = Regex("[\\s\\-()]")

fun searchPeople(people: List<PersonWithDetails>, query: String): List<PersonSearchResult> {
    val trimmedQuery = query.trim()
    if (trimmedQuery.isEmpty()) return emptyList()
    val queryWithoutPhonePunctuation = trimmedQuery.replace(phonePunctuation, "")

    val nameMatches = mutableListOf<PersonSearchResult>()
    val detailMatches = mutableListOf<PersonSearchResult>()
    for ((person, details) in people) {
        if (person.trashedAt != null) continue
        val liveDetails = details.filterNot { it.isDeleted }
        if (person.title.containsForSearch(trimmedQuery)) {
            val firstPhoneOrEmail = liveDetails.firstOrNull { it.kind == PersonDetailKind.PHONE }
                ?: liveDetails.firstOrNull { it.kind == PersonDetailKind.EMAIL }
            nameMatches += PersonSearchResult(person, firstPhoneOrEmail)
        } else {
            val matchingDetail = liveDetails.firstOrNull { detail ->
                detail.matches(trimmedQuery, queryWithoutPhonePunctuation)
            } ?: continue
            detailMatches += PersonSearchResult(person, matchingDetail)
        }
    }
    val byName = compareBy<PersonSearchResult>({ it.person.title.isBlank() }, { it.person.title.trim().lowercase() })
    return nameMatches.sortedWith(byName) + detailMatches.sortedWith(byName)
}

private fun PersonDetailBlock.matches(query: String, queryWithoutPhonePunctuation: String): Boolean {
    if (value.containsForSearch(query)) return true
    if (label?.containsForSearch(query) == true) return true
    return kind == PersonDetailKind.PHONE &&
        queryWithoutPhonePunctuation.isNotEmpty() &&
        value.replace(phonePunctuation, "").contains(queryWithoutPhonePunctuation)
}
