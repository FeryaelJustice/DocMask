package com.feryaeljustice.docmask.core.model

import android.graphics.Rect
import android.graphics.RectF

/**
 * Immutable representation of a rectangular region on a document image.
 * Coordinates are represented as floats, either in normalized range [0.0, 1.0]
 * or absolute pixels.
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(0f)
    val height: Float get() = (bottom - top).coerceAtLeast(0f)

    init {
        require(right >= left) { "BoundingBox right ($right) must be >= left ($left)" }
        require(bottom >= top) { "BoundingBox bottom ($bottom) must be >= top ($top)" }
    }

    fun contains(x: Float, y: Float): Boolean {
        return x in left..right && y in top..bottom
    }

    fun overlaps(other: BoundingBox): Boolean {
        return left < other.right && right > other.left &&
                top < other.bottom && bottom > other.top
    }

    fun toRect(): Rect = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

    fun toRectF(): RectF = RectF(left, top, right, bottom)
}

fun Rect.toBoundingBox(): BoundingBox = BoundingBox(
    left = left.toFloat(),
    top = top.toFloat(),
    right = right.toFloat(),
    bottom = bottom.toFloat()
)

fun RectF.toBoundingBox(): BoundingBox = BoundingBox(
    left = left,
    top = top,
    right = right,
    bottom = bottom
)
