package com.feryaeljustice.docmask.domain.usecase

import android.graphics.Bitmap
import com.feryaeljustice.docmask.core.model.BoundingBox
import com.feryaeljustice.docmask.core.model.DocMaskError
import com.feryaeljustice.docmask.core.model.DocMaskResult
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.core.model.SensitiveFieldType
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
 * Unit tests for [DetectSensitiveRegionsUseCase].
 */
class DetectSensitiveRegionsUseCaseTest {

    private val repository: DocumentProcessorRepository = mockk()
    private val bitmap: Bitmap = mockk()
    private lateinit var useCase: DetectSensitiveRegionsUseCase

    @Before
    fun setUp() {
        useCase = DetectSensitiveRegionsUseCase(repository)
    }

    @Test
    fun givenDocumentBitmap_whenDetectSensitiveRegionsInvoked_thenReturnsSuccessWithDetectedRegions() = runTest {
        // Given
        val expectedAreas = listOf(
            RedactionArea(
                type = SensitiveFieldType.SUPPORT_NUMBER,
                boundingBox = BoundingBox(10f, 10f, 100f, 50f),
                detectedText = "IDESP AAA123456",
                isRedacted = true
            ),
            RedactionArea(
                type = SensitiveFieldType.CAN_CODE,
                boundingBox = BoundingBox(120f, 10f, 200f, 50f),
                detectedText = "CAN 654321",
                isRedacted = true
            )
        )
        coEvery { repository.detectSensitiveRegions(bitmap) } returns DocMaskResult.Success(expectedAreas)

        // When
        val result = useCase(bitmap)

        // Then
        assertTrue(result is DocMaskResult.Success)
        val data = (result as DocMaskResult.Success).data
        assertEquals(2, data.size)
        assertEquals(SensitiveFieldType.SUPPORT_NUMBER, data[0].type)
        assertEquals(SensitiveFieldType.CAN_CODE, data[1].type)
        coVerify(exactly = 1) { repository.detectSensitiveRegions(bitmap) }
    }

    @Test
    fun givenRepositoryFailure_whenDetectSensitiveRegionsInvoked_thenReturnsFailureResult() = runTest {
        // Given
        val expectedError = DocMaskError.DetectionFailed("Local ML Kit error")
        coEvery { repository.detectSensitiveRegions(bitmap) } returns DocMaskResult.Failure(expectedError)

        // When
        val result = useCase(bitmap)

        // Then
        assertTrue(result is DocMaskResult.Failure)
        val error = (result as DocMaskResult.Failure).error
        assertEquals("Local ML Kit error", error.message)
        coVerify(exactly = 1) { repository.detectSensitiveRegions(bitmap) }
    }
}
