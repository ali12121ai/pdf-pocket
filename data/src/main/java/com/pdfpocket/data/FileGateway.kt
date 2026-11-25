package com.pdfpocket.data

import android.content.Context
import android.content.pm.PackageInfo
import android.net.Uri
import java.io.File

/** All file movement goes through SAF URIs; no broad storage permission is requested. */
object FileGateway {
    private const val CACHE_FOLDER = "working"
    private const val MAX_CACHE_AGE_MS = 24L * 60 * 60 * 1000

    fun workingDir(context: Context): File {
        val dir = File(context.cacheDir, CACHE_FOLDER)
        dir.mkdirs()
        val cutoff = System.currentTimeMillis() - MAX_CACHE_AGE_MS
        dir.listFiles()?.filter { it.lastModified() < cutoff }?.forEach { it.delete() }
        return dir
    }

    fun writeToUri(context: Context, bytes: ByteArray, target: Uri): Boolean = try {
        context.contentResolver.openOutputStream(target)?.use { out ->
            out.write(bytes)
            out.flush()
        } != null
    } catch (e: Exception) {
        Analytics.log("export_failed", success = false)
        false
    }

    fun requestedPermissions(context: Context): Array<String> {
        val info: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return info.requestedPermissions ?: emptyArray()
    }
}
