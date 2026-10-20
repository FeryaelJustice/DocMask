package com.feryaeljustice.docmask.core.model

import java.util.UUID

/**
 * An individual sensitive area on the document marked for redaction / pixelation.
 */
data class RedactionArea(
    val id: String = UUID.randomUUID().toString(),
    val type: SensitiveFieldType,
    val boundingBox: BoundingBox,
    val detectedText: String? = null,
    val isRedacted: Boolean = type.recommendedToRedact,
    val pageIndex: Int = 1
)
