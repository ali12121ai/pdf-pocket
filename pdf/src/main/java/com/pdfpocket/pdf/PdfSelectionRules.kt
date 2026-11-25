package com.pdfpocket.pdf

/** Selection defaults and guards from FLOWS3. Empty means literally no pages, never "all". */
object PdfSelectionRules {
    val pageTools = setOf(
        ToolKey.EXTRACT, ToolKey.DELETE, ToolKey.ORGANISE,
        ToolKey.CROP, ToolKey.NUMBER, ToolKey.WATERMARK, ToolKey.TO_IMAGES,
    )
    val wholeDocumentTools = setOf(ToolKey.COMPRESS, ToolKey.UNLOCK, ToolKey.PROTECT)

    fun defaultSelection(tool: ToolKey, pageCount: Int): Set<Int> =
        if (tool in pageTools && tool != ToolKey.EXTRACT && tool != ToolKey.DELETE) {
            (0 until pageCount).toSet()
        } else {
            emptySet()
        }

    fun canApply(tool: ToolKey, selected: Set<Int>, pageCount: Int): Boolean = when (tool) {
        ToolKey.EXTRACT, ToolKey.CROP, ToolKey.NUMBER, ToolKey.WATERMARK, ToolKey.TO_IMAGES ->
            selected.isNotEmpty() && selected.all { it in 0 until pageCount }
        ToolKey.DELETE ->
            selected.isNotEmpty() && selected.size < pageCount && selected.all { it in 0 until pageCount }
        ToolKey.ORGANISE -> selected.isNotEmpty() && selected.all { it in 0 until pageCount }
        ToolKey.MERGE -> false
        else -> true
    }

    /** Parse a 1-based `2-5, 8` expression into 0-based indices. Invalid tokens are ignored. */
    fun parsePages(spec: String, pageCount: Int): Set<Int> {
        val result = mutableSetOf<Int>()
        spec.split(',', ' ', ';').map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
            if (token.contains('-')) {
                val parts = token.split('-', limit = 2)
                val from = parts[0].toIntOrNull() ?: return@forEach
                val to = parts[1].toIntOrNull() ?: return@forEach
                val start = maxOf(1, minOf(from, to))
                val end = minOf(pageCount, maxOf(from, to))
                if (start <= end) result.addAll((start - 1) until end)
            } else {
                val page = token.toIntOrNull() ?: return@forEach
                if (page in 1..pageCount) result.add(page - 1)
            }
        }
        return result
    }
}
