package com.feryaeljustice.docmask.domain.usecase

import com.feryaeljustice.docmask.domain.repository.TemplateRepository

class ToggleFavoriteTemplateUseCase(
    private val repository: TemplateRepository
) {
    suspend operator fun invoke(templateId: String) {
        repository.toggleFavorite(templateId)
    }
}
