package com.feryaeljustice.docmask.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
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
                result = applyPixelation(result, activeRegions, options.pixelBlockSize)
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

        for (region in regions) {
            val left = max(0, region.left.toInt())
            val top = max(0, region.top.toInt())
            val right = min(width, region.right.toInt())
            val bottom = min(height, region.bottom.toInt())

            if (right <= left || bottom <= top) continue

            var y = top
            while (y < bottom) {
                var x = left
                val blockH = min(safeBlockSize, bottom - y)
                while (x < right) {
                    val blockW = min(safeBlockSize, right - x)

                    var redSum = 0L
                    var greenSum = 0L
                    var blueSum = 0L
                    var count = 0

                    for (by in 0 until blockH) {
                        for (bx in 0 until blockW) {
                            val pixel = output.getPixel(x + bx, y + by)
                            redSum += Color.red(pixel)
                            greenSum += Color.green(pixel)
                            blueSum += Color.blue(pixel)
                            count++
                        }
                    }

                    if (count > 0) {
                        val avgColor = Color.rgb(
                            (redSum / count).toInt(),
                            (greenSum / count).toInt(),
                            (blueSum / count).toInt()
                        )

                        for (by in 0 until blockH) {
                            for (bx in 0 until blockW) {
                                output.setPixel(x + bx, y + by, avgColor)
                            }
                        }
                    }

                    x += safeBlockSize
                }
                y += safeBlockSize
            }
        }
        return output
    }

    override fun applyGrayscale(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
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

        canvas.save()
        canvas.rotate(config.rotationDegrees, source.width / 2f, source.height / 2f)

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
                    canvas.drawText(text, x, y, paint)
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
            canvas.drawText(text, textX, textY, paint)
        }

        canvas.restore()
        return output
    }
}
