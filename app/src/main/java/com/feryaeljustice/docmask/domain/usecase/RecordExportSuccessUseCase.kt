package com.feryaeljustice.docmask.domain.usecase

import com.feryaeljustice.docmask.domain.repository.DocumentQuotaRepository

class RecordExportSuccessUseCase(
    private val quotaRepository: DocumentQuotaRepository
) {
    suspend operator fun invoke() {
        quotaRepository.recordExport()
    }
}
