package com.emberr.domain.util.export

import com.emberr.data.local.room.entity.CanvasEdgeEntity
import com.emberr.data.local.room.entity.CanvasNodeEntity
import com.emberr.data.local.room.entity.CanvasNodeType
import com.emberr.data.local.room.entity.CanvasSide
import com.emberr.data.local.room.entity.CanvasStrokeEntity
import com.emberr.domain.canvas.CanvasContent
import com.emberr.domain.importer.ImportLinkResolver
import com.emberr.domain.importer.canvas.ObsidianCanvasConverter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObsidianCanvasWriterTest {

    private fun node(id: String, x: Float, y: Float, width: Float, height: Float, text: String = "", type: CanvasNodeType = CanvasNodeType.TEXT) =
        CanvasNodeEntity(nodeId = id, noteId = "canvas", x = x, y = y, width = width, height = height, text = text, createdAt = 1L, updatedAt = 1L, type = type)

    private val imagesAsAttachments = object : ImportLinkResolver {
        override fun noteIdFor(linkTarget: String): String? = null
        override fun attachmentPathFor(linkTarget: String): String? = linkTarget
    }

    @Test
    fun aCanvasReadBackByTheObsidianImporterKeepsCardsGroupsImagesAndArrowsButNotDrawings() {
        val canvas = CanvasContent(
            nodes = listOf(
                node("card", x = 40f, y = 40f, width = 200f, height = 120f, text = "Plan").copy(color = "lime"),
                node("group", x = 40f, y = 200f, width = 400f, height = 240f, text = "Ideas", type = CanvasNodeType.GROUP),
                node("image", x = 80f, y = 240f, width = 120f, height = 80f, type = CanvasNodeType.IMAGE).copy(imagePath = "media_a.png"),
                node("gone", x = 400f, y = 40f, width = 120f, height = 80f, text = "Deleted").copy(isDeleted = true)
            ),
            edges = listOf(
                CanvasEdgeEntity(
                    edgeId = "edge",
                    noteId = "canvas",
                    fromNodeId = "card",
                    fromSide = CanvasSide.BOTTOM,
                    toNodeId = "image",
                    toSide = CanvasSide.TOP,
                    createdAt = 1L,
                    updatedAt = 1L,
                    label = "shows"
                )
            ),
            strokes = listOf(CanvasStrokeEntity(strokeId = "stroke", noteId = "canvas", x = 0f, y = 0f, points = "", width = 2f, createdAt = 1L, updatedAt = 1L))
        )

        val canvasJson = ObsidianCanvasWriter.write(canvas, mapOf("media_a.png" to "attachments/media_a.png"))
        val readBack = ObsidianCanvasConverter.convert(canvasJson, imagesAsAttachments, noteTitlesById = emptyMap(), now = 1L)
        val nodesByType = readBack.content.nodes.groupBy { it.type }

        val card = nodesByType.getValue(CanvasNodeType.TEXT).single()
        assertEquals(listOf("Plan", "lime"), listOf(card.text, card.color))
        assertEquals(listOf(40f, 40f, 200f, 120f), listOf(card.x, card.y, card.width, card.height))
        val group = nodesByType.getValue(CanvasNodeType.GROUP).single()
        assertEquals(listOf(40f, 200f, 400f, 240f), listOf(group.x, group.y, group.width, group.height))
        assertEquals("Ideas", group.text)
        val image = nodesByType.getValue(CanvasNodeType.IMAGE).single()
        assertEquals(listOf("attachments/media_a.png"), readBack.pendingImages.map { it.relativePath })

        val edge = readBack.content.edges.single()
        assertEquals(listOf(card.nodeId, image.nodeId, "shows"), listOf(edge.fromNodeId, edge.toNodeId, edge.label))
        assertEquals(CanvasSide.BOTTOM to CanvasSide.TOP, edge.fromSide to edge.toSide)
        assertTrue(readBack.content.strokes.isEmpty())
    }

    @Test
    fun canvasFilesAreNamedAfterTheCanvasOrCanvasAndNeverClash() {
        val canvases = listOf("", "Debug canvas", "").mapIndexed { index, title ->
            ExportedCanvas(canvasNoteId = "canvas-$index", title = title, content = CanvasContent())
        }
        val attachments = listOf(MarkdownAttachment(storedFileName = "media_x.png", pathInExport = "attachments/media_x.png"))

        assertEquals(
            listOf("attachments/Canvas.canvas", "attachments/Debug_canvas.canvas", "attachments/Canvas_2.canvas"),
            MarkdownAttachments.canvasFilesFor(canvases, attachments).map { it.pathInExport }
        )
    }
}
