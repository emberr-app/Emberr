package com.emberr.domain.cloud.sync

private const val MICROSECONDS_PER_MILLISECOND = 1_000L

fun localEditIsNewerThanTombstone(localUpdatedAtMillis: Long, tombstoneUpdatedAtMicroseconds: Long): Boolean =
    localUpdatedAtMillis * MICROSECONDS_PER_MILLISECOND > tombstoneUpdatedAtMicroseconds

fun tombstoneTimeInMillis(tombstoneUpdatedAtMicroseconds: Long): Long =
    tombstoneUpdatedAtMicroseconds / MICROSECONDS_PER_MILLISECOND

fun <Item> parentsBeforeChildren(
    items: List<Item>,
    idOf: (Item) -> String,
    parentIdOf: (Item) -> String?
): List<Item> {
    val itemsById = items.associateBy(idOf)
    val ordered = LinkedHashMap<String, Item>()

    fun addAfterItsParents(item: Item, idsOnThisPath: MutableSet<String>) {
        val id = idOf(item)
        if (id in ordered || !idsOnThisPath.add(id)) return
        parentIdOf(item)?.let(itemsById::get)?.let { parent -> addAfterItsParents(parent, idsOnThisPath) }
        ordered[id] = item
    }

    items.forEach { item -> addAfterItsParents(item, mutableSetOf()) }
    return ordered.values.toList()
}
