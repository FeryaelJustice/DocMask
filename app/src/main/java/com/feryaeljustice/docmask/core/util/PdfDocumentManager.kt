package com.feryaeljustice.docmask.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * 100% Offline PDF utility manager using native Android PdfRenderer and PdfDocument.
 */
object PdfDocumentManager {

    /**
     * Renders pages of a given PDF URI into in-memory Bitmaps for local ML Kit OCR processing.
     */
    fun renderPdfPagesToBitmaps(
        context: Context,
        pdfUri: Uri,
        maxPages: Int = 15
    ): Result<List<Bitmap>> {
        return try {
            val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(pdfUri, "r")
                ?: return Result.failure(IllegalStateException("No se pudo abrir el descriptor del archivo PDF."))

            val bitmaps = mutableListOf<Bitmap>()
            pfd.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    val pageCount = renderer.pageCount.coerceAtMost(maxPages)
                    for (i in 0 until pageCount) {
                        renderer.openPage(i).use { page ->
                            val width = page.width
                            val height = page.height
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bitmaps.add(bitmap)
                        }
                    }
                }
            }
            Result.success(bitmaps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compiles a list of processed bitmaps into a single PDF written to an output stream (e.g. SAF DocumentFile).
     */
    fun createPdfFromBitmapsToStream(
        bitmaps: List<Bitmap>,
        outputStream: OutputStream
    ): Result<Unit> {
        return try {
            val document = PdfDocument()
            try {
                bitmaps.forEachIndexed { index, bitmap ->
                    val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                    val page = document.startPage(pageInfo)
                    page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    document.finishPage(page)
                }
                document.writeTo(outputStream)
            } finally {
                document.close()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compiles a list of processed bitmaps into a local cache PDF file.
     */
    fun createPdfFromBitmaps(
        bitmaps: List<Bitmap>,
        outputFile: File
    ): Result<File> {
        return try {
            FileOutputStream(outputFile).use { out ->
                createPdfFromBitmapsToStream(bitmaps, out).getOrThrow()
            }
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
