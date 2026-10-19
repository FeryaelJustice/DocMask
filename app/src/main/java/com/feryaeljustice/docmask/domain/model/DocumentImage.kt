package com.feryaeljustice.docmask.domain.model

import android.graphics.Bitmap
import com.feryaeljustice.docmask.core.model.RedactionArea

/**
 * Domain entity representing an identity document being edited.
 */
data class DocumentImage(
    val id: String,
    val originalBitmap: Bitmap,
    val previewBitmap: Bitmap,
    val detectedRegions: List<RedactionArea> = emptyList(),
    val fileName: String? = null
)
