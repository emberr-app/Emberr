package com.emberr.domain.util.export

import com.emberr.data.local.room.entity.CanvasNodeEntity
import com.emberr.data.local.room.entity.CanvasNodeType
import com.emberr.domain.canvas.CanvasContent
import com.emberr.domain.canvas.isGroup
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

private const val EMBERR_TO_OBSIDIAN_SCALE = 1.25f
private const val TEXT_NODE = "text"
private const val FILE_NODE = "file"
private const val GROUP_NODE = "group"

private val obsidianColorsByEmberrColorName = mapOf(
    "red" to "1",
    "orange" to "2",
    "yellow" to "3",
    "green" to "4",
    "blue" to "5",
    "purple" to "6",
    "lime" to "#DCE775",
    "pink" to "#F8BBD0",
    "grey" to "#D9D9D9"
)

@Serializable
private data class ObsidianCanvasFile(
    val nodes: List<ObsidianCanvasNode>,
    val edges: List<ObsidianCanvasEdge>
)

@Serializable
private data class ObsidianCanvasNode(
    val id: String,
    val type: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val color: String? = null,
    val text: String? = null,
    val file: String? = null,
    val label: String? = null
)

@Serializable
private data class ObsidianCanvasEdge(
    val id: String,
    val fromNode: String,
    val fromSide: String,
    val toNode: String,
    val toSide: String,
    val label: String? = null
)

object ObsidianCanvasWriter {

    private val json = Json { prettyPrint = true }

    fun write(canvas: CanvasContent, attachmentPathsByStoredName: Map<String, String>): String {
        val live = canvas.liveOnly()
        val nodes = live.nodes
            .sortedBy { node -> !node.isGroup }
            .mapNotNull { node -> obsidianNodeOf(node, attachmentPathsByStoredName) }
        val writtenNodeIds = nodes.mapTo(HashSet()) { it.id }
        val edges = live.edges
            .filter { edge -> edge.fromNodeId in writtenNodeIds && edge.toNodeId in writtenNodeIds }
            .map { edge ->
                ObsidianCanvasEdge(
                    id = edge.edgeId,
                    fromNode = edge.fromNodeId,
                    fromSide = edge.fromSide.name.lowercase(),
                    toNode = edge.toNodeId,
                    toSide = edge.toSide.name.lowercase(),
                    label = edge.label
                )
            }
        return json.encodeToString(ObsidianCanvasFile.serializer(), ObsidianCanvasFile(nodes, edges))
    }

    private fun obsidianNodeOf(node: CanvasNodeEntity, attachmentPathsByStoredName: Map<String, String>): ObsidianCanvasNode? {
        val placedNode = ObsidianCanvasNode(
            id = node.nodeId,
            type = TEXT_NODE,
            x = scaled(node.x),
            y = scaled(node.y),
            width = scaled(node.width),
            height = scaled(node.height),
            color = node.color?.let(obsidianColorsByEmberrColorName::get)
        )
        return when (node.type) {
            CanvasNodeType.TEXT, CanvasNodeType.FREE_TEXT -> placedNode.copy(text = node.text)
            CanvasNodeType.GROUP -> placedNode.copy(type = GROUP_NODE, label = node.text.takeIf { it.isNotBlank() })
            CanvasNodeType.IMAGE -> {
                val imagePath = MarkdownAttachments.storedFileNameOf(node.imagePath)?.let(attachmentPathsByStoredName::get) ?: return null
                placedNode.copy(type = FILE_NODE, file = imagePath)
            }
        }
    }

    private fun scaled(value: Float): Int = (value * EMBERR_TO_OBSIDIAN_SCALE).roundToInt()
}
