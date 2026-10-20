package com.feryaeljustice.docmask.data.repository

import com.feryaeljustice.docmask.core.model.WatermarkConfig
import com.feryaeljustice.docmask.domain.model.WatermarkTemplate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TemplateRepositoryTest {

    private lateinit var repository: TemplateRepositoryImpl

    @Before
    fun setUp() {
        repository = TemplateRepositoryImpl()
    }

    @Test
    fun givenInitialState_thenReturnsDefaultTemplates() = runTest {
        val templates = repository.getTemplates().first()
        assertTrue(templates.isNotEmpty())
        assertEquals(4, templates.count { it.isDefault })
    }

    @Test
    fun givenDefaultTemplate_whenDeleteAttempted_thenIsNotDeleted() = runTest {
        val initialTemplates = repository.getTemplates().first()
        val defaultTemplate = initialTemplates.first { it.isDefault }

        repository.deleteTemplate(defaultTemplate.id)

        val updatedTemplates = repository.getTemplates().first()
        assertTrue(updatedTemplates.any { it.id == defaultTemplate.id })
    }

    @Test
    fun givenCustomTemplate_whenDeleteAttempted_thenIsRemoved() = runTest {
        val customTemplate = WatermarkTemplate(
            id = "custom-123",
            title = "Mi plantilla personalizada",
            config = WatermarkConfig(purposeText = "Trámite de prueba"),
            isDefault = false
        )

        repository.saveTemplate(customTemplate)
        val afterSave = repository.getTemplates().first()
        assertTrue(afterSave.any { it.id == "custom-123" })

        repository.deleteTemplate("custom-123")
        val afterDelete = repository.getTemplates().first()
        assertFalse(afterDelete.any { it.id == "custom-123" })
    }

    @Test
    fun givenExistingTemplate_whenSaveCalled_thenUpdatesInPlace() = runTest {
        val initialTemplates = repository.getTemplates().first()
        val templateToEdit = initialTemplates.first()

        val updated = templateToEdit.copy(
            title = "Título Modificado",
            config = templateToEdit.config.copy(purposeText = "Nuevo propósito", opacity = 0.5f)
        )

        repository.saveTemplate(updated)

        val resultTemplates = repository.getTemplates().first()
        val found = resultTemplates.first { it.id == templateToEdit.id }
        assertEquals("Título Modificado", found.title)
        assertEquals("Nuevo propósito", found.config.purposeText)
        assertEquals(0.5f, found.config.opacity, 0.001f)
    }

    @Test
    fun givenTemplate_whenToggleFavoriteCalled_thenFavoriteStateInverts() = runTest {
        val initialTemplates = repository.getTemplates().first()
        val template = initialTemplates.first()
        val initialFavorite = template.isFavorite

        repository.toggleFavorite(template.id)

        val updatedTemplates = repository.getTemplates().first()
        val found = updatedTemplates.first { it.id == template.id }
        assertEquals(!initialFavorite, found.isFavorite)

        repository.toggleFavorite(template.id)

        val revertedTemplates = repository.getTemplates().first()
        val revertedFound = revertedTemplates.first { it.id == template.id }
        assertEquals(initialFavorite, revertedFound.isFavorite)
    }
}
