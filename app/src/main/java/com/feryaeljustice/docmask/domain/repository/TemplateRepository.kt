package com.feryaeljustice.docmask.domain.repository

import com.feryaeljustice.docmask.domain.model.WatermarkTemplate
import kotlinx.coroutines.flow.Flow

interface TemplateRepository {
    fun getTemplates(): Flow<List<WatermarkTemplate>>
    suspend fun saveTemplate(template: WatermarkTemplate)
    suspend fun deleteTemplate(templateId: String)
}
