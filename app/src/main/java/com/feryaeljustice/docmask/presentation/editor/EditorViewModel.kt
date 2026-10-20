package com.feryaeljustice.docmask.presentation.editor

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.feryaeljustice.docmask.core.model.DocMaskResult
import com.feryaeljustice.docmask.core.model.DocumentSide
import com.feryaeljustice.docmask.core.model.MaskingOptions
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.core.model.WatermarkConfig
import com.feryaeljustice.docmask.data.local.preferences.UserPreferencesRepository
import com.feryaeljustice.docmask.domain.model.WatermarkTemplate
import com.feryaeljustice.docmask.domain.repository.BillingRepository
import com.feryaeljustice.docmask.domain.repository.DocumentQuotaRepository
import com.feryaeljustice.docmask.domain.repository.StoragePreferencesRepository
import com.feryaeljustice.docmask.domain.usecase.ApplyAepdPresetUseCase
import com.feryaeljustice.docmask.domain.usecase.ApplyMaskingUseCase
import com.feryaeljustice.docmask.domain.usecase.CheckAppPermissionsUseCase
import com.feryaeljustice.docmask.domain.usecase.DeleteWatermarkTemplateUseCase
import com.feryaeljustice.docmask.domain.usecase.DetectSensitiveRegionsUseCase
import com.feryaeljustice.docmask.domain.usecase.GetWatermarkTemplatesUseCase
import com.feryaeljustice.docmask.domain.usecase.SaveWatermarkTemplateUseCase
import com.feryaeljustice.docmask.domain.usecase.ToggleFavoriteTemplateUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    private val saveWatermarkTemplateUseCase: SaveWatermarkTemplateUseCase,
    private val deleteWatermarkTemplateUseCase: DeleteWatermarkTemplateUseCase,
    private val toggleFavoriteTemplateUseCase: ToggleFavoriteTemplateUseCase,
    private val billingRepository: BillingRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val documentQuotaRepository: DocumentQuotaRepository,
    private val storagePreferencesRepository: StoragePreferencesRepository,
    private val applyAepdPresetUseCase: ApplyAepdPresetUseCase,
    private val checkAppPermissionsUseCase: CheckAppPermissionsUseCase,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var maskingJob: Job? = null

    init {
        observeTemplates()
        observeBillingStatus()
        observeBiometricPreference()
        observeQuota()
        observeCustomStorage()
    }

    private fun observeQuota() {
        documentQuotaRepository.dailyExportCount.onEach { count ->
            val limit = documentQuotaRepository.getMaxDailyLimit(_uiState.value.isProUser)
            _uiState.update { it.copy(dailyExportsUsed = count, dailyExportsLimit = limit) }
        }.launchIn(viewModelScope)
    }

    private fun observeCustomStorage() {
        storagePreferencesRepository.customStorageUri.onEach { uriStr ->
            _uiState.update { it.copy(customStorageUri = uriStr) }
        }.launchIn(viewModelScope)
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
            val limit = documentQuotaRepository.getMaxDailyLimit(isPro)
            _uiState.update { current ->
                val updatedConfig = if (!isPro && current.maskingOptions.watermarkConfig.includeDate) {
                    current.maskingOptions.watermarkConfig.copy(includeDate = false)
                } else current.maskingOptions.watermarkConfig
                val updatedGeneratePdf = if (!isPro) false else current.generatePdfForDni
                current.copy(
                    isProUser = isPro,
                    dailyExportsLimit = limit,
                    generatePdfForDni = updatedGeneratePdf,
                    maskingOptions = current.maskingOptions.copy(watermarkConfig = updatedConfig)
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observeBiometricPreference() {
        userPreferencesRepository.isBiometricEnabled.onEach { isEnabled ->
            _uiState.update { it.copy(isBiometricEnabled = isEnabled) }
        }.launchIn(viewModelScope)
    }

    fun onEvent(event: EditorUiEvent) {
        when (event) {
            is EditorUiEvent.SelectInputMode -> onSelectInputMode(event.mode)
            is EditorUiEvent.ImageSelected -> onImageSelected(event.bitmap, event.targetSide)
            is EditorUiEvent.PdfSelected -> onPdfSelected(event.name, event.pages, event.detectedRegions)
            is EditorUiEvent.ToggleDualSidedMode -> onToggleDualSidedMode(event.enabled)
            is EditorUiEvent.SelectDocumentSide -> onSelectDocumentSide(event.side)
            is EditorUiEvent.ToggleRegionRedaction -> onToggleRegion(event.regionId)
            is EditorUiEvent.RedactAllSensitive -> onRedactAllSensitive(event.redact)
            is EditorUiEvent.ApplyAepdPreset -> onApplyAepdPreset()
            is EditorUiEvent.ToggleGrayscale -> onToggleGrayscale(event.enabled)
            is EditorUiEvent.UpdatePixelIntensity -> onUpdatePixelIntensity(event.percent)
            is EditorUiEvent.ToggleWatermarkDate -> onToggleWatermarkDate(event.enabled)
            is EditorUiEvent.ToggleGeneratePdf -> onToggleGeneratePdf(event.enabled)
            is EditorUiEvent.UpdateWatermarkText -> onUpdateWatermarkText(event.text)
            is EditorUiEvent.UpdateWatermarkOpacity -> onUpdateWatermarkOpacity(event.opacity)
            is EditorUiEvent.SelectTemplate -> onSelectTemplate(event.template)
            is EditorUiEvent.SaveTemplate -> onSaveTemplate(event.template)
            is EditorUiEvent.DeleteTemplate -> onDeleteTemplate(event.templateId)
            is EditorUiEvent.ToggleFavoriteTemplate -> onToggleFavoriteTemplate(event.templateId)
            is EditorUiEvent.UpdateWatermarkConfig -> onUpdateWatermarkConfig(event.config)
            is EditorUiEvent.ToggleBiometric -> onToggleBiometric(event.enabled)
            is EditorUiEvent.ToggleDebugPro -> onToggleDebugPro(event.enabled)
            is EditorUiEvent.SetEditingRegions -> onSetEditingRegions(event.isEditing)
            is EditorUiEvent.SaveManualRegions -> onSaveManualRegions(event.side, event.manualRegions)
            is EditorUiEvent.PurchasePro -> onPurchasePro(event.activity)
            is EditorUiEvent.RestorePurchases -> onRestorePurchases()
            is EditorUiEvent.ExportDocument -> onExportDocument()
            is EditorUiEvent.DownloadPdf -> onDownloadPdf()
            is EditorUiEvent.RefreshPermissions -> refreshPermissions(event.context)
            is EditorUiEvent.SetLoading -> _uiState.update { it.copy(isLoading = event.isLoading, loadingMessage = event.message) }
            is EditorUiEvent.SetExporting -> _uiState.update { it.copy(isExporting = event.isExporting, loadingMessage = event.message) }
            is EditorUiEvent.ShowError -> _uiState.update { it.copy(errorMessage = event.message) }
            is EditorUiEvent.DismissError -> _uiState.update { it.copy(errorMessage = null, quotaExceeded = false) }
            is EditorUiEvent.DismissSuccessMessage -> _uiState.update { it.copy(exportSuccessMessage = null, isExportSuccess = false) }
        }
    }

    private fun onSelectInputMode(mode: InputMode) {
        _uiState.update {
            it.copy(
                inputMode = mode,
                isDualSidedMode = (mode == InputMode.DNI_DUAL),
                activeSide = DocumentSide.FRONT
            )
        }
    }

    private fun onToggleDualSidedMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isDualSidedMode = enabled,
                inputMode = if (enabled) InputMode.DNI_DUAL else InputMode.SINGLE_PHOTO,
                activeSide = DocumentSide.FRONT
            )
        }
    }

    private fun onSelectDocumentSide(side: DocumentSide) {
        _uiState.update { it.copy(activeSide = side) }
    }

    private fun onToggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBiometricEnabled(enabled)
        }
    }

    private fun onSetEditingRegions(isEditing: Boolean) {
        _uiState.update { it.copy(isEditingRegions = isEditing) }
    }

    private fun onImageSelected(bitmap: Bitmap, targetSide: DocumentSide? = null) {
        val side = targetSide ?: _uiState.value.activeSide
        viewModelScope.launch(defaultDispatcher) {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Analizando documento con IA local...") }

            // 1. Detect sensitive regions with ML Kit
            val detectionResult = detectSensitiveRegionsUseCase(bitmap)
            val regions = when (detectionResult) {
                is DocMaskResult.Success -> detectionResult.data
                is DocMaskResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = detectionResult.error.message) }
                    emptyList()
                }
            }

            _uiState.update { it.copy(loadingMessage = "Aplicando protección y marcas de agua...") }

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
                        loadingMessage = null,
                        originalBitmap = bitmap,
                        detectedRegions = regions,
                        maskedBitmap = maskedBitmap
                    )
                } else {
                    current.copy(
                        isLoading = false,
                        loadingMessage = null,
                        backOriginalBitmap = bitmap,
                        backDetectedRegions = regions,
                        backMaskedBitmap = maskedBitmap
                    )
                }
            }
        }
    }

    private fun onPdfSelected(name: String, pages: List<Bitmap>, detectedRegions: List<RedactionArea>) {
        _uiState.update {
            it.copy(
                isLoading = true,
                loadingMessage = "Analizando páginas de PDF...",
                pdfDocumentName = name,
                pdfPageCount = pages.size,
                pdfPagesBitmaps = pages,
                isPdfDownloadAvailable = false
            )
        }
        viewModelScope.launch(defaultDispatcher) {
            val allRegions = if (detectedRegions.isNotEmpty()) {
                detectedRegions
            } else {
                val list = mutableListOf<RedactionArea>()
                pages.forEachIndexed { pageIndex, pageBitmap ->
                    val result = detectSensitiveRegionsUseCase(pageBitmap)
                    if (result is DocMaskResult.Success) {
                        result.data.forEach { area ->
                            list.add(area.copy(pageIndex = pageIndex + 1))
                        }
                    }
                }
                list
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    loadingMessage = null,
                    pdfDetectedRegions = allRegions
                )
            }
            reapplyMaskingPdf(allRegions, _uiState.value.maskingOptions)
        }
    }

    private fun onToggleRegion(regionId: String) {
        val state = _uiState.value
        if (state.isPdfMode) {
            val updated = state.pdfDetectedRegions.map { area ->
                if (area.id == regionId) area.copy(isRedacted = !area.isRedacted) else area
            }
            reapplyMaskingPdf(updated, state.maskingOptions)
        } else if (state.activeSide == DocumentSide.FRONT) {
            val updatedRegions = state.detectedRegions.map { area ->
                if (area.id == regionId) area.copy(isRedacted = !area.isRedacted) else area
            }
            reapplyMaskingSingleSide(DocumentSide.FRONT, updatedRegions, state.maskingOptions)
        } else {
            val updatedRegions = state.backDetectedRegions.map { area ->
                if (area.id == regionId) area.copy(isRedacted = !area.isRedacted) else area
            }
            reapplyMaskingSingleSide(DocumentSide.BACK, updatedRegions, state.maskingOptions)
        }
    }

    private fun onRedactAllSensitive(redact: Boolean) {
        val state = _uiState.value
        if (state.isPdfMode) {
            val updated = state.pdfDetectedRegions.map { it.copy(isRedacted = redact) }
            reapplyMaskingPdf(updated, state.maskingOptions)
        } else if (state.activeSide == DocumentSide.FRONT) {
            val updatedRegions = state.detectedRegions.map { it.copy(isRedacted = redact) }
            reapplyMaskingSingleSide(DocumentSide.FRONT, updatedRegions, state.maskingOptions)
        } else {
            val updatedRegions = state.backDetectedRegions.map { it.copy(isRedacted = redact) }
            reapplyMaskingSingleSide(DocumentSide.BACK, updatedRegions, state.maskingOptions)
        }
    }

    private fun onApplyAepdPreset() {
        val state = _uiState.value
        if (state.isPdfMode) {
            val updated = applyAepdPresetUseCase(state.pdfDetectedRegions)
            reapplyMaskingPdf(updated, state.maskingOptions)
        } else if (state.activeSide == DocumentSide.FRONT) {
            val updated = applyAepdPresetUseCase(state.detectedRegions)
            reapplyMaskingSingleSide(DocumentSide.FRONT, updated, state.maskingOptions)
        } else {
            val updated = applyAepdPresetUseCase(state.backDetectedRegions)
            reapplyMaskingSingleSide(DocumentSide.BACK, updated, state.maskingOptions)
        }
    }

    private fun onToggleGrayscale(enabled: Boolean) {
        val updatedOptions = _uiState.value.maskingOptions.copy(applyGrayscale = enabled)
        _uiState.update { it.copy(maskingOptions = updatedOptions) }
        reapplyMaskingAll(updatedOptions)
    }

    private fun onUpdatePixelIntensity(percent: Int) {
        val updatedOptions = _uiState.value.maskingOptions.copy(pixelPercent = percent)
        _uiState.update { it.copy(maskingOptions = updatedOptions) }
        reapplyMaskingAll(updatedOptions)
    }

    private fun onToggleWatermarkDate(enabled: Boolean) {
        if (!_uiState.value.isProUser && enabled) {
            _uiState.update {
                it.copy(errorMessage = "La inclusión de fecha en la marca de agua es una función exclusiva de DocMask Pro.")
            }
            return
        }
        val updatedWatermark = _uiState.value.maskingOptions.watermarkConfig.copy(includeDate = enabled)
        val updatedOptions = _uiState.value.maskingOptions.copy(watermarkConfig = updatedWatermark)
        _uiState.update { it.copy(maskingOptions = updatedOptions) }
        reapplyMaskingAll(updatedOptions)
    }

    private fun onToggleGeneratePdf(enabled: Boolean) {
        if (!_uiState.value.isProUser && enabled) {
            _uiState.update {
                it.copy(errorMessage = "La generación de documento PDF combinado para DNI es una función exclusiva de DocMask Pro.")
            }
            return
        }
        _uiState.update { it.copy(generatePdfForDni = enabled) }
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
        val customTemplates = _uiState.value.customTemplates
        val isEditing = customTemplates.any { it.id == template.id }
        val maxAllowed = if (_uiState.value.isProUser) 15 else 4
        if (!isEditing && customTemplates.size >= maxAllowed) {
            val msg = if (!_uiState.value.isProUser) {
                "Has alcanzado el límite de 4 plantillas de la versión gratuita. Pasa a DocMask Pro para guardar hasta 15."
            } else {
                "Has alcanzado el límite máximo de 15 plantillas de DocMask Pro."
            }
            _uiState.update { it.copy(errorMessage = msg) }
            return
        }

        viewModelScope.launch {
            saveWatermarkTemplateUseCase(template)
            if (_uiState.value.selectedTemplateId == template.id) {
                val updatedOptions = _uiState.value.maskingOptions.copy(watermarkConfig = template.config)
                _uiState.update { it.copy(maskingOptions = updatedOptions) }
                reapplyMaskingAll(updatedOptions)
            }
        }
    }

    private fun onSaveManualRegions(side: DocumentSide, manualRegions: List<RedactionArea>) {
        val state = _uiState.value
        if (side == DocumentSide.FRONT) {
            val aiRegions = state.detectedRegions.filter { it.type != com.feryaeljustice.docmask.core.model.SensitiveFieldType.CUSTOM_SELECTION }
            val combined = aiRegions + manualRegions
            reapplyMaskingSingleSide(DocumentSide.FRONT, combined, state.maskingOptions)
        } else {
            val aiRegions = state.backDetectedRegions.filter { it.type != com.feryaeljustice.docmask.core.model.SensitiveFieldType.CUSTOM_SELECTION }
            val combined = aiRegions + manualRegions
            reapplyMaskingSingleSide(DocumentSide.BACK, combined, state.maskingOptions)
        }
    }

    private fun onPurchasePro(activity: Activity?) {
        if (activity == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Conectando con Google Play Store...") }
            val result = billingRepository.launchBillingFlow(activity)
            _uiState.update { it.copy(isLoading = false, loadingMessage = null) }
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Error al iniciar compra en Google Play")
                }
            }
        }
    }

    private fun onRestorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadingMessage = "Comprobando compras en Google Play...") }
            val result = billingRepository.restorePurchases()
            _uiState.update { it.copy(isLoading = false, loadingMessage = null) }
            result.fold(
                onSuccess = { restored ->
                    if (restored) {
                        _uiState.update { it.copy(exportSuccessMessage = "¡Compras restauradas! DocMask Pro activado.") }
                    } else {
                        _uiState.update { it.copy(errorMessage = "No se encontraron compras activas asociadas a esta cuenta de Google Play.") }
                    }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = "Error al restaurar compras: ${error.message}") }
                }
            )
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

    private fun onToggleFavoriteTemplate(templateId: String) {
        viewModelScope.launch {
            toggleFavoriteTemplateUseCase(templateId)
        }
    }

    private fun onToggleDebugPro(enabled: Boolean) {
        billingRepository.setDebugProStatus(enabled)
    }

    private fun onUpdateWatermarkConfig(config: WatermarkConfig) {
        val updatedOptions = _uiState.value.maskingOptions.copy(watermarkConfig = config)
        _uiState.update { it.copy(maskingOptions = updatedOptions) }
        reapplyMaskingAll(updatedOptions)
    }

    private fun reapplyMaskingPdf(regions: List<RedactionArea>, options: MaskingOptions) {
        maskingJob?.cancel()
        maskingJob = viewModelScope.launch(defaultDispatcher) {
            val pages = _uiState.value.pdfPagesBitmaps
            val masked = pages.mapIndexed { index, pageBitmap ->
                val pageRegions = regions.filter { it.pageIndex == index + 1 }
                val res = applyMaskingUseCase(pageBitmap, pageRegions, options)
                if (res is DocMaskResult.Success) res.data else pageBitmap
            }
            _uiState.update {
                it.copy(
                    pdfDetectedRegions = regions,
                    pdfMaskedPagesBitmaps = masked,
                    maskingOptions = options
                )
            }
        }
    }

    private fun reapplyMaskingAll(options: MaskingOptions) {
        maskingJob?.cancel()
        maskingJob = viewModelScope.launch(defaultDispatcher) {
            val state = _uiState.value
            if (state.isPdfMode) {
                val pages = state.pdfPagesBitmaps
                val regions = state.pdfDetectedRegions
                val masked = pages.mapIndexed { index, pageBitmap ->
                    val pageRegions = regions.filter { it.pageIndex == index + 1 }
                    val res = applyMaskingUseCase(pageBitmap, pageRegions, options)
                    if (res is DocMaskResult.Success) res.data else pageBitmap
                }
                _uiState.update {
                    it.copy(
                        pdfMaskedPagesBitmaps = masked,
                        maskingOptions = options
                    )
                }
                return@launch
            }

            val frontOrig = state.originalBitmap
            val frontRegions = state.detectedRegions
            val newFrontMasked = if (frontOrig != null) {
                val res = applyMaskingUseCase(frontOrig, frontRegions, options)
                if (res is DocMaskResult.Success) res.data else state.maskedBitmap
            } else null

            val backOrig = state.backOriginalBitmap
            val backRegions = state.backDetectedRegions
            val newBackMasked = if (backOrig != null) {
                val res = applyMaskingUseCase(backOrig, backRegions, options)
                if (res is DocMaskResult.Success) res.data else state.backMaskedBitmap
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
        viewModelScope.launch(defaultDispatcher) {
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
        viewModelScope.launch {
            if (!documentQuotaRepository.canExportToday(_uiState.value.isProUser)) {
                val limit = _uiState.value.dailyExportsLimit
                _uiState.update {
                    it.copy(
                        quotaExceeded = true,
                        errorMessage = "Has alcanzado el límite diario de $limit documentos. " +
                                if (!it.isProUser) "Mejora a DocMask Pro para obtener hasta 40 documentos al día." else "Vuelve mañana."
                    )
                }
                return@launch
            }
            documentQuotaRepository.recordExport()
            _uiState.update { it.copy(isExportSuccess = true, isPdfDownloadAvailable = true) }
        }
    }

    private fun onDownloadPdf() {
        viewModelScope.launch {
            if (!documentQuotaRepository.canExportToday(_uiState.value.isProUser)) {
                val limit = _uiState.value.dailyExportsLimit
                _uiState.update {
                    it.copy(
                        quotaExceeded = true,
                        errorMessage = "Has alcanzado el límite diario de $limit documentos."
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isPdfDownloadAvailable = true) }
        }
    }

    fun refreshPermissions(context: Context) {
        val permissions = checkAppPermissionsUseCase(context)
        _uiState.update { it.copy(permissions = permissions) }
    }

    fun updateCustomStorageUri(uriString: String?) {
        viewModelScope.launch {
            storagePreferencesRepository.setCustomStorageUri(uriString)
        }
    }
}
