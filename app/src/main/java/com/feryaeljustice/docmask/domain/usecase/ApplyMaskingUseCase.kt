package com.feryaeljustice.docmask.domain.usecase

import android.graphics.Bitmap
import com.feryaeljustice.docmask.core.model.DocMaskResult
import com.feryaeljustice.docmask.core.model.MaskingOptions
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.domain.repository.DocumentProcessorRepository

/**
 * Use case to apply pixelation, optional grayscale, and watermark banners to a document.
 */
class ApplyMaskingUseCase(
    private val repository: DocumentProcessorRepository
) {
    suspend operator fun invoke(
        source: Bitmap,
        areas: List<RedactionArea>,
        options: MaskingOptions
    ): DocMaskResult<Bitmap> {
        return repository.applyMasking(source, areas, options)
    }
}
