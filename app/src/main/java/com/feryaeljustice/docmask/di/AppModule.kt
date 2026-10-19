package com.feryaeljustice.docmask.di

import com.feryaeljustice.docmask.core.util.AndroidBitmapProcessor
import com.feryaeljustice.docmask.core.util.BitmapProcessor
import com.feryaeljustice.docmask.data.ml.SensitiveFieldDetector
import com.feryaeljustice.docmask.data.repository.BillingRepositoryImpl
import com.feryaeljustice.docmask.data.repository.DocumentProcessorRepositoryImpl
import com.feryaeljustice.docmask.data.repository.TemplateRepositoryImpl
import com.feryaeljustice.docmask.domain.repository.BillingRepository
import com.feryaeljustice.docmask.domain.repository.DocumentProcessorRepository
import com.feryaeljustice.docmask.domain.repository.TemplateRepository
import com.feryaeljustice.docmask.domain.usecase.ApplyMaskingUseCase
import com.feryaeljustice.docmask.domain.usecase.DetectSensitiveRegionsUseCase
import com.feryaeljustice.docmask.domain.usecase.GetWatermarkTemplatesUseCase
import com.feryaeljustice.docmask.presentation.editor.EditorViewModel
import com.feryaeljustice.docmask.data.local.preferences.UserPreferencesRepository
import com.feryaeljustice.docmask.data.local.preferences.UserPreferencesRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val coreModule = module {
    single<BitmapProcessor> { AndroidBitmapProcessor() }
    single { SensitiveFieldDetector() }
}

val dataModule = module {
    single<DocumentProcessorRepository> { DocumentProcessorRepositoryImpl(get(), get()) }
    single<TemplateRepository> { TemplateRepositoryImpl() }
    single<BillingRepository> { BillingRepositoryImpl() }
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(androidContext()) }
}

val domainModule = module {
    factory { DetectSensitiveRegionsUseCase(get()) }
    factory { ApplyMaskingUseCase(get()) }
    factory { GetWatermarkTemplatesUseCase(get()) }
}

val viewModelModule = module {
    viewModel { EditorViewModel(get(), get(), get(), get(), get()) }
}

val appModules = listOf(
    coreModule,
    dataModule,
    domainModule,
    viewModelModule
)
