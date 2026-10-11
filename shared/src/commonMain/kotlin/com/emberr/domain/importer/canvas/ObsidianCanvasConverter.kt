package com.emberr.domain.importer.canvas

import com.emberr.data.local.room.entity.CanvasEdgeEntity
import com.emberr.data.local.room.entity.CanvasNodeEntity
import com.emberr.data.local.room.entity.CanvasNodeType
import com.emberr.data.local.room.entity.CanvasSide
import com.emberr.domain.canvas.CanvasContent
import com.emberr.domain.canvas.cleanedEdgeLabel
import com.emberr.domain.importer.ImportLinkResolver
import com.emberr.domain.importer.ImportPaths
import com.emberr.domain.importer.newImportId
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.math.abs
import kotlin.math.max

private const val TEXT_NODE = "text"
private const val FILE_NODE = "file"
private const val LINK_NODE = "link"
private const val GROUP_NODE = "group"
private const val ARROW_END = "arrow"
private const val NO_END = "none"
private const val MINIMUM_NODE_WIDTH = 80f
private const val MINIMUM_NODE_HEIGHT = 40f
private const val OBSIDIAN_TO_EMBERR_SCALE = 0.8
private const val CONTENT_MARGIN = 40f
private const val SUBPATH_SEPARATOR = " › "
private const val BLOCK_REFERENCE_MARKER = '^'

internal data class PendingCanvasImage(val nodeId: String, val relativePath: String)

internal data class ConvertedCanvas(
    val content: CanvasContent,
    val pendingImages: List<PendingCanvasImage>
)

@Serializable
private class ObsidianCanvasFile(
    val nodes: List<JsonElement> = emptyList(),
    val edges: List<JsonElement> = emptyList()
)

@Serializable
private class ObsidianCanvasNode(
    val id: String,
    val type: String,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val color: String? = null,
    val text: String? = null,
    val file: String? = null,
    val subpath: String? = null,
    val url: String? = null,
    val label: String? = null
)

@Serializable
private class ObsidianCanvasEdge(
    val fromNode: String,
    val toNode: String,
    val fromSide: String? = null,
    val toSide: String? = null,
    val fromEnd: String? = null,
    val toEnd: String? = null,
    val label: String? = null
)

internal object ObsidianCanvasConverter {

    private val json = Json { ignoreUnknownKeys = true }

    fun convert(
        canvasJson: String,
        linkResolver: ImportLinkResolver,
        noteTitlesById: Map<String, String>,
        now: Long,
        newId: () -> String = ::newImportId
    ): ConvertedCanvas {
        val canvasFile = json.decodeFromString(ObsidianCanvasFile.serializer(), canvasJson)
        val pendingImages = mutableListOf<PendingCanvasImage>()
        val emberrNodeIdsByObsidianId = mutableMapOf<String, String>()

        val placedNodes = canvasFile.nodes.mapNotNull { element ->
            val obsidianNode = decodeOrNull(ObsidianCanvasNode.serializer(), element) ?: return@mapNotNull null
            val nodeId = newId()
            val emptyNode = CanvasNodeEntity(
                nodeId = nodeId,
                noteId = "",
                x = (obsidianNode.x * OBSIDIAN_TO_EMBERR_SCALE).toFloat(),
                y = (obsidianNode.y * OBSIDIAN_TO_EMBERR_SCALE).toFloat(),
                width = max((obsidianNode.width * OBSIDIAN_TO_EMBERR_SCALE).toFloat(), MINIMUM_NODE_WIDTH),
                height = max((obsidianNode.height * OBSIDIAN_TO_EMBERR_SCALE).toFloat(), MINIMUM_NODE_HEIGHT),
                text = "",
                createdAt = now,
                updatedAt = now,
                color = CanvasColorMatcher.colorNameFor(obsidianNode.color)
            )
            val node = when (obsidianNode.type) {
                TEXT_NODE -> emptyNode.copy(text = CanvasCardText.fromMarkdown(obsidianNode.text.orEmpty(), now))
                LINK_NODE -> emptyNode.copy(text = obsidianNode.url.orEmpty())
                GROUP_NODE -> emptyNode.copy(text = obsidianNode.label.orEmpty(), type = CanvasNodeType.GROUP)
                FILE_NODE -> {
                    val filePath = obsidianNode.file.orEmpty()
                    val imagePath = linkResolver.attachmentPathFor(filePath)?.takeIf(ImportPaths::isImageFile)
                    if (imagePath != null) {
                        pendingImages += PendingCanvasImage(nodeId = nodeId, relativePath = imagePath)
                        emptyNode.copy(type = CanvasNodeType.IMAGE)
                    } else {
                        emptyNode.copy(text = fileCardText(filePath, obsidianNode.subpath, linkResolver, noteTitlesById))
                    }
                }
                else -> return@mapNotNull null
            }
            emberrNodeIdsByObsidianId[obsidianNode.id] = nodeId
            node
        }

        val nodes = shiftedIntoView(placedNodes)
        val nodesById = nodes.associateBy { it.nodeId }
        val edges = canvasFile.edges.mapNotNull { element ->
            val obsidianEdge = decodeOrNull(ObsidianCanvasEdge.serializer(), element) ?: return@mapNotNull null
            val fromNode = emberrNodeIdsByObsidianId[obsidianEdge.fromNode]?.let(nodesById::get) ?: return@mapNotNull null
            val toNode = emberrNodeIdsByObsidianId[obsidianEdge.toNode]?.let(nodesById::get) ?: return@mapNotNull null
            edgeBetween(fromNode, toNode, obsidianEdge, now, newId)
        }

        return ConvertedCanvas(content = CanvasContent(nodes = nodes, edges = edges), pendingImages = pendingImages)
    }

    private fun <T> decodeOrNull(deserializer: DeserializationStrategy<T>, element: JsonElement): T? {
        if (element !is JsonObject) return null
        return try {
            json.decodeFromJsonElement(deserializer, element)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun fileCardText(
        filePath: String,
        subpath: String?,
        linkResolver: ImportLinkResolver,
        noteTitlesById: Map<String, String>
    ): String {
        val fileName = filePath.substringAfterLast('/')
        val noteId = linkResolver.noteIdFor(filePath) ?: return fileName
        val noteTitle = noteTitlesById[noteId] ?: fileName.substringBeforeLast('.')
        val heading = subpath?.removePrefix("#")?.trim()?.takeIf { it.isNotEmpty() && it.first() != BLOCK_REFERENCE_MARKER }
        return if (heading == null) noteTitle else noteTitle + SUBPATH_SEPARATOR + heading
    }

    private fun shiftedIntoView(nodes: List<CanvasNodeEntity>): List<CanvasNodeEntity> {
        if (nodes.isEmpty()) return nodes
        val shiftX = CONTENT_MARGIN - nodes.minOf { it.x }
        val shiftY = CONTENT_MARGIN - nodes.minOf { it.y }
        return nodes.map { node -> node.copy(x = node.x + shiftX, y = node.y + shiftY) }
    }

    private fun edgeBetween(
        fromNode: CanvasNodeEntity,
        toNode: CanvasNodeEntity,
        obsidianEdge: ObsidianCanvasEdge,
        now: Long,
        newId: () -> String
    ): CanvasEdgeEntity {
        val (defaultFromSide, defaultToSide) = facingSides(fromNode, toNode)
        val fromSide = sideNamed(obsidianEdge.fromSide) ?: defaultFromSide
        val toSide = sideNamed(obsidianEdge.toSide) ?: defaultToSide
        val arrowPointsBackwards = (obsidianEdge.fromEnd ?: NO_END) == ARROW_END && (obsidianEdge.toEnd ?: ARROW_END) != ARROW_END
        return CanvasEdgeEntity(
            edgeId = newId(),
            noteId = "",
            fromNodeId = if (arrowPointsBackwards) toNode.nodeId else fromNode.nodeId,
            fromSide = if (arrowPointsBackwards) toSide else fromSide,
            toNodeId = if (arrowPointsBackwards) fromNode.nodeId else toNode.nodeId,
            toSide = if (arrowPointsBackwards) fromSide else toSide,
            createdAt = now,
            updatedAt = now,
            label = cleanedEdgeLabel(obsidianEdge.label)
        )
    }

    private fun facingSides(fromNode: CanvasNodeEntity, toNode: CanvasNodeEntity): Pair<CanvasSide, CanvasSide> {
        val distanceX = (toNode.x + toNode.width / 2f) - (fromNode.x + fromNode.width / 2f)
        val distanceY = (toNode.y + toNode.height / 2f) - (fromNode.y + fromNode.height / 2f)
        return when {
            abs(distanceX) >= abs(distanceY) && distanceX >= 0f -> CanvasSide.RIGHT to CanvasSide.LEFT
            abs(distanceX) >= abs(distanceY) -> CanvasSide.LEFT to CanvasSide.RIGHT
            distanceY >= 0f -> CanvasSide.BOTTOM to CanvasSide.TOP
            else -> CanvasSide.TOP to CanvasSide.BOTTOM
        }
    }

    private fun sideNamed(name: String?): CanvasSide? =
        CanvasSide.entries.firstOrNull { side -> side.name.equals(name?.trim(), ignoreCase = true) }
}
