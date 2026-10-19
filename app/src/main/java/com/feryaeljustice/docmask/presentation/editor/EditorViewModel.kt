package com.feryaeljustice.docmask.presentation.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feryaeljustice.docmask.core.model.DocMaskResult
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
import kotlinx.coroutines.launch

class EditorViewModel(
    private val detectSensitiveRegionsUseCase: DetectSensitiveRegionsUseCase,
    private val applyMaskingUseCase: ApplyMaskingUseCase,
    private val getWatermarkTemplatesUseCase: GetWatermarkTemplatesUseCase,
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        observeTemplates()
        observeBillingStatus()
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

    fun onEvent(event: EditorUiEvent) {
        when (event) {
            is EditorUiEvent.ImageSelected -> onImageSelected(event.bitmap)
            is EditorUiEvent.ToggleRegionRedaction -> onToggleRegion(event.regionId)
            is EditorUiEvent.RedactAllSensitive -> onRedactAllSensitive(event.redact)
            is EditorUiEvent.ToggleGrayscale -> onToggleGrayscale(event.enabled)
            is EditorUiEvent.UpdateWatermarkText -> onUpdateWatermarkText(event.text)
            is EditorUiEvent.SelectTemplate -> onSelectTemplate(event.template)
            is EditorUiEvent.UpdateWatermarkConfig -> onUpdateWatermarkConfig(event.config)
            is EditorUiEvent.ExportDocument -> onExportDocument()
            is EditorUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun onImageSelected(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    originalBitmap = bitmap,
                    errorMessage = null
                )
            }

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

            _uiState.update {
                it.copy(
                    isLoading = false,
                    detectedRegions = regions,
                    maskedBitmap = maskedBitmap
                )
            }
        }
    }

    private fun onToggleRegion(regionId: String) {
        val updatedRegions = _uiState.value.detectedRegions.map { area ->
            if (area.id == regionId) area.copy(isRedacted = !area.isRedacted) else area
        }
        reapplyMasking(updatedRegions, _uiState.value.maskingOptions)
    }

    private fun onRedactAllSensitive(redact: Boolean) {
        val updatedRegions = _uiState.value.detectedRegions.map { area ->
            area.copy(isRedacted = redact)
        }
        reapplyMasking(updatedRegions, _uiState.value.maskingOptions)
    }

    private fun onToggleGrayscale(enabled: Boolean) {
        val updatedOptions = _uiState.value.maskingOptions.copy(applyGrayscale = enabled)
        _uiState.update { it.copy(maskingOptions = updatedOptions) }
        reapplyMasking(_uiState.value.detectedRegions, updatedOptions)
    }

    private fun onUpdateWatermarkText(text: String) {
        val updatedWatermark = _uiState.value.maskingOptions.watermarkConfig.copy(purposeText = text)
        val updatedOptions = _uiState.value.maskingOptions.copy(watermarkConfig = updatedWatermark)
        _uiState.update { it.copy(maskingOptions = updatedOptions) }
        reapplyMasking(_uiState.value.detectedRegions, updatedOptions)
    }

    private fun onSelectTemplate(template: WatermarkTemplate) {
        val updatedOptions = _uiState.value.maskingOptions.copy(watermarkConfig = template.config)
        _uiState.update { it.copy(selectedTemplateId = template.id, maskingOptions = updatedOptions) }
        reapplyMasking(_uiState.value.detectedRegions, updatedOptions)
    }

    private fun onUpdateWatermarkConfig(config: com.feryaeljustice.docmask.core.model.WatermarkConfig) {
        val updatedOptions = _uiState.value.maskingOptions.copy(watermarkConfig = config)
        _uiState.update { it.copy(maskingOptions = updatedOptions) }
        reapplyMasking(_uiState.value.detectedRegions, updatedOptions)
    }

    private fun reapplyMasking(regions: List<RedactionArea>, options: MaskingOptions) {
        val original = _uiState.value.originalBitmap ?: return
        viewModelScope.launch {
            val result = applyMaskingUseCase(original, regions, options)
            when (result) {
                is DocMaskResult.Success -> {
                    _uiState.update {
                        it.copy(
                            detectedRegions = regions,
                            maskingOptions = options,
                            maskedBitmap = result.data
                        )
                    }
                }
                is DocMaskResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = result.error.message) }
                }
            }
        }
    }

    private fun onExportDocument() {
        _uiState.update { it.copy(isExportSuccess = true) }
    }
}
