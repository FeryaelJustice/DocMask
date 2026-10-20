package com.feryaeljustice.docmask.domain.model

import com.feryaeljustice.docmask.core.model.WatermarkConfig
import java.util.UUID

/**
 * Reusable user watermark template (e.g. "Alquiler vivienda", "Check-in hotel", "Trámite laboral").
 */
data class WatermarkTemplate(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val config: WatermarkConfig,
    val isDefault: Boolean = false,
    val isFavorite: Boolean = false
)
