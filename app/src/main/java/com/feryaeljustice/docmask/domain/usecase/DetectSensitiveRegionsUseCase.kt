package com.feryaeljustice.docmask.domain.usecase

import android.graphics.Bitmap
import com.feryaeljustice.docmask.core.model.DocMaskResult
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.domain.repository.DocumentProcessorRepository

/**
 * Use case to analyze document images and automatically detect sensitive identity regions
 * (Support number, CAN code, signature, face) using on-device ML Kit.
 */
class DetectSensitiveRegionsUseCase(
    private val repository: DocumentProcessorRepository
) {
    suspend operator fun invoke(bitmap: Bitmap): DocMaskResult<List<RedactionArea>> {
        return repository.detectSensitiveRegions(bitmap)
    }
}
