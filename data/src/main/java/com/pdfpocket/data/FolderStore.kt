package com.pdfpocket.data

import android.content.Context
import android.net.Uri

/** Persisted SAF tree grants. No storage permission is requested. */
object FolderStore {
    private const val PREFS = "pdf_pocket_folders"
    private const val KEY = "tree_uris"

    fun add(context: Context, uri: Uri) {
        val current = list(context).map { it.toString() }.toMutableSet()
        current.add(uri.toString())
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, current).apply()
    }

    fun list(context: Context): List<Uri> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY, emptySet())!!
            .mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }

    fun remove(context: Context, uri: Uri) {
        val current = list(context).filter { it != uri }.map { it.toString() }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putStringSet(KEY, current.toSet()).apply()
    }
}
