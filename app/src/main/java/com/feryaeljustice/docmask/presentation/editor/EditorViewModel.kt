package com.feryaeljustice.docmask.presentation.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feryaeljustice.docmask.core.model.DocMaskResult
import com.feryaeljustice.docmask.core.model.DocumentSide
import com.feryaeljustice.docmask.core.model.MaskingOptions
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.domain.model.WatermarkTemplate
import com.feryaeljustice.docmask.domain.repository.BillingRepository
import com.feryaeljustice.docmask.domain.usecase.ApplyMaskingUseCase
import com.feryaeljustice.docmask.domain.usecase.DetectSensitiveRegionsUseCase
import com.feryaeljustice.docmask.domain.usecase.GetWatermarkTemplatesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import com.feryaeljustice.docmask.data.local.preferences.UserPreferencesRepository
import kotlinx.coroutines.launch

import com.feryaeljustice.docmask.domain.usecase.SaveWatermarkTemplateUseCase
import com.feryaeljustice.docmask.domain.usecase.DeleteWatermarkTemplateUseCase

class EditorViewModel(
    private val detectSensitiveRegionsUseCase: DetectSensitiveRegionsUseCase,
    private val applyMaskingUseCase: ApplyMaskingUseCase,
    private val getWatermarkTemplatesUseCase: GetWatermarkTemplatesUseCase,
    private val saveWatermarkTemplateUseCase: SaveWatermarkTemplateUseCase,
    private val deleteWatermarkTemplateUseCase: DeleteWatermarkTemplateUseCase,
    private val billingRepository: BillingRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        observeTemplates()
        observeBillingStatus()
        observeBiometricPreference()
    }

    private fun observeTemplates() {
        getWatermarkTemplatesUseCase().onEach { templates ->
            _uiState.update { current ->
                val defaultTemplate = templates.firstOrNull { it.isDefault }
                current.copy(
                    availableTemplates = templates,
                    selectedTemplateId = current.selectedTemplateId ?: defaultTemplate?.id,
                    maskingOptions = if (current.selectedTemplateId == null && defaultTemplate != null) {
                        current.maskingOptions.copy(watermarkConfig = defaultTemplate.config)
                    } else {
                        current.maskingOptions
                    }
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observeBillingStatus() {
        billingRepository.isProUser.onEach { isPro ->
            _uiState.update { it.copy(isProUser = isPro) }
        }.launchIn(viewModelScope)
    }

    private fun observeBiometricPreference() {
        userPreferencesRepository.isBiometricEnabled.onEach { isEnabled ->
            _uiState.update { it.copy(isBiometricEnabled = isEnabled) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: EditorUiEvent) {
        when (event) {
            is EditorUiEvent.ImageSelected -> onImageSelected(event.bitmap, event.targetSide)
            is EditorUiEvent.ToggleDualSidedMode -> onToggleDualSidedMode(event.enabled)
            is EditorUiEvent.SelectDocumentSide -> onSelectDocumentSide(event.side)
            is EditorUiEvent.ToggleRegionRedaction -> onToggleRegion(event.regionId)
            is EditorUiEvent.RedactAllSensitive -> onRedactAllSensitive(event.redact)
            is EditorUiEvent.ToggleGrayscale -> onToggleGrayscale(event.enabled)
            is EditorUiEvent.UpdateWatermarkText -> onUpdateWatermarkText(event.text)
            is EditorUiEvent.UpdateWatermarkOpacity -> onUpdateWatermarkOpacity(event.opacity)
            is EditorUiEvent.SelectTemplate -> onSelectTemplate(event.template)
            is EditorUiEvent.SaveTemplate -> onSaveTemplate(event.template)
            is EditorUiEvent.DeleteTemplate -> onDeleteTemplate(event.templateId)
            is EditorUiEvent.UpdateWatermarkConfig -> onUpdateWatermarkConfig(event.config)
            is EditorUiEvent.ToggleBiometric -> onToggleBiometric(event.enabled)
            is EditorUiEvent.ToggleDebugPro -> onToggleDebugPro(event.enabled)
            is EditorUiEvent.ExportDocument -> onExportDocument()
            is EditorUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun onToggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBiometricEnabled(enabled)
        }
    }

    private fun onToggleDualSidedMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isDualSidedMode = enabled,
                activeSide = if (!enabled) DocumentSide.FRONT else it.activeSide
            )
        }
    }

    private fun onSelectDocumentSide(side: DocumentSide) {
        _uiState.update { it.copy(activeSide = side) }
    }

    private fun onImageSelected(bitmap: Bitmap, targetSide: DocumentSide? = null) {
        val side = targetSide ?: _uiState.value.activeSide
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Detect sensitive regions with ML Kit
            val detectionResult = detectSensitiveRegionsUseCase(bitmap)
            val regions = when (detectionResult) {
                is DocMaskResult.Success -> detectionResult.data
                is DocMaskResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = detectionResult.error.message) }
                    emptyList()
                }
            }

            // 2. Render initial masked preview
            val maskingResult = applyMaskingUseCase(
                source = bitmap,
                areas = regions,
                options = _uiState.value.maskingOptions
            )

            val maskedBitmap = when (maskingResult) {
                is DocMaskResult.Success -> maskingResult.data
                is DocMaskResult.Failure -> bitmap
            }

            _uiState.update { current ->
                if (side == DocumentSide.FRONT) {
                    current.copy(
                        isLoading = false,
                        originalBitmap = bitmap,
                        detectedRegions = regions,
                        maskedBitmap = maskedBitmap
                    )
                } else {
                    current.copy(
                        isLoading = false,
                        backOriginalBitmap = bitmap,
                        backDetectedRegions = regions,
                        backMaskedBitmap = maskedBitmap
                    )
                }
            }
        }
    }

    private fun onToggleRegion(regionId: String) {
        val currentSide = _uiState.value.activeSide
        if (currentSide == DocumentSide.FRONT) {
            val updatedRegions = _uiState.value.detectedRegions.map { area ->
                if (area.id == regionId) area.copy(isRedacted = !area.isRedacted) else area
            }
            reapplyMaskingSingleSide(DocumentSide.FRONT, updatedRegions, _uiState.value.maskingOptions)
        } else {
            val updatedRegions = _uiState.value.backDetectedRegions.map { area ->
                if (area.id == regionId) area.copy(isRedacted = !area.isRedacted) else area
            }
            reapplyMaskingSingleSide(DocumentSide.BACK, updatedRegions, _uiState.value.maskingOptions)
        }
    }

    private fun onRedactAllSensitive(redact: Boolean) {
        val currentSide = _uiState.value.activeSide
        if (currentSide == DocumentSide.FRONT) {
            val updatedRegions = _uiState.value.detectedRegions.map { area ->
                area.copy(isRedacted = redact)
            }
            reapplyMaskingSingleSide(DocumentSide.FRONT, updatedRegions, _uiState.value.maskingOptions)
        } else {
            val updatedRegions = _uiState.value.backDetectedRegions.map { area ->
                area.copy(isRedacted = redact)
            }
            reapplyMaskingSingleSide(DocumentSide.BACK, updatedRegions, _uiState.value.maskingOptions)
        }
    }

    private fun onToggleGrayscale(enabled: Boolean) {
        val updatedOptions = _uiState.value.maskingOptions.copy(applyGrayscale = enabled)
        _uiState.update { it.copy(maskingOptions = updatedOptions) }
        reapplyMaskingAll(updatedOptions)
    }

    private fun onUpdateWatermarkText(text: String) {
        val updatedWatermark = _uiState.value.maskingOptions.watermarkConfig.copy(purposeText = text)
        val updatedOptions = _uiState.value.maskingOptions.copy(watermarkConfig = updatedWatermark)
        _uiState.update { it.copy(maskingOptions = updatedOptions) }
        reapplyMaskingAll(updatedOptions)
    }

    private fun onUpdateWatermarkOpacity(opacity: Float) {
        val updatedWatermark = _uiState.value.maskingOptions.watermarkConfig.copy(opacity = opacity)
        val updatedOptions = _uiState.value.maskingOptions.copy(watermarkConfig = updatedWatermark)
        _uiState.update { it.copy(maskingOptions = updatedOptions) }
        reapplyMaskingAll(updatedOptions)
    }

    private fun onSelectTemplate(template: WatermarkTemplate) {
        val updatedOptions = _uiState.value.maskingOptions.copy(watermarkConfig = template.config)
        _uiState.update { it.copy(selectedTemplateId = template.id, maskingOptions = updatedOptions) }
        reapplyMaskingAll(updatedOptions)
    }

    private fun onSaveTemplate(template: WatermarkTemplate) {
        viewModelScope.launch {
            saveWatermarkTemplateUseCase(template)
            if (_uiState.value.selectedTemplateId == template.id) {
                val updatedOptions = _uiState.value.maskingOptions.copy(watermarkConfig = template.config)
                _uiState.update { it.copy(maskingOptions = updatedOptions) }
                reapplyMaskingAll(updatedOptions)
            }
        }
    }

    private fun onDeleteTemplate(templateId: String) {
        viewModelScope.launch {
            deleteWatermarkTemplateUseCase(templateId)
            if (_uiState.value.selectedTemplateId == templateId) {
                val defaultTemplate = _uiState.value.availableTemplates.firstOrNull { it.isDefault }
                val updatedOptions = if (defaultTemplate != null) {
                    _uiState.value.maskingOptions.copy(watermarkConfig = defaultTemplate.config)
                } else _uiState.value.maskingOptions
                _uiState.update {
                    it.copy(
                        selectedTemplateId = defaultTemplate?.id,
                        maskingOptions = updatedOptions
                    )
                }
                reapplyMaskingAll(updatedOptions)
            }
        }
    }

    private fun onToggleDebugPro(enabled: Boolean) {
        billingRepository.setDebugProStatus(enabled)
    }

    private fun onUpdateWatermarkConfig(config: com.feryaeljustice.docmask.core.model.WatermarkConfig) {
        val updatedOptions = _uiState.value.maskingOptions.copy(watermarkConfig = config)
        _uiState.update { it.copy(maskingOptions = updatedOptions) }
        reapplyMaskingAll(updatedOptions)
    }

    private fun reapplyMaskingAll(options: MaskingOptions) {
        viewModelScope.launch {
            val frontOrig = _uiState.value.originalBitmap
            val frontRegions = _uiState.value.detectedRegions
            val newFrontMasked = if (frontOrig != null) {
                val res = applyMaskingUseCase(frontOrig, frontRegions, options)
                if (res is DocMaskResult.Success) res.data else _uiState.value.maskedBitmap
            } else null

            val backOrig = _uiState.value.backOriginalBitmap
            val backRegions = _uiState.value.backDetectedRegions
            val newBackMasked = if (backOrig != null) {
                val res = applyMaskingUseCase(backOrig, backRegions, options)
                if (res is DocMaskResult.Success) res.data else _uiState.value.backMaskedBitmap
            } else null

            _uiState.update {
                it.copy(
                    maskingOptions = options,
                    maskedBitmap = newFrontMasked,
                    backMaskedBitmap = newBackMasked
                )
            }
        }
    }

    private fun reapplyMaskingSingleSide(side: DocumentSide, regions: List<RedactionArea>, options: MaskingOptions) {
        viewModelScope.launch {
            if (side == DocumentSide.FRONT) {
                val orig = _uiState.value.originalBitmap ?: return@launch
                val res = applyMaskingUseCase(orig, regions, options)
                if (res is DocMaskResult.Success) {
                    _uiState.update { it.copy(detectedRegions = regions, maskedBitmap = res.data) }
                }
            } else {
                val orig = _uiState.value.backOriginalBitmap ?: return@launch
                val res = applyMaskingUseCase(orig, regions, options)
                if (res is DocMaskResult.Success) {
                    _uiState.update { it.copy(backDetectedRegions = regions, backMaskedBitmap = res.data) }
                }
            }
        }
    }

    private fun onExportDocument() {
        _uiState.update { it.copy(isExportSuccess = true) }
    }
}
