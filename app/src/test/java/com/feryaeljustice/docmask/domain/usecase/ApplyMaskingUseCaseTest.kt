package com.feryaeljustice.docmask.domain.usecase

import android.graphics.Bitmap
import com.feryaeljustice.docmask.core.model.DocMaskError
import com.feryaeljustice.docmask.core.model.DocMaskResult
import com.feryaeljustice.docmask.core.model.MaskingOptions
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.domain.repository.DocumentProcessorRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [ApplyMaskingUseCase].
 */
class ApplyMaskingUseCaseTest {

    private val repository: DocumentProcessorRepository = mockk()
    private val sourceBitmap: Bitmap = mockk()
    private val maskedBitmap: Bitmap = mockk()
    private lateinit var useCase: ApplyMaskingUseCase

    @Before
    fun setUp() {
        useCase = ApplyMaskingUseCase(repository)
    }

    @Test
    fun givenValidBitmapAndAreas_whenApplyMaskingInvoked_thenReturnsSuccessWithMaskedBitmap() = runTest {
        // Given
        val areas = emptyList<RedactionArea>()
        val options = MaskingOptions(applyGrayscale = true)
        coEvery { repository.applyMasking(sourceBitmap, areas, options) } returns DocMaskResult.Success(maskedBitmap)

        // When
        val result = useCase(sourceBitmap, areas, options)

        // Then
        assertTrue(result is DocMaskResult.Success)
        val data = (result as DocMaskResult.Success).data
        assertEquals(maskedBitmap, data)
        coVerify(exactly = 1) { repository.applyMasking(sourceBitmap, areas, options) }
    }

    @Test
    fun givenProcessingError_whenApplyMaskingInvoked_thenReturnsFailureResult() = runTest {
        // Given
        val areas = emptyList<RedactionArea>()
        val options = MaskingOptions()
        val error = DocMaskError.ImageProcessingFailed("Out of memory on canvas")
        coEvery { repository.applyMasking(sourceBitmap, areas, options) } returns DocMaskResult.Failure(error)

        // When
        val result = useCase(sourceBitmap, areas, options)

        // Then
        assertTrue(result is DocMaskResult.Failure)
        val actualError = (result as DocMaskResult.Failure).error
        assertEquals("Out of memory on canvas", actualError.message)
    }
}
