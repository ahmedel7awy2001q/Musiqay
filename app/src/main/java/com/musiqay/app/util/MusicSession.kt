package com.musiqay.app.util

fun isRadioMediaId(id: String?): Boolean = id?.startsWith("radio:") == true

/** Radio never overwrites the saved local queue or its selected duplicate occurrence. */
data class MusicSession(
    val ids: List<Long> = emptyList(), val index: Int = 0, val positionMs: Long = 0,
    val shuffle: Boolean = false, val repeat: Int = 0
) {
    val currentId: Long? get() = ids.getOrNull(index)
    fun available(availableIds: Set<Long>): MusicSession {
        val found = ids.filter { it in availableIds }
        if (found.isEmpty()) return copy(ids = emptyList(), index = 0, positionMs = 0)
        val target = restoredQueueIndex(ids, availableIds, index, currentId ?: -1)
        return copy(ids = found, index = target, positionMs = if (found[target] == currentId) positionMs else 0)
    }
    fun append(id: Long, next: Boolean = false): MusicSession =
        insert(id, if (next && ids.isNotEmpty()) (index + 1).coerceIn(0, ids.size) else ids.size)
    fun insert(id: Long, at: Int, restorePositionMs: Long? = null): MusicSession {
        val target = at.coerceIn(0, ids.size)
        val selected = if (restorePositionMs != null || ids.isEmpty()) target
            else if (target <= index) index + 1 else index
        return copy(ids = ids.toMutableList().apply { add(target, id) }, index = selected,
            positionMs = restorePositionMs?.coerceAtLeast(0) ?: positionMs)
    }
}
fun radioRetryDelayMs(attempt: Int): Long? = listOf(3_000L, 6_000L, 12_000L).getOrNull(attempt)
