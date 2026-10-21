package com.feryaeljustice.docmask.presentation.editor

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import com.feryaeljustice.docmask.core.model.DocumentSide
import com.feryaeljustice.docmask.core.model.MaskingOptions
import com.feryaeljustice.docmask.core.model.PermissionItemState
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.core.model.WatermarkConfig
import com.feryaeljustice.docmask.domain.model.WatermarkTemplate

enum class InputMode {
    SINGLE_PHOTO,
    DNI_DUAL,
    PDF_DOCUMENT
}

/**
 * Immutable UI State for the Document Editor Screen.
 */
data class EditorUiState(
    val isLoading: Boolean = false,
    val loadingMessage: String? = null,
    val isExporting: Boolean = false,
    val inputMode: InputMode = InputMode.SINGLE_PHOTO,
    val isDualSidedMode: Boolean = false,
    val activeSide: DocumentSide = DocumentSide.FRONT,
    // Front side (and primary document in single-sided mode)
    val originalBitmap: Bitmap? = null,
    val maskedBitmap: Bitmap? = null,
    val detectedRegions: List<RedactionArea> = emptyList(),
    // Back side (for dual-sided DNI mode)
    val backOriginalBitmap: Bitmap? = null,
    val backMaskedBitmap: Bitmap? = null,
    val backDetectedRegions: List<RedactionArea> = emptyList(),
    // PDF document mode state
    val pdfDocumentName: String? = null,
    val pdfPageCount: Int = 0,
    val pdfPagesBitmaps: List<Bitmap> = emptyList(),
    val pdfMaskedPagesBitmaps: List<Bitmap> = emptyList(),
    val pdfDetectedRegions: List<RedactionArea> = emptyList(),
    // Options & Settings
    val maskingOptions: MaskingOptions = MaskingOptions(),
    val availableTemplates: List<WatermarkTemplate> = emptyList(),
    val selectedTemplateId: String? = null,
    val isProUser: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isEditingRegions: Boolean = false,
    val generatePdfForSingleSide: Boolean = false,
    val generatePdfForDni: Boolean = false,
    val isMaskingPreviewLoading: Boolean = false,
    val dailyExportsUsed: Int = 0,
    val dailyExportsLimit: Int = 3,
    val customStorageUri: String? = null,
    val permissions: List<PermissionItemState> = emptyList(),
    val quotaExceeded: Boolean = false,
    val errorMessage: String? = null,
    val exportSuccessMessage: String? = null,
    val isExportSuccess: Boolean = false
) {
    val isPdfMode: Boolean get() = inputMode == InputMode.PDF_DOCUMENT
    val currentOriginalBitmap: Bitmap? get() = if (activeSide == DocumentSide.FRONT) originalBitmap else backOriginalBitmap
    val currentMaskedBitmap: Bitmap? get() = if (activeSide == DocumentSide.FRONT) maskedBitmap else backMaskedBitmap
    val currentDetectedRegions: List<RedactionArea> get() = if (isPdfMode) pdfDetectedRegions else if (activeSide == DocumentSide.FRONT) detectedRegions else backDetectedRegions
    val hasDocument: Boolean get() = if (isPdfMode) pdfPagesBitmaps.isNotEmpty() else currentOriginalBitmap != null
    val hasFront: Boolean get() = originalBitmap != null
    val hasBack: Boolean get() = backOriginalBitmap != null
    val canExport: Boolean get() = (if (isPdfMode) pdfPagesBitmaps.isNotEmpty() else if (isDualSidedMode) hasFront && hasBack else hasFront) && dailyExportsRemaining > 0
    val dailyExportsRemaining: Int get() = (dailyExportsLimit - dailyExportsUsed).coerceAtLeast(0)
    val maxTemplatesLimit: Int get() = if (isProUser) 15 else 4
    val canCreateTemplate: Boolean get() = customTemplates.size < (maxTemplatesLimit - defaultTemplates.size)
    val redactedCount: Int get() = currentDetectedRegions.count { it.isRedacted }
    val defaultTemplates: List<WatermarkTemplate> get() = availableTemplates.filter { it.isDefault }
    val customTemplates: List<WatermarkTemplate> get() = availableTemplates.filter { !it.isDefault }
    val favoriteTemplates: List<WatermarkTemplate> get() = availableTemplates.filter { it.isFavorite }
}

sealed interface EditorUiEvent {
    data class SelectInputMode(val mode: InputMode) : EditorUiEvent
    data class ImageSelected(val bitmap: Bitmap, val targetSide: DocumentSide? = null) : EditorUiEvent
    data class PdfSelected(val name: String, val pages: List<Bitmap>, val detectedRegions: List<RedactionArea>) : EditorUiEvent
    data class ToggleDualSidedMode(val enabled: Boolean) : EditorUiEvent
    data class SelectDocumentSide(val side: DocumentSide) : EditorUiEvent
    data class ToggleRegionRedaction(val regionId: String) : EditorUiEvent
    data class RedactAllSensitive(val redact: Boolean) : EditorUiEvent
    data class ToggleGrayscale(val enabled: Boolean) : EditorUiEvent
    data class UpdatePixelIntensity(val percent: Int) : EditorUiEvent
    data class ToggleWatermarkDate(val enabled: Boolean) : EditorUiEvent
    data class ToggleGeneratePdf(val enabled: Boolean) : EditorUiEvent
    data class ToggleGeneratePdfSingleSide(val enabled: Boolean) : EditorUiEvent
    data class SetMaskingPreviewLoading(val isLoading: Boolean) : EditorUiEvent
    data class UpdateWatermarkText(val text: String) : EditorUiEvent
    data class UpdateWatermarkOpacity(val opacity: Float) : EditorUiEvent
    data class SelectTemplate(val template: WatermarkTemplate) : EditorUiEvent
    data class SaveTemplate(val template: WatermarkTemplate) : EditorUiEvent
    data class DeleteTemplate(val templateId: String) : EditorUiEvent
    data class ToggleFavoriteTemplate(val templateId: String) : EditorUiEvent
    data class UpdateWatermarkConfig(val config: WatermarkConfig) : EditorUiEvent
    data class ToggleBiometric(val enabled: Boolean) : EditorUiEvent
    data class ToggleDebugPro(val enabled: Boolean) : EditorUiEvent
    data class SetEditingRegions(val isEditing: Boolean) : EditorUiEvent
    data class SaveManualRegions(val side: DocumentSide, val manualRegions: List<RedactionArea>) : EditorUiEvent
    data class PurchasePro(val activity: Activity?) : EditorUiEvent
    data class RefreshPermissions(val context: Context) : EditorUiEvent
    data object ApplyAepdPreset : EditorUiEvent
    data object RestorePurchases : EditorUiEvent
    data object ExportDocument : EditorUiEvent
    data class SetLoading(val isLoading: Boolean, val message: String? = null) : EditorUiEvent
    data class SetExporting(val isExporting: Boolean, val message: String? = null) : EditorUiEvent
    data class ShowError(val message: String) : EditorUiEvent
    data object DismissError : EditorUiEvent
    data object DismissSuccessMessage : EditorUiEvent
}
