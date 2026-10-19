package com.feryaeljustice.docmask.core.util

import android.content.Context
import java.io.File
import java.util.Locale

object CacheManager {

    fun getExportedDocumentsDir(context: Context): File {
        return File(context.cacheDir, "exported_documents")
    }

    fun getTemporaryStorageStats(context: Context): Pair<Int, Long> {
        val dir = getExportedDocumentsDir(context)
        if (!dir.exists() || !dir.isDirectory) {
            return Pair(0, 0L)
        }

        var count = 0
        var totalBytes = 0L

        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                count++
                totalBytes += file.length()
            }
        }

        return Pair(count, totalBytes)
    }

    fun clearTemporaryStorage(context: Context): Pair<Int, Long> {
        val (count, bytes) = getTemporaryStorageStats(context)
        val dir = getExportedDocumentsDir(context)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        return Pair(count, bytes)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        return when {
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
