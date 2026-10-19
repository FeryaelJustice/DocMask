package com.feryaeljustice.docmask.presentation.editor

import android.graphics.Bitmap
import com.feryaeljustice.docmask.core.model.DocumentSide
import com.feryaeljustice.docmask.core.model.MaskingOptions
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.core.model.WatermarkConfig
import com.feryaeljustice.docmask.domain.model.WatermarkTemplate

/**
 * Immutable UI State for the Document Editor Screen.
 */
data class EditorUiState(
    val isLoading: Boolean = false,
    val isDualSidedMode: Boolean = false,
    val activeSide: DocumentSide = DocumentSide.FRONT,
    // Front side (and primary document in single-sided mode)
    val originalBitmap: Bitmap? = null,
    val maskedBitmap: Bitmap? = null,
    val detectedRegions: List<RedactionArea> = emptyList(),
    // Back side (for Pro dual-sided DNI mode)
    val backOriginalBitmap: Bitmap? = null,
    val backMaskedBitmap: Bitmap? = null,
    val backDetectedRegions: List<RedactionArea> = emptyList(),
    val maskingOptions: MaskingOptions = MaskingOptions(),
    val availableTemplates: List<WatermarkTemplate> = emptyList(),
    val selectedTemplateId: String? = null,
    val isProUser: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val errorMessage: String? = null,
    val isExportSuccess: Boolean = false
) {
    val currentOriginalBitmap: Bitmap? get() = if (activeSide == DocumentSide.FRONT) originalBitmap else backOriginalBitmap
    val currentMaskedBitmap: Bitmap? get() = if (activeSide == DocumentSide.FRONT) maskedBitmap else backMaskedBitmap
    val currentDetectedRegions: List<RedactionArea> get() = if (activeSide == DocumentSide.FRONT) detectedRegions else backDetectedRegions
    val hasDocument: Boolean get() = currentOriginalBitmap != null
    val hasFront: Boolean get() = originalBitmap != null
    val hasBack: Boolean get() = backOriginalBitmap != null
    val canExport: Boolean get() = if (isDualSidedMode) hasFront && hasBack else hasFront
    val redactedCount: Int get() = currentDetectedRegions.count { it.isRedacted }
    val defaultTemplates: List<WatermarkTemplate> get() = availableTemplates.filter { it.isDefault }
    val customTemplates: List<WatermarkTemplate> get() = availableTemplates.filter { !it.isDefault }
}

sealed interface EditorUiEvent {
    data class ImageSelected(val bitmap: Bitmap, val targetSide: DocumentSide? = null) : EditorUiEvent
    data class ToggleDualSidedMode(val enabled: Boolean) : EditorUiEvent
    data class SelectDocumentSide(val side: DocumentSide) : EditorUiEvent
    data class ToggleRegionRedaction(val regionId: String) : EditorUiEvent
    data class RedactAllSensitive(val redact: Boolean) : EditorUiEvent
    data class ToggleGrayscale(val enabled: Boolean) : EditorUiEvent
    data class UpdateWatermarkText(val text: String) : EditorUiEvent
    data class UpdateWatermarkOpacity(val opacity: Float) : EditorUiEvent
    data class SelectTemplate(val template: WatermarkTemplate) : EditorUiEvent
    data class SaveTemplate(val template: WatermarkTemplate) : EditorUiEvent
    data class DeleteTemplate(val templateId: String) : EditorUiEvent
    data class UpdateWatermarkConfig(val config: WatermarkConfig) : EditorUiEvent
    data class ToggleBiometric(val enabled: Boolean) : EditorUiEvent
    data class ToggleDebugPro(val enabled: Boolean) : EditorUiEvent
    object ExportDocument : EditorUiEvent
    object DismissError : EditorUiEvent
}
