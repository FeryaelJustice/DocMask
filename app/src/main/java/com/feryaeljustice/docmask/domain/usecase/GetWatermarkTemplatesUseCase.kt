package com.feryaeljustice.docmask.domain.usecase

import com.feryaeljustice.docmask.domain.model.WatermarkTemplate
import com.feryaeljustice.docmask.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe the list of user-defined watermark templates.
 */
class GetWatermarkTemplatesUseCase(
    private val repository: TemplateRepository
) {
    operator fun invoke(): Flow<List<WatermarkTemplate>> {
        return repository.getTemplates()
    }
}
