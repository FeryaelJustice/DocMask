package com.feryaeljustice.docmask.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import androidx.core.graphics.createBitmap
import androidx.core.graphics.get
import androidx.core.graphics.scale
import androidx.core.graphics.set
import androidx.core.graphics.withRotation
import com.feryaeljustice.docmask.core.model.BoundingBox
import com.feryaeljustice.docmask.core.model.MaskingOptions
import com.feryaeljustice.docmask.core.model.RedactionArea
import kotlin.math.max
import kotlin.math.min

/**
 * Pure Android Graphics Bitmap processor for offline document masking:
 * - Local pixelation of sensitive bounding boxes
 * - Grayscale color matrix filter
 * - Semi-transparent diagonal cross-document watermark
 */
interface BitmapProcessor {
    fun processDocument(
        source: Bitmap,
        redactionAreas: List<RedactionArea>,
        options: MaskingOptions
    ): Bitmap

    fun applyPixelation(
        source: Bitmap,
        regions: List<BoundingBox>,
        blockSize: Int = 24
    ): Bitmap

    fun applyGrayscale(source: Bitmap): Bitmap

    fun applyWatermark(
        source: Bitmap,
        options: MaskingOptions
    ): Bitmap
}

class AndroidBitmapProcessor : BitmapProcessor {

    override fun processDocument(
        source: Bitmap,
        redactionAreas: List<RedactionArea>,
        options: MaskingOptions
    ): Bitmap {
        var result = source.copy(Bitmap.Config.ARGB_8888, true)

        if (options.applyPixelation) {
            val activeRegions = redactionAreas
                .filter { it.isRedacted }
                .map { it.boundingBox }
            if (activeRegions.isNotEmpty()) {
                val baseDimension = max(source.width, source.height)
                // Calculate dynamic block size according to pixelPercent (10% to 100%)
                val dynamicBlockSize = (baseDimension * (options.pixelPercent.coerceIn(10, 100) / 100f) * 0.05f)
                    .toInt()
                    .coerceAtLeast(8)
                val effectiveBlockSize = max(dynamicBlockSize, options.pixelBlockSize)
                result = applyPixelation(result, activeRegions, effectiveBlockSize)
            }
        }

        if (options.applyGrayscale) {
            result = applyGrayscale(result)
        }

        result = applyWatermark(result, options)

        return result
    }

    override fun applyPixelation(
        source: Bitmap,
        regions: List<BoundingBox>,
        blockSize: Int
    ): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val width = output.width
        val height = output.height
        val safeBlockSize = max(4, blockSize)
        val canvas = Canvas(output)
        val nearestNeighborPaint = Paint().apply {
            isFilterBitmap = false // Nearest-neighbor ensures distinct pixelated privacy mosaic
        }

        for (region in regions) {
            val left = max(0, region.left.toInt())
            val top = max(0, region.top.toInt())
            val right = min(width, region.right.toInt())
            val bottom = min(height, region.bottom.toInt())

            val regionW = right - left
            val regionH = bottom - top

            if (regionW <= 0 || regionH <= 0) continue

            // Downscale to tiny dimensions then upscale without filtering to form mosaic blocks
            val downscaledW = max(1, regionW / safeBlockSize)
            val downscaledH = max(1, regionH / safeBlockSize)

            try {
                val subBitmap = Bitmap.createBitmap(output, left, top, regionW, regionH)
                val scaledDown = subBitmap.scale(downscaledW, downscaledH, filter = false)
                val scaledUp = scaledDown.scale(regionW, regionH, filter = false)

                canvas.drawBitmap(scaledUp, left.toFloat(), top.toFloat(), nearestNeighborPaint)

                if (subBitmap != scaledDown && subBitmap != scaledUp) {
                    subBitmap.recycle()
                }
                if (scaledDown != scaledUp) {
                    scaledDown.recycle()
                }
                scaledUp.recycle()
            } catch (e: Exception) {
                // Fallback pixel by pixel if memory allocation fails for sub-bitmaps
                var y = top
                while (y < bottom) {
                    var x = left
                    val blockH = min(safeBlockSize, bottom - y)
                    while (x < right) {
                        val blockW = min(safeBlockSize, right - x)
                        val samplePixel = output[x, y]
                        for (by in 0 until blockH) {
                            for (bx in 0 until blockW) {
                                output[x + bx, y + by] = samplePixel
                            }
                        }
                        x += safeBlockSize
                    }
                    y += safeBlockSize
                }
            }
        }
        return output
    }

    override fun applyGrayscale(source: Bitmap): Bitmap {
        val output = createBitmap(source.width, source.height)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    override fun applyWatermark(
        source: Bitmap,
        options: MaskingOptions
    ): Bitmap {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val config = options.watermarkConfig
        val text = config.buildDisplayText()

        if (text.isBlank() || config.opacity <= 0f) return output

        val baseDimension = max(source.width, source.height)
        val textSize = (baseDimension * 0.035f).coerceIn(24f, 80f)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.DKGRAY
            alpha = (config.opacity.coerceIn(0f, 1f) * 255).toInt()
            this.textSize = textSize
            style = Paint.Style.FILL
            isFakeBoldText = true
        }

        canvas.withRotation(config.rotationDegrees, source.width / 2f, source.height / 2f) {
            if (config.isRepeatedGrid) {
                val textBounds = Rect()
                paint.getTextBounds(text, 0, text.length, textBounds)
                val textWidth = max(textBounds.width().toFloat(), textSize * 5f)
                val colStep = textWidth + (textSize * 3f)
                val rowStep = textSize * 4.5f

                val diagonal = kotlin.math.hypot(source.width.toDouble(), source.height.toDouble()).toFloat()
                val startX = (source.width / 2f) - diagonal
                val endX = (source.width / 2f) + diagonal
                val startY = (source.height / 2f) - diagonal
                val endY = (source.height / 2f) + diagonal

                var rowIndex = 0
                var y = startY
                while (y < endY) {
                    val xOffset = if (rowIndex % 2 == 1) colStep * 0.5f else 0f
                    var x = startX + xOffset
                    while (x < endX) {
                        drawText(text, x, y, paint)
                        x += colStep
                    }
                    y += rowStep
                    rowIndex++
                }
            } else {
                val textBounds = Rect()
                paint.getTextBounds(text, 0, text.length, textBounds)
                val textX = (source.width - textBounds.width()) / 2f
                val textY = (source.height + textBounds.height()) / 2f
                drawText(text, textX, textY, paint)
            }
        }

        return output
    }
}
