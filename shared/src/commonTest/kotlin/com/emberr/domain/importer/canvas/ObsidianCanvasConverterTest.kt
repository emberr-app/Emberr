package com.emberr.domain.importer.canvas

import com.emberr.data.local.room.entity.CanvasEdgeEntity
import com.emberr.data.local.room.entity.CanvasNodeEntity
import com.emberr.data.local.room.entity.CanvasNodeType
import com.emberr.data.local.room.entity.CanvasSide
import com.emberr.domain.importer.ImportLinkResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

private const val NOW = 1_700_000_000_000L

class ObsidianCanvasConverterTest {

    private class FakeLinkResolver(
        private val noteIdsByTarget: Map<String, String> = emptyMap(),
        private val attachmentPathsByTarget: Map<String, String> = emptyMap()
    ) : ImportLinkResolver {
        override fun noteIdFor(linkTarget: String): String? = noteIdsByTarget[linkTarget]
        override fun attachmentPathFor(linkTarget: String): String? = attachmentPathsByTarget[linkTarget]
    }

    private fun convert(
        canvasJson: String,
        linkResolver: ImportLinkResolver = FakeLinkResolver(),
        noteTitlesById: Map<String, String> = emptyMap()
    ): ConvertedCanvas {
        var lastIdNumber = 0
        return ObsidianCanvasConverter.convert(canvasJson, linkResolver, noteTitlesById, NOW) { "id-${++lastIdNumber}" }
    }

    private fun node(id: String, type: String, x: Int = 0, y: Int = 0, width: Int = 250, height: Int = 60, extraFields: String = ""): String =
        """{"id":"$id","type":"$type","x":$x,"y":$y,"width":$width,"height":$height$extraFields}"""

    private fun canvasOf(nodes: List<String>, edges: List<String> = emptyList()): String =
        """{"nodes":[${nodes.joinToString(",")}],"edges":[${edges.joinToString(",")}]}"""

    private fun emberrNode(nodeId: String, text: String, x: Float = 40f, y: Float = 40f, width: Float = 200f, height: Float = 48f) = CanvasNodeEntity(
        nodeId = nodeId,
        noteId = "",
        x = x,
        y = y,
        width = width,
        height = height,
        text = text,
        createdAt = NOW,
        updatedAt = NOW
    )

    private fun emberrEdge(edgeId: String, fromNodeId: String, fromSide: CanvasSide, toNodeId: String, toSide: CanvasSide) = CanvasEdgeEntity(
        edgeId = edgeId,
        noteId = "",
        fromNodeId = fromNodeId,
        fromSide = fromSide,
        toNodeId = toNodeId,
        toSide = toSide,
        createdAt = NOW,
        updatedAt = NOW
    )

    @Test
    fun aTextNodeBecomesATextCardWithPlainText() {
        val converted = convert(canvasOf(listOf(node("a", "text", extraFields = ""","text":"**Buy** milk""""))))

        assertEquals(listOf(emberrNode("id-1", "Buy milk")), converted.content.nodes)
        assertTrue(converted.pendingImages.isEmpty())
    }

    @Test
    fun nodesAreShrunkAndMovedSoTheTopLeftOfTheContentIsInView() {
        val converted = convert(
            canvasOf(
                listOf(
                    node("a", "text", x = -500, y = -300, extraFields = ""","text":"A""""),
                    node("b", "text", x = 100, y = 200, extraFields = ""","text":"B"""")
                )
            )
        )

        assertEquals(listOf(40f to 40f, 520f to 440f), converted.content.nodes.map { it.x to it.y })
    }

    @Test
    fun decimalPositionsAreAccepted() {
        val converted = convert("""{"nodes":[{"id":"a","type":"text","text":"A","x":12.5,"y":-2.5,"width":250.5,"height":60}]}""")

        assertEquals(listOf(emberrNode("id-1", "A", width = 200.4f)), converted.content.nodes)
    }

    @Test
    fun tinyNodesGrowToTheSmallestSizeTheCanvasCanShow() {
        val converted = convert(canvasOf(listOf(node("a", "text", width = 20, height = 10, extraFields = ""","text":"A""""))))

        assertEquals(80f to 40f, converted.content.nodes.single().let { it.width to it.height })
    }

    @Test
    fun aLinkNodeBecomesACardWithItsAddress() {
        val converted = convert(canvasOf(listOf(node("a", "link", extraFields = ""","url":"https://obsidian.md""""))))

        assertEquals(listOf(emberrNode("id-1", "https://obsidian.md")), converted.content.nodes)
    }

    @Test
    fun aGroupNodeBecomesAGroupWithItsLabel() {
        val converted = convert(
            canvasOf(
                listOf(
                    node("a", "group", extraFields = ""","label":"Ideas","background":"bg.png","backgroundStyle":"cover""""),
                    node("b", "group", x = 400)
                )
            )
        )

        assertEquals(
            listOf(
                emberrNode("id-1", "Ideas").copy(type = CanvasNodeType.GROUP),
                emberrNode("id-2", "", x = 360f).copy(type = CanvasNodeType.GROUP)
            ),
            converted.content.nodes
        )
    }

    @Test
    fun aFileNodeForAnImageBecomesAnImageNodeWaitingForItsFile() {
        val converted = convert(
            canvasOf(listOf(node("a", "file", extraFields = ""","file":"Images/cat.png""""))),
            linkResolver = FakeLinkResolver(attachmentPathsByTarget = mapOf("Images/cat.png" to "Vault/Images/cat.png"))
        )

        assertEquals(listOf(emberrNode("id-1", "").copy(type = CanvasNodeType.IMAGE)), converted.content.nodes)
        assertEquals(listOf(PendingCanvasImage(nodeId = "id-1", relativePath = "Vault/Images/cat.png")), converted.pendingImages)
    }

    @Test
    fun aFileNodeForANoteShowsTheNoteTitleAndHeading() {
        val converted = convert(
            canvasOf(
                listOf(
                    node("a", "file", extraFields = ""","file":"Notes/Plan.md""""),
                    node("b", "file", extraFields = ""","file":"Notes/Plan.md","subpath":"#Goals""""),
                    node("c", "file", extraFields = ""","file":"Notes/Plan.md","subpath":"#^a1b2c3""""),
                    node("d", "file", extraFields = ""","file":"Boards/Roadmap.canvas"""")
                )
            ),
            linkResolver = FakeLinkResolver(noteIdsByTarget = mapOf("Notes/Plan.md" to "note-plan", "Boards/Roadmap.canvas" to "note-roadmap")),
            noteTitlesById = mapOf("note-plan" to "Project plan")
        )

        assertEquals(
            listOf("Project plan", "Project plan › Goals", "Project plan", "Roadmap"),
            converted.content.nodes.map { it.text }
        )
        assertTrue(converted.content.nodes.all { it.type == CanvasNodeType.TEXT })
    }

    @Test
    fun aFileNodeForAnyOtherFileShowsTheFileName() {
        val converted = convert(
            canvasOf(
                listOf(
                    node("a", "file", extraFields = ""","file":"Docs/report.pdf""""),
                    node("b", "file", extraFields = ""","file":"Images/missing.png"""")
                )
            ),
            linkResolver = FakeLinkResolver(attachmentPathsByTarget = mapOf("Docs/report.pdf" to "Docs/report.pdf"))
        )

        assertEquals(listOf("report.pdf", "missing.png"), converted.content.nodes.map { it.text })
        assertTrue(converted.pendingImages.isEmpty())
    }

    @Test
    fun nodeColorsBecomeEmberrColorNames() {
        val converted = convert(
            canvasOf(
                listOf(
                    node("a", "text", extraFields = ""","text":"A","color":"5""""),
                    node("b", "text", extraFields = ""","text":"B","color":"#FF0000""""),
                    node("c", "text", extraFields = ""","text":"C"""")
                )
            )
        )

        assertEquals(listOf("blue", "red", null), converted.content.nodes.map { it.color })
    }

    @Test
    fun nodesKeepTheirOrder() {
        val converted = convert(
            canvasOf(
                listOf(
                    node("back", "text", extraFields = ""","text":"Back""""),
                    node("front", "text", extraFields = ""","text":"Front"""")
                )
            )
        )

        assertEquals(listOf("Back", "Front"), converted.content.nodes.map { it.text })
    }

    @Test
    fun edgesKeepTheirSidesAndLabel() {
        val converted = convert(
            canvasOf(
                nodes = listOf(node("a", "text", extraFields = ""","text":"A""""), node("b", "text", y = 200, extraFields = ""","text":"B"""")),
                edges = listOf("""{"id":"e","fromNode":"a","fromSide":"left","toNode":"b","toSide":"right","color":"1","label":"next"}""")
            )
        )

        assertEquals(listOf(emberrEdge("id-3", "id-1", CanvasSide.LEFT, "id-2", CanvasSide.RIGHT).copy(label = "next")), converted.content.edges)
    }

    @Test
    fun aLabelWrittenOnSeveralLinesBecomesOneLineAndABlankLabelIsDropped() {
        val converted = convert(
            canvasOf(
                nodes = listOf(node("a", "text", extraFields = ""","text":"A""""), node("b", "text", y = 200, extraFields = ""","text":"B"""")),
                edges = listOf(
                    """{"id":"e1","fromNode":"a","toNode":"b","label":"leads\nto"}""",
                    """{"id":"e2","fromNode":"a","toNode":"b","label":"  "}"""
                )
            )
        )

        assertEquals(listOf("leads to", null), converted.content.edges.map { it.label })
    }

    @Test
    fun missingSidesFaceTheOtherNode() {
        val converted = convert(
            canvasOf(
                nodes = listOf(
                    node("a", "text", width = 100, height = 50, extraFields = ""","text":"A""""),
                    node("right", "text", x = 300, width = 100, height = 50, extraFields = ""","text":"Right""""),
                    node("below", "text", y = 200, width = 100, height = 50, extraFields = ""","text":"Below"""")
                ),
                edges = listOf(
                    """{"id":"e1","fromNode":"a","toNode":"right"}""",
                    """{"id":"e2","fromNode":"right","toNode":"a"}""",
                    """{"id":"e3","fromNode":"a","toNode":"below"}""",
                    """{"id":"e4","fromNode":"below","fromSide":"right","toNode":"a"}"""
                )
            )
        )

        assertEquals(
            listOf(
                emberrEdge("id-4", "id-1", CanvasSide.RIGHT, "id-2", CanvasSide.LEFT),
                emberrEdge("id-5", "id-2", CanvasSide.LEFT, "id-1", CanvasSide.RIGHT),
                emberrEdge("id-6", "id-1", CanvasSide.BOTTOM, "id-3", CanvasSide.TOP),
                emberrEdge("id-7", "id-3", CanvasSide.RIGHT, "id-1", CanvasSide.BOTTOM)
            ),
            converted.content.edges
        )
    }

    @Test
    fun anArrowOnlyAtTheStartIsTurnedAroundSoItStillPointsTheSameWay() {
        val converted = convert(
            canvasOf(
                nodes = listOf(node("a", "text", extraFields = ""","text":"A""""), node("b", "text", y = 200, extraFields = ""","text":"B"""")),
                edges = listOf("""{"id":"e","fromNode":"a","fromSide":"bottom","fromEnd":"arrow","toNode":"b","toSide":"top","toEnd":"none","label":"comes from"}""")
            )
        )

        assertEquals(listOf(emberrEdge("id-3", "id-2", CanvasSide.TOP, "id-1", CanvasSide.BOTTOM).copy(label = "comes from")), converted.content.edges)
    }

    @Test
    fun arrowsAtBothEndsOrNeitherEndKeepTheirDirection() {
        val converted = convert(
            canvasOf(
                nodes = listOf(node("a", "text", extraFields = ""","text":"A""""), node("b", "text", y = 200, extraFields = ""","text":"B"""")),
                edges = listOf(
                    """{"id":"e1","fromNode":"a","fromEnd":"arrow","toNode":"b","toEnd":"arrow"}""",
                    """{"id":"e2","fromNode":"a","fromEnd":"none","toNode":"b","toEnd":"none"}"""
                )
            )
        )

        assertEquals(
            listOf(
                emberrEdge("id-3", "id-1", CanvasSide.BOTTOM, "id-2", CanvasSide.TOP),
                emberrEdge("id-4", "id-1", CanvasSide.BOTTOM, "id-2", CanvasSide.TOP)
            ),
            converted.content.edges
        )
    }

    @Test
    fun unknownAndBrokenNodesAreSkippedWithTheirEdges() {
        val converted = convert(
            canvasOf(
                nodes = listOf(
                    node("weird", "sticker"),
                    """{"id":"no-position","type":"text","text":"Lost"}""",
                    "42",
                    node("a", "text", extraFields = ""","text":"Kept","styleAttributes":{"shape":"pill"}""")
                ),
                edges = listOf(
                    """{"id":"e1","fromNode":"weird","toNode":"a"}""",
                    """{"id":"e2","fromNode":"a","toNode":"missing"}""",
                    """{"id":"e3","toNode":"a"}"""
                )
            )
        )

        assertEquals(listOf("Kept"), converted.content.nodes.map { it.text })
        assertTrue(converted.content.edges.isEmpty())
    }

    @Test
    fun anEmptyCanvasHasNoNodes() {
        assertTrue(convert("{}").content.isEmpty())
    }

    @Test
    fun aFileThatIsNotACanvasFails() {
        assertFails { convert("not json") }
        assertFails { convert("[]") }
        assertFails { convert("""{"nodes":5}""") }
    }
}
