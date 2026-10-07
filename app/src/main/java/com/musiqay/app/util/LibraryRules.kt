package com.musiqay.app.util

import java.text.Normalizer
import java.util.Locale

/** Legacy folder names remain valid; new choices are scoped to a full path. */
fun isHiddenFolder(path: String, name: String, hidden: Set<String>): Boolean =
    "path:$path" in hidden || path in hidden || name in hidden

private val combiningMarks = Regex("\\p{M}+")

fun normalizeSearch(value: String): String = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
    .replace(combiningMarks, "")
    .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ى', 'ي')
    .lowercase(Locale.ROOT)

/** Retain the selected occurrence when a queue contains duplicates or missing files. */
fun restoredQueueIndex(savedIds: List<Long>, availableIds: Set<Long>, savedIndex: Int, currentId: Long): Int {
    val selected = savedIndex.takeIf { it in savedIds.indices && savedIds[it] == currentId }
        ?: savedIds.indexOf(currentId).takeIf { it >= 0 }
        ?: 0
    val surviving = savedIds.indices.filter { savedIds[it] in availableIds }
    if (surviving.isEmpty()) return 0
    return surviving.indexOf(selected).takeIf { it >= 0 }
        ?: surviving.indexOfFirst { it >= selected }.takeIf { it >= 0 }
        ?: surviving.lastIndex
}
