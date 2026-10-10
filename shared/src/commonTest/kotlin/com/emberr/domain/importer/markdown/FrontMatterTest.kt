package com.emberr.domain.importer.markdown

import kotlin.test.Test
import kotlin.test.assertEquals

class FrontMatterTest {

    @Test
    fun frontMatterIsSplitFromTheBody() {
        val withFrontMatter = FrontMatter.split("---\ntitle: Plan\n---\nBody")

        assertEquals(listOf(FrontMatterField("title", FrontMatterValue.Single("Plan"))), withFrontMatter.frontMatter)
        assertEquals("Body", withFrontMatter.body)
    }

    @Test
    fun aLeadingDividerIsNotMistakenForFrontMatter() {
        val markdown = "---\n\nJust text\n\n---\nMore"
        val withFrontMatter = FrontMatter.split(markdown)

        assertEquals(emptyList(), withFrontMatter.frontMatter)
        assertEquals(markdown, withFrontMatter.body)
    }

    @Test
    fun listsCanBeWrittenInBracketsOrAsDashedLines() {
        val fields = FrontMatter.parse(
            "tags: [project, \"two words\"]\naliases:\n  - First\n  - 'Second'\ntopics:\n- one\n- two\n"
        )

        assertEquals(
            listOf(
                FrontMatterField("tags", FrontMatterValue.Many(listOf("project", "two words"))),
                FrontMatterField("aliases", FrontMatterValue.Many(listOf("First", "Second"))),
                FrontMatterField("topics", FrontMatterValue.Many(listOf("one", "two")))
            ),
            fields
        )
    }

    @Test
    fun quotedAndMultiLineTextIsReadAndEmptyKeysAreLeftOut() {
        val fields = FrontMatter.parse("summary: \"Hello: world\"\nnotes: |\n  line one\n  line two\nempty:\n# a comment\n")

        assertEquals(
            listOf(
                FrontMatterField("summary", FrontMatterValue.Single("Hello: world")),
                FrontMatterField("notes", FrontMatterValue.Single("line one\nline two"))
            ),
            fields
        )
    }
}
