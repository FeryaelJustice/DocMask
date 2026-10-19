package com.feryaeljustice.docmask.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for [WatermarkConfig] formatting and date stamping.
 */
class WatermarkConfigTest {

    @Test
    fun givenWatermarkConfigWithDate_whenBuildDisplayTextCalled_thenFormatsTextWithCurrentDate() {
        // Given
        val config = WatermarkConfig(
            purposeText = "Copia para alquiler de piso",
            includeDate = true
        )
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 19)
        }

        // When
        val formattedText = config.buildDisplayText(calendar.time)

        // Then
        assertTrue(formattedText.startsWith("Copia para alquiler de piso - "))
        assertTrue(formattedText.contains("19/10/2026"))
    }

    @Test
    fun givenWatermarkConfigWithoutDate_whenBuildDisplayTextCalled_thenReturnsPurposeOnly() {
        // Given
        val config = WatermarkConfig(
            purposeText = "Solo para trámite laboral",
            includeDate = false
        )

        // When
        val formattedText = config.buildDisplayText()

        // Then
        assertEquals("Solo para trámite laboral", formattedText)
    }
}
