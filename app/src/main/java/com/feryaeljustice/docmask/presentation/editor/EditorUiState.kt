package com.feryaeljustice.docmask.presentation.editor

import android.graphics.Bitmap
import com.feryaeljustice.docmask.core.model.MaskingOptions
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.core.model.WatermarkConfig
import com.feryaeljustice.docmask.domain.model.WatermarkTemplate

/**
 * Immutable UI State for the Document Editor Screen.
 */
data class EditorUiState(
    val isLoading: Boolean = false,
    val originalBitmap: Bitmap? = null,
    val maskedBitmap: Bitmap? = null,
    val detectedRegions: List<RedactionArea> = emptyList(),
    val maskingOptions: MaskingOptions = MaskingOptions(),
    val availableTemplates: List<WatermarkTemplate> = emptyList(),
    val selectedTemplateId: String? = null,
    val isProUser: Boolean = false,
    val errorMessage: String? = null,
    val isExportSuccess: Boolean = false
) {
    val hasDocument: Boolean get() = originalBitmap != null
    val redactedCount: Int get() = detectedRegions.count { it.isRedacted }
}

sealed interface EditorUiEvent {
    data class ImageSelected(val bitmap: Bitmap) : EditorUiEvent
    data class ToggleRegionRedaction(val regionId: String) : EditorUiEvent
    data class RedactAllSensitive(val redact: Boolean) : EditorUiEvent
    data class ToggleGrayscale(val enabled: Boolean) : EditorUiEvent
    data class UpdateWatermarkText(val text: String) : EditorUiEvent
    data class SelectTemplate(val template: WatermarkTemplate) : EditorUiEvent
    data class UpdateWatermarkConfig(val config: WatermarkConfig) : EditorUiEvent
    object ExportDocument : EditorUiEvent
    object DismissError : EditorUiEvent
}
