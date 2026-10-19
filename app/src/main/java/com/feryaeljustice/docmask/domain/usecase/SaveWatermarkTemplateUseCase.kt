package com.feryaeljustice.docmask.domain.usecase

import com.feryaeljustice.docmask.domain.model.WatermarkTemplate
import com.feryaeljustice.docmask.domain.repository.TemplateRepository

/**
 * Use case to create or update a user-defined watermark template.
 */
class SaveWatermarkTemplateUseCase(
    private val repository: TemplateRepository
) {
    suspend operator fun invoke(template: WatermarkTemplate) {
        repository.saveTemplate(template)
    }
}
