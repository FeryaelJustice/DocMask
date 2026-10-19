package com.feryaeljustice.docmask.presentation.editor

import android.graphics.Bitmap
import app.cash.turbine.test
import com.feryaeljustice.docmask.core.model.BoundingBox
import com.feryaeljustice.docmask.core.model.DocMaskResult
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.core.model.SensitiveFieldType
import com.feryaeljustice.docmask.core.model.WatermarkConfig
import com.feryaeljustice.docmask.domain.model.WatermarkTemplate
import com.feryaeljustice.docmask.domain.repository.BillingRepository
import com.feryaeljustice.docmask.domain.usecase.ApplyMaskingUseCase
import com.feryaeljustice.docmask.domain.usecase.DetectSensitiveRegionsUseCase
import com.feryaeljustice.docmask.domain.usecase.GetWatermarkTemplatesUseCase
import com.feryaeljustice.docmask.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Professional ViewModel tests verifying UDF (Unidirectional Data Flow) state transitions
 * with Turbine and MockK.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val detectSensitiveRegionsUseCase: DetectSensitiveRegionsUseCase = mockk()
    private val applyMaskingUseCase: ApplyMaskingUseCase = mockk()
    private val getWatermarkTemplatesUseCase: GetWatermarkTemplatesUseCase = mockk()
    private val billingRepository: BillingRepository = mockk()
    private val testBitmap: Bitmap = mockk()
    private val maskedBitmap: Bitmap = mockk()

    private val sampleTemplates = listOf(
        WatermarkTemplate(
            id = "template-1",
            title = "Alquiler vivienda",
            config = WatermarkConfig(purposeText = "Para alquiler"),
            isDefault = true
        )
    )

    private lateinit var viewModel: EditorViewModel

    @Before
    fun setUp() {
        every { getWatermarkTemplatesUseCase() } returns flowOf(sampleTemplates)
        every { billingRepository.isProUser } returns flowOf(false)

        viewModel = EditorViewModel(
            detectSensitiveRegionsUseCase = detectSensitiveRegionsUseCase,
            applyMaskingUseCase = applyMaskingUseCase,
            getWatermarkTemplatesUseCase = getWatermarkTemplatesUseCase,
            billingRepository = billingRepository
        )
    }

    @Test
    fun givenInitialState_whenCreated_thenObservesTemplatesAndDefaultWatermarkConfig() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.availableTemplates.size)
            assertEquals("template-1", state.selectedTemplateId)
            assertEquals("Para alquiler", state.maskingOptions.watermarkConfig.purposeText)
            assertFalse(state.isProUser)
            assertFalse(state.hasDocument)
        }
    }

    @Test
    fun givenValidBitmap_whenImageSelectedEventSent_thenTransitionsToLoadedWithDetectedRegions() = runTest {
        // Given
        val detectedAreas = listOf(
            RedactionArea(
                id = "area-1",
                type = SensitiveFieldType.SUPPORT_NUMBER,
                boundingBox = BoundingBox(10f, 10f, 100f, 50f),
                isRedacted = true
            )
        )
        coEvery { detectSensitiveRegionsUseCase(testBitmap) } returns DocMaskResult.Success(detectedAreas)
        coEvery { applyMaskingUseCase(testBitmap, detectedAreas, any()) } returns DocMaskResult.Success(maskedBitmap)

        // When
        viewModel.onEvent(EditorUiEvent.ImageSelected(testBitmap))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.hasDocument)
            assertEquals(1, state.detectedRegions.size)
            assertEquals(maskedBitmap, state.maskedBitmap)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun givenLoadedDocument_whenToggleRegionDispatched_thenFlipsRedactionState() = runTest {
        // Given
        val detectedAreas = listOf(
            RedactionArea(
                id = "area-1",
                type = SensitiveFieldType.SUPPORT_NUMBER,
                boundingBox = BoundingBox(10f, 10f, 100f, 50f),
                isRedacted = true
            )
        )
        coEvery { detectSensitiveRegionsUseCase(testBitmap) } returns DocMaskResult.Success(detectedAreas)
        coEvery { applyMaskingUseCase(testBitmap, any(), any()) } returns DocMaskResult.Success(maskedBitmap)
        viewModel.onEvent(EditorUiEvent.ImageSelected(testBitmap))

        // When
        viewModel.onEvent(EditorUiEvent.ToggleRegionRedaction("area-1"))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            val region = state.detectedRegions.first { it.id == "area-1" }
            assertFalse(region.isRedacted)
        }
    }

    @Test
    fun givenGrayscaleToggled_whenEventSent_thenUpdatesMaskingOptions() = runTest {
        // Given
        coEvery { applyMaskingUseCase(any(), any(), any()) } returns DocMaskResult.Success(maskedBitmap)

        // When
        viewModel.onEvent(EditorUiEvent.ToggleGrayscale(false))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.maskingOptions.applyGrayscale)
        }
    }

    @Test
    fun givenExportDispatched_whenEventSent_thenFlagsExportSuccess() = runTest {
        // When
        viewModel.onEvent(EditorUiEvent.ExportDocument)

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isExportSuccess)
        }
    }
}
