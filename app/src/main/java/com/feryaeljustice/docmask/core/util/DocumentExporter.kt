package com.feryaeljustice.docmask.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentExporter {

    fun exportAndShareBitmap(
        context: Context,
        bitmap: Bitmap,
        title: String = "docmask_protegido"
    ): Result<Uri> {
        return try {
            val exportDir = File(context.cacheDir, "exported_documents").apply {
                if (!exists()) mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "${title}_$timestamp.png")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Documento protegido con DocMask")
                putExtra(Intent.EXTRA_TEXT, "Adjunto documento de identidad protegido con DocMask (100% Offline).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Compartir documento protegido").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportAndShareBitmapsBatch(
        context: Context,
        frontBitmap: Bitmap,
        backBitmap: Bitmap,
        title: String = "docmask_dni"
    ): Result<List<Uri>> {
        return try {
            val exportDir = File(context.cacheDir, "exported_documents").apply {
                if (!exists()) mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val frontFile = File(exportDir, "${title}_anverso_$timestamp.png")
            val backFile = File(exportDir, "${title}_reverso_$timestamp.png")

            FileOutputStream(frontFile).use { out ->
                frontBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileOutputStream(backFile).use { out ->
                backBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val authority = "${context.packageName}.fileprovider"
            val frontUri = FileProvider.getUriForFile(context, authority, frontFile)
            val backUri = FileProvider.getUriForFile(context, authority, backFile)
            val uriList = arrayListOf(frontUri, backUri)

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/png"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                putExtra(Intent.EXTRA_SUBJECT, "Documento de identidad protegido con DocMask")
                putExtra(Intent.EXTRA_TEXT, "Adjunto anverso y reverso protegidos con DocMask (100% Offline).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Compartir DNI protegido (Anverso y Reverso)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            Result.success(uriList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
