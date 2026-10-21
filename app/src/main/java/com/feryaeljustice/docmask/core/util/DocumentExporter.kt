package com.feryaeljustice.docmask.core.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.feryaeljustice.docmask.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DocumentExporter {

    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }

    /**
     * Saves a stream of content to the user-chosen SAF custom directory if present.
     */
    private fun saveToSafOrFallback(
        context: Context,
        customStorageUriString: String?,
        fileName: String,
        mimeType: String,
        writeBlock: (OutputStream) -> Unit
    ): Uri? {
        if (!customStorageUriString.isNullOrBlank()) {
            try {
                val treeUri = customStorageUriString.toUri()
                val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                if (pickedDir != null && pickedDir.canWrite()) {
                    val newFile = pickedDir.createFile(mimeType, fileName)
                    if (newFile != null) {
                        context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                            writeBlock(out)
                        }
                        return newFile.uri
                    }
                }
            } catch (e: Exception) {
                // Fallback to internal storage or proceed without failing export
            }
        }
        return null
    }

    fun exportAndShareBitmap(
        context: Context,
        bitmap: Bitmap,
        title: String = "docmask_protegido",
        customStorageUri: String? = null
    ): Result<Uri> {
        return try {
            val exportDir = File(context.cacheDir, "exported_documents").apply {
                if (!exists()) mkdirs()
            }

            val timestamp = getTimestamp()
            val fileName = "${title}_$timestamp.png"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Dual storage: Also write to SAF folder if configured
            saveToSafOrFallback(context, customStorageUri, fileName, "image/png") { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject_single))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_body_single))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_chooser_single)).apply {
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
        title: String = "docmask_dni",
        customStorageUri: String? = null
    ): Result<List<Uri>> {
        return try {
            val exportDir = File(context.cacheDir, "exported_documents").apply {
                if (!exists()) mkdirs()
            }

            val timestamp = getTimestamp()
            val frontFileName = "${title}_anverso_$timestamp.png"
            val backFileName = "${title}_reverso_$timestamp.png"
            val frontFile = File(exportDir, frontFileName)
            val backFile = File(exportDir, backFileName)

            FileOutputStream(frontFile).use { out ->
                frontBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileOutputStream(backFile).use { out ->
                backBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Dual storage: Also write both to SAF folder if configured
            saveToSafOrFallback(context, customStorageUri, frontFileName, "image/png") { out ->
                frontBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            saveToSafOrFallback(context, customStorageUri, backFileName, "image/png") { out ->
                backBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val authority = "${context.packageName}.fileprovider"
            val frontUri = FileProvider.getUriForFile(context, authority, frontFile)
            val backUri = FileProvider.getUriForFile(context, authority, backFile)
            val uriList = arrayListOf(frontUri, backUri)

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/png"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject_dual_images))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_body_dual_images))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_chooser_dual_images)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            Result.success(uriList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportAndShareSinglePdf(
        context: Context,
        bitmap: Bitmap,
        title: String = "docmask_documento",
        customStorageUri: String? = null
    ): Result<Uri> {
        return try {
            val exportDir = File(context.cacheDir, "exported_documents").apply {
                if (!exists()) mkdirs()
            }

            val timestamp = getTimestamp()
            val fileName = "${title}_$timestamp.pdf"
            val pdfFile = File(exportDir, fileName)

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            // Also write to SAF folder if configured
            saveToSafOrFallback(context, customStorageUri, fileName, "application/pdf") { out ->
                val doc = PdfDocument()
                val p = doc.startPage(pageInfo)
                p.canvas.drawBitmap(bitmap, 0f, 0f, null)
                doc.finishPage(p)
                doc.writeTo(out)
                doc.close()
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject_single_pdf))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_body_single_pdf))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_chooser_single_pdf)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportAndSharePdfBatch(
        context: Context,
        frontBitmap: Bitmap,
        backBitmap: Bitmap,
        title: String = "docmask_dni_completo",
        customStorageUri: String? = null
    ): Result<Uri> {
        return try {
            val exportDir = File(context.cacheDir, "exported_documents").apply {
                if (!exists()) mkdirs()
            }

            val timestamp = getTimestamp()
            val fileName = "${title}_$timestamp.pdf"
            val pdfFile = File(exportDir, fileName)

            val pdfDocument = PdfDocument()

            // Page 1: Front
            val pageInfo1 = PdfDocument.PageInfo.Builder(frontBitmap.width, frontBitmap.height, 1).create()
            val page1 = pdfDocument.startPage(pageInfo1)
            page1.canvas.drawBitmap(frontBitmap, 0f, 0f, null)
            pdfDocument.finishPage(page1)

            // Page 2: Back
            val pageInfo2 = PdfDocument.PageInfo.Builder(backBitmap.width, backBitmap.height, 2).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            page2.canvas.drawBitmap(backBitmap, 0f, 0f, null)
            pdfDocument.finishPage(page2)

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            // Dual storage: Also write to SAF folder if configured
            saveToSafOrFallback(context, customStorageUri, fileName, "application/pdf") { out ->
                val doc = PdfDocument()
                val p1 = doc.startPage(pageInfo1)
                p1.canvas.drawBitmap(frontBitmap, 0f, 0f, null)
                doc.finishPage(p1)
                val p2 = doc.startPage(pageInfo2)
                p2.canvas.drawBitmap(backBitmap, 0f, 0f, null)
                doc.finishPage(p2)
                doc.writeTo(out)
                doc.close()
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject_dual_pdf))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_body_dual_pdf))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, context.getString(R.string.share_chooser_dual_pdf)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generates a protected multi-page PDF document and writes it directly to the target storage.
     */
    fun savePdfToStorage(
        context: Context,
        bitmaps: List<Bitmap>,
        title: String = "docmask_documento",
        customStorageUri: String? = null
    ): Result<Uri> {
        return try {
            val exportDir = File(context.cacheDir, "exported_documents").apply {
                if (!exists()) mkdirs()
            }
            val timestamp = getTimestamp()
            val fileName = "${title}_$timestamp.pdf"
            val cacheFile = File(exportDir, fileName)

            PdfDocumentManager.createPdfFromBitmaps(bitmaps, cacheFile).getOrThrow()

            // Save to SAF destination if configured
            val safUri = saveToSafOrFallback(context, customStorageUri, fileName, "application/pdf") { out ->
                PdfDocumentManager.createPdfFromBitmapsToStream(bitmaps, out)
            }

            val authority = "${context.packageName}.fileprovider"
            val cacheUri = FileProvider.getUriForFile(context, authority, cacheFile)
            Result.success(safUri ?: cacheUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}