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
import com.feryaeljustice.docmask.domain.repository.DocumentQuotaRepository
import com.feryaeljustice.docmask.domain.repository.StoragePreferencesRepository
import com.feryaeljustice.docmask.domain.usecase.ApplyAepdPresetUseCase
import com.feryaeljustice.docmask.domain.usecase.ApplyMaskingUseCase
import com.feryaeljustice.docmask.domain.usecase.CheckAppPermissionsUseCase
import com.feryaeljustice.docmask.domain.usecase.DeleteWatermarkTemplateUseCase
import com.feryaeljustice.docmask.domain.usecase.DetectSensitiveRegionsUseCase
import com.feryaeljustice.docmask.domain.usecase.GetWatermarkTemplatesUseCase
import com.feryaeljustice.docmask.domain.usecase.SaveWatermarkTemplateUseCase
import com.feryaeljustice.docmask.domain.usecase.ToggleFavoriteTemplateUseCase
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
    private val toggleFavoriteTemplateUseCase: ToggleFavoriteTemplateUseCase = mockk()
    private val billingRepository: BillingRepository = mockk()
    private val userPreferencesRepository: UserPreferencesRepository = mockk()
    private val documentQuotaRepository: DocumentQuotaRepository = mockk()
    private val storagePreferencesRepository: StoragePreferencesRepository = mockk()
    private val applyAepdPresetUseCase: ApplyAepdPresetUseCase = mockk()
    private val checkAppPermissionsUseCase: CheckAppPermissionsUseCase = mockk()
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
        every { documentQuotaRepository.dailyExportCount } returns flowOf(0)
        every { storagePreferencesRepository.customStorageUri } returns flowOf(null)
        coEvery { storagePreferencesRepository.setCustomStorageUri(any()) } returns Unit
        coEvery { documentQuotaRepository.getMaxDailyLimit(any()) } returns 3
        coEvery { documentQuotaRepository.canExportToday(any()) } returns true
        coEvery { documentQuotaRepository.recordExport() } returns Unit
        coEvery { userPreferencesRepository.setBiometricEnabled(any()) } returns Unit
        coEvery { saveWatermarkTemplateUseCase(any()) } returns Unit
        coEvery { deleteWatermarkTemplateUseCase(any()) } returns Unit
        coEvery { toggleFavoriteTemplateUseCase(any()) } returns Unit
        every { applyAepdPresetUseCase(any()) } answers { firstArg() }
        every { checkAppPermissionsUseCase(any()) } returns emptyList()

        viewModel = EditorViewModel(
            detectSensitiveRegionsUseCase = detectSensitiveRegionsUseCase,
            applyMaskingUseCase = applyMaskingUseCase,
            getWatermarkTemplatesUseCase = getWatermarkTemplatesUseCase,
            saveWatermarkTemplateUseCase = saveWatermarkTemplateUseCase,
            deleteWatermarkTemplateUseCase = deleteWatermarkTemplateUseCase,
            toggleFavoriteTemplateUseCase = toggleFavoriteTemplateUseCase,
            billingRepository = billingRepository,
            userPreferencesRepository = userPreferencesRepository,
            documentQuotaRepository = documentQuotaRepository,
            storagePreferencesRepository = storagePreferencesRepository,
            applyAepdPresetUseCase = applyAepdPresetUseCase,
            checkAppPermissionsUseCase = checkAppPermissionsUseCase,
            defaultDispatcher = mainDispatcherRule.testDispatcher,
            ioDispatcher = mainDispatcherRule.testDispatcher
        )
    }

    @Test
    fun givenInitialState_whenCreated_thenObservesTemplatesAndDefaultWatermarkConfig() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.availableTemplates.size)
            assertEquals("template-1", state.selectedTemplateId)
            assertEquals("Para alquiler", state.maskingOptions.watermarkConfig.purposeText)
            assertEquals(InputMode.SINGLE_PHOTO, state.inputMode)
            assertFalse(state.isProUser)
            assertFalse(state.hasDocument)
        }
    }

    @Test
    fun givenSegmentedSelection_whenPdfModeChosen_thenResetsDualSidedMode() = runTest {
        viewModel.onEvent(EditorUiEvent.ToggleDualSidedMode(true))
        viewModel.onEvent(EditorUiEvent.SelectInputMode(InputMode.PDF_DOCUMENT))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(InputMode.PDF_DOCUMENT, state.inputMode)
            assertFalse(state.isDualSidedMode)
            assertTrue(state.isPdfMode)
        }
    }

    @Test
    fun givenSinglePhotoMode_whenDniDualModeChosen_thenActivatesDualSidedMode() = runTest {
        viewModel.onEvent(EditorUiEvent.SelectInputMode(InputMode.DNI_DUAL))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(InputMode.DNI_DUAL, state.inputMode)
            assertTrue(state.isDualSidedMode)
            assertEquals(DocumentSide.FRONT, state.activeSide)
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
    fun givenPixelIntensityChanged_whenEventSent_thenUpdatesPixelPercent() = runTest {
        viewModel.onEvent(EditorUiEvent.UpdatePixelIntensity(75))

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(75, state.maskingOptions.pixelPercent)
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

    @Test
    fun givenTemplateId_whenToggleFavoriteEventSent_thenInvokesUseCase() = runTest {
        viewModel.onEvent(EditorUiEvent.ToggleFavoriteTemplate("template-1"))
        coVerify(exactly = 1) { toggleFavoriteTemplateUseCase("template-1") }
    }

    @Test
    fun givenSaveManualRegionsDispatched_whenEventSent_thenCombinesAiAndManualRegions() = runTest {
        coEvery { detectSensitiveRegionsUseCase(testBitmap) } returns DocMaskResult.Success(emptyList())
        coEvery { applyMaskingUseCase(any(), any(), any()) } returns DocMaskResult.Success(maskedBitmap)

        viewModel.onEvent(EditorUiEvent.ImageSelected(testBitmap, DocumentSide.FRONT))

        val manualArea = RedactionArea(
            id = "manual-1",
            type = SensitiveFieldType.CUSTOM_SELECTION,
            boundingBox = BoundingBox(0.1f, 0.1f, 0.5f, 0.5f),
            isRedacted = true
        )

        viewModel.onEvent(EditorUiEvent.SaveManualRegions(DocumentSide.FRONT, listOf(manualArea)))

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.detectedRegions.any { it.id == "manual-1" })
        }
    }

    @Test
    fun givenGeneratePdfToggled_whenUserIsNotPro_thenSetsErrorMessage() = runTest {
        viewModel.onEvent(EditorUiEvent.ToggleGeneratePdf(true))

        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.generatePdfForDni)
            assertTrue(state.errorMessage?.contains("DocMask Pro") == true)
        }
    }

    @Test
    fun givenRestorePurchasesDispatched_whenEventSent_thenInvokesBillingRepository() = runTest {
        coEvery { billingRepository.restorePurchases() } returns Result.success(true)

        viewModel.onEvent(EditorUiEvent.RestorePurchases)

        coVerify(exactly = 1) { billingRepository.restorePurchases() }
    }

    @Test
    fun givenCustomStorageUriUpdated_whenInvoked_thenCallsStoragePreferencesRepository() = runTest {
        viewModel.updateCustomStorageUri("content://tree/custom")
        coVerify(exactly = 1) { storagePreferencesRepository.setCustomStorageUri("content://tree/custom") }
    }
}
