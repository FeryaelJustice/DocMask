package com.feryaeljustice.docmask.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Professional unit test suite for [BoundingBox] domain model.
 * Follows Given-When-Then descriptive naming conventions.
 */
class BoundingBoxTest {

    @Test
    fun givenValidCoordinates_whenBoundingBoxInstantiated_thenCalculatesWidthAndHeightCorrectly() {
        // Given
        val left = 10f
        val top = 20f
        val right = 110f
        val bottom = 120f

        // When
        val box = BoundingBox(left, top, right, bottom)

        // Then
        assertEquals(100f, box.width, 0.001f)
        assertEquals(100f, box.height, 0.001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun givenRightCoordinateSmallerThanLeft_whenInstantiated_thenThrowsIllegalArgumentException() {
        // Given & When & Then
        BoundingBox(left = 100f, top = 10f, right = 50f, bottom = 60f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun givenBottomCoordinateSmallerThanTop_whenInstantiated_thenThrowsIllegalArgumentException() {
        // Given & When & Then
        BoundingBox(left = 10f, top = 100f, right = 50f, bottom = 50f)
    }

    @Test
    fun givenPointInsideBox_whenContainsCalled_thenReturnsTrue() {
        // Given
        val box = BoundingBox(left = 0f, top = 0f, right = 100f, bottom = 100f)

        // When
        val isInside = box.contains(50f, 50f)

        // Then
        assertTrue(isInside)
    }

    @Test
    fun givenPointOutsideBox_whenContainsCalled_thenReturnsFalse() {
        // Given
        val box = BoundingBox(left = 0f, top = 0f, right = 100f, bottom = 100f)

        // When
        val isInside = box.contains(150f, 50f)

        // Then
        assertFalse(isInside)
    }

    @Test
    fun givenOverlappingBoxes_whenOverlapsEvaluated_thenReturnsTrue() {
        // Given
        val boxA = BoundingBox(left = 0f, top = 0f, right = 50f, bottom = 50f)
        val boxB = BoundingBox(left = 25f, top = 25f, right = 75f, bottom = 75f)

        // When & Then
        assertTrue(boxA.overlaps(boxB))
        assertTrue(boxB.overlaps(boxA))
    }

    @Test
    fun givenDisjointBoxes_whenOverlapsEvaluated_thenReturnsFalse() {
        // Given
        val boxA = BoundingBox(left = 0f, top = 0f, right = 20f, bottom = 20f)
        val boxB = BoundingBox(left = 50f, top = 50f, right = 80f, bottom = 80f)

        // When & Then
        assertFalse(boxA.overlaps(boxB))
        assertFalse(boxB.overlaps(boxA))
    }
}
