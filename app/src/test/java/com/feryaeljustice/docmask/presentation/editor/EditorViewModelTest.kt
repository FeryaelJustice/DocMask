package com.feryaeljustice.docmask.presentation.editor

import android.graphics.Bitmap
import app.cash.turbine.test
import com.feryaeljustice.docmask.core.model.BoundingBox
import com.feryaeljustice.docmask.core.model.DocMaskResult
import com.feryaeljustice.docmask.core.model.DocumentSide
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.core.model.SensitiveFieldType
import com.feryaeljustice.docmask.core.model.WatermarkConfig
import com.feryaeljustice.docmask.data.local.preferences.UserPreferencesRepository
import com.feryaeljustice.docmask.domain.model.WatermarkTemplate
import com.feryaeljustice.docmask.domain.repository.BillingRepository
import com.feryaeljustice.docmask.domain.usecase.ApplyMaskingUseCase
import com.feryaeljustice.docmask.domain.usecase.DetectSensitiveRegionsUseCase
import com.feryaeljustice.docmask.domain.usecase.GetWatermarkTemplatesUseCase
import com.feryaeljustice.docmask.domain.usecase.SaveWatermarkTemplateUseCase
import com.feryaeljustice.docmask.domain.usecase.DeleteWatermarkTemplateUseCase
import com.feryaeljustice.docmask.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private val saveWatermarkTemplateUseCase: SaveWatermarkTemplateUseCase = mockk()
    private val deleteWatermarkTemplateUseCase: DeleteWatermarkTemplateUseCase = mockk()
    private val billingRepository: BillingRepository = mockk()
    private val userPreferencesRepository: UserPreferencesRepository = mockk()
    private val testBitmap: Bitmap = mockk()
    private val maskedBitmap: Bitmap = mockk()

    private val sampleTemplates = listOf(
        WatermarkTemplate(
            id = "template-1",
            title = "Alquiler vivienda",
            config = WatermarkConfig(purposeText = "Para alquiler", opacity = 0.35f),
            isDefault = true
        )
    )

    private lateinit var viewModel: EditorViewModel

    @Before
    fun setUp() {
        every { getWatermarkTemplatesUseCase() } returns flowOf(sampleTemplates)
        every { billingRepository.isProUser } returns flowOf(false)
        every { billingRepository.setDebugProStatus(any()) } returns Unit
        every { userPreferencesRepository.isBiometricEnabled } returns flowOf(false)
        coEvery { userPreferencesRepository.setBiometricEnabled(any()) } returns Unit
        coEvery { saveWatermarkTemplateUseCase(any()) } returns Unit
        coEvery { deleteWatermarkTemplateUseCase(any()) } returns Unit

        viewModel = EditorViewModel(
            detectSensitiveRegionsUseCase = detectSensitiveRegionsUseCase,
            applyMaskingUseCase = applyMaskingUseCase,
            getWatermarkTemplatesUseCase = getWatermarkTemplatesUseCase,
            saveWatermarkTemplateUseCase = saveWatermarkTemplateUseCase,
            deleteWatermarkTemplateUseCase = deleteWatermarkTemplateUseCase,
            billingRepository = billingRepository,
            userPreferencesRepository = userPreferencesRepository
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

    @Test
    fun givenBiometricToggled_whenEventSent_thenUpdatesPreferencesRepository() = runTest {
        // When
        viewModel.onEvent(EditorUiEvent.ToggleBiometric(true))

        // Then
        coVerify(exactly = 1) { userPreferencesRepository.setBiometricEnabled(true) }
    }

    @Test
    fun givenOpacityChanged_whenEventSent_thenUpdatesMaskingOptionsOpacity() = runTest {
        // When
        viewModel.onEvent(EditorUiEvent.UpdateWatermarkOpacity(0.6f))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(0.6f, state.maskingOptions.watermarkConfig.opacity, 0.001f)
        }
    }

    @Test
    fun givenSaveTemplateDispatched_whenEventSent_thenInvokesSaveUseCase() = runTest {
        val newTemplate = WatermarkTemplate(
            id = "custom-test",
            title = "Test Template",
            config = WatermarkConfig(purposeText = "Test Purpose", opacity = 0.4f),
            isDefault = false
        )

        // When
        viewModel.onEvent(EditorUiEvent.SaveTemplate(newTemplate))

        // Then
        coVerify(exactly = 1) { saveWatermarkTemplateUseCase(newTemplate) }
    }

    @Test
    fun givenDeleteTemplateDispatched_whenEventSent_thenInvokesDeleteUseCase() = runTest {
        // When
        viewModel.onEvent(EditorUiEvent.DeleteTemplate("custom-123"))

        // Then
        coVerify(exactly = 1) { deleteWatermarkTemplateUseCase("custom-123") }
    }

    @Test
    fun givenToggleDebugProDispatched_whenEventSent_thenUpdatesBillingRepo() = runTest {
        // When
        viewModel.onEvent(EditorUiEvent.ToggleDebugPro(true))

        // Then
        verify(exactly = 1) { billingRepository.setDebugProStatus(true) }
    }

    @Test
    fun givenDualSidedModeEnabled_whenBackSelected_thenActiveSideIsBack() = runTest {
        // When
        viewModel.onEvent(EditorUiEvent.ToggleDualSidedMode(true))
        viewModel.onEvent(EditorUiEvent.SelectDocumentSide(DocumentSide.BACK))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isDualSidedMode)
            assertEquals(DocumentSide.BACK, state.activeSide)
        }
    }

    @Test
    fun givenImagesLoadedOnBothSides_whenDualModeDisabled_thenPreservesFrontImage() = runTest {
        coEvery { detectSensitiveRegionsUseCase(testBitmap) } returns DocMaskResult.Success(emptyList())
        coEvery { applyMaskingUseCase(any(), any(), any()) } returns DocMaskResult.Success(maskedBitmap)

        // Given
        viewModel.onEvent(EditorUiEvent.ToggleDualSidedMode(true))
        viewModel.onEvent(EditorUiEvent.ImageSelected(testBitmap, DocumentSide.FRONT))
        viewModel.onEvent(EditorUiEvent.ImageSelected(testBitmap, DocumentSide.BACK))

        // When
        viewModel.onEvent(EditorUiEvent.ToggleDualSidedMode(false))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isDualSidedMode)
            assertEquals(DocumentSide.FRONT, state.activeSide)
            assertTrue(state.hasFront)
            assertEquals(testBitmap, state.originalBitmap)
        }
    }

    @Test
    fun givenMissingBackImage_whenInDualMode_thenCanExportIsFalse() = runTest {
        coEvery { detectSensitiveRegionsUseCase(testBitmap) } returns DocMaskResult.Success(emptyList())
        coEvery { applyMaskingUseCase(any(), any(), any()) } returns DocMaskResult.Success(maskedBitmap)

        // Given only front loaded
        viewModel.onEvent(EditorUiEvent.ToggleDualSidedMode(true))
        viewModel.onEvent(EditorUiEvent.ImageSelected(testBitmap, DocumentSide.FRONT))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.isDualSidedMode)
            assertTrue(state.hasFront)
            assertFalse(state.hasBack)
            assertFalse(state.canExport)
        }
    }
}
