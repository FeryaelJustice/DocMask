package com.feryaeljustice.docmask.data.repository

import android.content.Context
import com.feryaeljustice.docmask.R
import com.feryaeljustice.docmask.core.model.WatermarkConfig
import com.feryaeljustice.docmask.domain.model.WatermarkTemplate
import com.feryaeljustice.docmask.domain.repository.TemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TemplateRepositoryImpl(
    private val context: Context? = null
) : TemplateRepository {

    private fun createDefaultTemplates(): List<WatermarkTemplate> = listOf(
        WatermarkTemplate(
            id = "template-rental",
            title = context?.getString(R.string.template_rental_title) ?: "Alquiler de Vivienda",
            config = WatermarkConfig(
                purposeText = context?.getString(R.string.template_rental_purpose) ?: "Copia para alquiler de vivienda",
                includeDate = true,
                opacity = 0.35f
            ),
            isDefault = true,
            isFavorite = true
        ),
        WatermarkTemplate(
            id = "template-hotel",
            title = context?.getString(R.string.template_hotel_title) ?: "Reserva Hotelera",
            config = WatermarkConfig(
                purposeText = context?.getString(R.string.template_hotel_purpose) ?: "Copia exclusiva para reserva de hotel",
                includeDate = true,
                opacity = 0.35f
            ),
            isDefault = true,
            isFavorite = true
        ),
        WatermarkTemplate(
            id = "template-employment",
            title = context?.getString(R.string.template_employment_title) ?: "Trámite Laboral",
            config = WatermarkConfig(
                purposeText = context?.getString(R.string.template_employment_purpose) ?: "Copia para proceso de contratación",
                includeDate = true,
                opacity = 0.35f
            ),
            isDefault = true,
            isFavorite = false
        ),
        WatermarkTemplate(
            id = "template-banking",
            title = context?.getString(R.string.template_banking_title) ?: "Gestión Bancaria",
            config = WatermarkConfig(
                purposeText = context?.getString(R.string.template_banking_purpose) ?: "Copia exclusiva para trámite bancario",
                includeDate = true,
                opacity = 0.35f
            ),
            isDefault = true,
            isFavorite = false
        )
    )

    private val _templates = MutableStateFlow(createDefaultTemplates())

    override fun getTemplates(): Flow<List<WatermarkTemplate>> = _templates.asStateFlow()

    override suspend fun saveTemplate(template: WatermarkTemplate) {
        _templates.update { current ->
            val index = current.indexOfFirst { it.id == template.id }
            if (index >= 0) {
                current.toMutableList().apply { set(index, template) }
            } else {
                current + template
            }
        }
    }

    override suspend fun deleteTemplate(templateId: String) {
        _templates.update { current ->
            current.filterNot { it.id == templateId && !it.isDefault }
        }
    }

    override suspend fun toggleFavorite(templateId: String) {
        _templates.update { current ->
            current.map { template ->
                if (template.id == templateId) {
                    template.copy(isFavorite = !template.isFavorite)
                } else {
                    template
                }
            }
        }
    }
}
