package com.example.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ScreenshotStorage {
    private const val DIR_NAME = "trade_screenshots"

    fun saveFromUri(context: Context, uri: Uri): String? {
        return runCatching {
            val dir = File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }
            val outFile = File(dir, "chart_${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            outFile.absolutePath
        }.getOrNull()
    }

    fun deleteIfOwned(context: Context, path: String) {
        runCatching {
            val dir = File(context.filesDir, DIR_NAME).canonicalFile
            val target = File(path).canonicalFile
            if (target.parentFile == dir && target.exists()) {
                target.delete()
            }
        }
    }

    fun clearAll(context: Context) {
        runCatching {
            val dir = File(context.filesDir, DIR_NAME)
            if (dir.exists()) {
                dir.deleteRecursively()
            }
        }
    }
}
