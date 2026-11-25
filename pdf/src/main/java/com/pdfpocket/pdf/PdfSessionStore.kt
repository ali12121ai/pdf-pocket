package com.pdfpocket.pdf

import java.io.File

/** Cache files scoped to one disposable tool session. StateFlow only holds paths, never PDF bytes. */
object PdfSessionStore {
    fun write(cacheDir: File, session: String, name: String, bytes: ByteArray): String {
        val dir = File(cacheDir, "pdf-sessions/$session")
        dir.mkdirs()
        val safe = name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifEmpty { "output.pdf" }
        val file = File(dir, safe)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    fun read(path: String?): ByteArray? = path?.let { File(it).takeIf { f -> f.isFile }?.readBytes() }

    fun clear(cacheDir: File, session: String) {
        File(cacheDir, "pdf-sessions/$session").deleteRecursively()
    }
}
