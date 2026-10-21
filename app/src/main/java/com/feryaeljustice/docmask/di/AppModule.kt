package com.feryaeljustice.docmask.di

import com.feryaeljustice.docmask.core.util.AndroidBitmapProcessor
import com.feryaeljustice.docmask.core.util.BitmapProcessor
import com.feryaeljustice.docmask.data.local.preferences.UserPreferencesRepository
import com.feryaeljustice.docmask.data.local.preferences.UserPreferencesRepositoryImpl
import com.feryaeljustice.docmask.data.ml.SensitiveFieldDetector
import com.feryaeljustice.docmask.data.repository.BillingRepositoryImpl
import com.feryaeljustice.docmask.data.repository.DocumentProcessorRepositoryImpl
import com.feryaeljustice.docmask.data.repository.DocumentQuotaRepositoryImpl
import com.feryaeljustice.docmask.data.repository.StoragePreferencesRepositoryImpl
import com.feryaeljustice.docmask.data.repository.TemplateRepositoryImpl
import com.feryaeljustice.docmask.domain.repository.BillingRepository
import com.feryaeljustice.docmask.domain.repository.DocumentProcessorRepository
import com.feryaeljustice.docmask.domain.repository.DocumentQuotaRepository
import com.feryaeljustice.docmask.domain.repository.StoragePreferencesRepository
import com.feryaeljustice.docmask.domain.repository.TemplateRepository
import com.feryaeljustice.docmask.domain.usecase.ApplyAepdPresetUseCase
import com.feryaeljustice.docmask.domain.usecase.ApplyMaskingUseCase
import com.feryaeljustice.docmask.domain.usecase.CheckAppPermissionsUseCase
import com.feryaeljustice.docmask.domain.usecase.CheckDailyQuotaUseCase
import com.feryaeljustice.docmask.domain.usecase.DeleteWatermarkTemplateUseCase
import com.feryaeljustice.docmask.domain.usecase.DetectSensitiveRegionsUseCase
import com.feryaeljustice.docmask.domain.usecase.GetWatermarkTemplatesUseCase
import com.feryaeljustice.docmask.domain.usecase.RecordExportSuccessUseCase
import com.feryaeljustice.docmask.domain.usecase.SaveWatermarkTemplateUseCase
import com.feryaeljustice.docmask.domain.usecase.ToggleFavoriteTemplateUseCase
import com.feryaeljustice.docmask.domain.usecase.ValidateCustomStorageUriUseCase
import com.feryaeljustice.docmask.presentation.editor.EditorViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val coreModule = module {
    single<BitmapProcessor> { AndroidBitmapProcessor() }
    single { SensitiveFieldDetector() }
}

val dataModule = module {
    single<DocumentProcessorRepository> { DocumentProcessorRepositoryImpl(get(), get()) }
    single<TemplateRepository> { TemplateRepositoryImpl(androidContext()) }
    single<BillingRepository> { BillingRepositoryImpl(androidContext()) }
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(androidContext()) }
    single<DocumentQuotaRepository> { DocumentQuotaRepositoryImpl(androidContext()) }
    single<StoragePreferencesRepository> { StoragePreferencesRepositoryImpl(androidContext()) }
}

val domainModule = module {
    factory { DetectSensitiveRegionsUseCase(get()) }
    factory { ApplyMaskingUseCase(get()) }
    factory { GetWatermarkTemplatesUseCase(get()) }
    factory { SaveWatermarkTemplateUseCase(get()) }
    factory { DeleteWatermarkTemplateUseCase(get()) }
    factory { ToggleFavoriteTemplateUseCase(get()) }
    factory { CheckDailyQuotaUseCase(get(), get()) }
    factory { RecordExportSuccessUseCase(get()) }
    factory { ApplyAepdPresetUseCase() }
    factory { ValidateCustomStorageUriUseCase(androidContext()) }
    factory { CheckAppPermissionsUseCase() }
}

val viewModelModule = module {
    viewModel {
        EditorViewModel(
            detectSensitiveRegionsUseCase = get(),
            applyMaskingUseCase = get(),
            getWatermarkTemplatesUseCase = get(),
            saveWatermarkTemplateUseCase = get(),
            deleteWatermarkTemplateUseCase = get(),
            toggleFavoriteTemplateUseCase = get(),
            billingRepository = get(),
            userPreferencesRepository = get(),
            documentQuotaRepository = get(),
            storagePreferencesRepository = get(),
            applyAepdPresetUseCase = get(),
            checkAppPermissionsUseCase = get()
        )
    }
}

val appModules = listOf(
    coreModule,
    dataModule,
    domainModule,
    viewModelModule
)
