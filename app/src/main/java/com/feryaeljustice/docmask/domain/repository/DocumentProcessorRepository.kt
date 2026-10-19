package com.feryaeljustice.docmask.domain.repository

import android.graphics.Bitmap
import com.feryaeljustice.docmask.core.model.DocMaskResult
import com.feryaeljustice.docmask.core.model.MaskingOptions
import com.feryaeljustice.docmask.core.model.RedactionArea

interface DocumentProcessorRepository {
    suspend fun detectSensitiveRegions(bitmap: Bitmap): DocMaskResult<List<RedactionArea>>
    suspend fun applyMasking(
        source: Bitmap,
        areas: List<RedactionArea>,
        options: MaskingOptions
    ): DocMaskResult<Bitmap>
}
