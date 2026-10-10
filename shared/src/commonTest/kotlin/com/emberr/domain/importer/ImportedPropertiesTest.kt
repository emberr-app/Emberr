package com.emberr.domain.importer

import com.emberr.domain.importer.markdown.FrontMatterField
import com.emberr.domain.importer.markdown.FrontMatterValue
import com.emberr.domain.model.PropertyBlock
import com.emberr.domain.model.PropertyType
import com.emberr.domain.model.PropertyValueType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImportedPropertiesTest {

    private fun single(text: String) = FrontMatterValue.Single(text)

    private fun many(vararg items: String) = FrontMatterValue.Many(items.toList())

    private fun customBlock(valueType: PropertyValueType) =
        PropertyBlock(id = "property", customPropertyId = "custom", customLabel = "Custom", customValueType = valueType)

    @Test
    fun tagsFromFrontMatterAndFromTheTextAreMergedIntoTheTagsProperty() {
        val details = ImportedProperties.detailsOf(
            frontMatter = listOf(FrontMatterField("tags", many("project", "#Emberr"))),
            inlineTags = listOf("emberr", "importer")
        )

        assertEquals(
            listOf(ImportedProperty(PropertyDestination.BuiltIn(PropertyType.TAGS), many("project", "Emberr", "importer"))),
            details.properties
        )
    }

    @Test
    fun tagsFoundOnlyInTheTextStillGetATagsPropertyAtTheTop() {
        val details = ImportedProperties.detailsOf(
            frontMatter = listOf(FrontMatterField("author", single("Ann"))),
            inlineTags = listOf("idea")
        )

        assertEquals(PropertyDestination.BuiltIn(PropertyType.TAGS), details.properties.first().destination)
        assertEquals(many("idea"), details.properties.first().value)
    }

    @Test
    fun theCreatedDateBecomesTheNotesCreatedDateAndToolKeysAreLeftOut() {
        val details = ImportedProperties.detailsOf(
            frontMatter = listOf(
                FrontMatterField("created", single("2026-10-01")),
                FrontMatterField("updated", single("2026-10-02")),
                FrontMatterField("cssclasses", many("wide"))
            ),
            inlineTags = emptyList()
        )

        val expectedCreatedAt = LocalDateTime(2026, 10, 1, 0, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        assertEquals(expectedCreatedAt, details.createdAt)
        assertEquals(emptyList(), details.properties)
    }

    @Test
    fun keysNamedLikeEmberrPropertiesUseThoseProperties() {
        val details = ImportedProperties.detailsOf(
            frontMatter = listOf(FrontMatterField("status", single("draft")), FrontMatterField("Due_Date", single("2026-10-20"))),
            inlineTags = emptyList()
        )

        assertEquals(
            listOf(PropertyDestination.BuiltIn(PropertyType.STATUS), PropertyDestination.BuiltIn(PropertyType.DUE_DATE)),
            details.properties.map { it.destination }
        )
    }

    @Test
    fun aliasesBecomeOneTextProperty() {
        val details = ImportedProperties.detailsOf(
            frontMatter = listOf(FrontMatterField("aliases", many("Plan v1", "Plan"))),
            inlineTags = emptyList()
        )

        assertEquals(
            listOf(ImportedProperty(PropertyDestination.Custom("aliases", PropertyValueType.TEXT), single("Plan v1, Plan"))),
            details.properties
        )
    }

    @Test
    fun otherKeysBecomeCustomPropertiesTypedByTheirValue() {
        val details = ImportedProperties.detailsOf(
            frontMatter = listOf(
                FrontMatterField("topics", many("design")),
                FrontMatterField("deadline", single("2026-10-20")),
                FrontMatterField("done", single("true")),
                FrontMatterField("rating", single("4.5")),
                FrontMatterField("author", single("Ann"))
            ),
            inlineTags = emptyList()
        )

        assertEquals(
            listOf(
                PropertyDestination.Custom("topics", PropertyValueType.TAGS),
                PropertyDestination.Custom("deadline", PropertyValueType.DATE),
                PropertyDestination.Custom("done", PropertyValueType.CHECKBOX),
                PropertyDestination.Custom("rating", PropertyValueType.NUMBER),
                PropertyDestination.Custom("author", PropertyValueType.TEXT)
            ),
            details.properties.map { it.destination }
        )
    }

    @Test
    fun aPropertyBlockTakesTheValueInTheShapeOfItsType() {
        assertEquals(
            listOf("a", "b", "c"),
            PropertyBlock(id = "tags", propertyType = PropertyType.TAGS).withImportedValue(single("a, b c"))?.tags
        )
        assertEquals(
            listOf("Draft"),
            PropertyBlock(id = "status", propertyType = PropertyType.STATUS).withImportedValue(many("Draft", "Done"))?.tags
        )

        val dateBlock = customBlock(PropertyValueType.DATE).withImportedValue(single("2026-10-20 14:30"))
        assertEquals(LocalDate(2026, 10, 20), dateBlock?.date)
        assertEquals(LocalTime(14, 30), dateBlock?.time)

        assertEquals(true, customBlock(PropertyValueType.CHECKBOX).withImportedValue(single("yes"))?.isChecked)
        assertEquals("a, b", customBlock(PropertyValueType.TEXT).withImportedValue(many("a", "b"))?.text)
    }

    @Test
    fun aValueThatDoesNotFitTheTypeIsLeftOut() {
        assertNull(customBlock(PropertyValueType.DATE).withImportedValue(single("someday")))
        assertNull(customBlock(PropertyValueType.CHECKBOX).withImportedValue(single("maybe")))
    }
}
