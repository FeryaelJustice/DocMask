package com.feryaeljustice.docmask.core.model

/**
 * Options applied to an identity document during export.
 */
data class MaskingOptions(
    val applyGrayscale: Boolean = true,
    val applyPixelation: Boolean = true,
    val pixelBlockSize: Int = 24,
    val watermarkConfig: WatermarkConfig = WatermarkConfig()
)
