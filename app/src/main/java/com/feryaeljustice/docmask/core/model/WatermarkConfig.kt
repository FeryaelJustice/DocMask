package com.feryaeljustice.docmask.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Configuration for the cross-document watermark per AEPD recommendations.
 */
data class WatermarkConfig(
    val purposeText: String = "Copia exclusiva para trámite",
    val includeDate: Boolean = true,
    val opacity: Float = 0.35f,
    val rotationDegrees: Float = -35f,
    val isRepeatedGrid: Boolean = true
) {
    /**
     * Formats the final watermark banner text combining purpose and optional date stamp.
     */
    fun buildDisplayText(date: Date = Date()): String {
        return if (includeDate) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            "$purposeText - ${dateFormat.format(date)}"
        } else {
            purposeText
        }
    }
}
