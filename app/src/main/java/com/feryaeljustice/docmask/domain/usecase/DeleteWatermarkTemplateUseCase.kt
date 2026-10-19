package com.feryaeljustice.docmask.domain.usecase

import com.feryaeljustice.docmask.domain.repository.TemplateRepository

/**
 * Use case to delete a watermark template by its unique ID.
 */
class DeleteWatermarkTemplateUseCase(
    private val repository: TemplateRepository
) {
    suspend operator fun invoke(templateId: String) {
        repository.deleteTemplate(templateId)
    }
}
