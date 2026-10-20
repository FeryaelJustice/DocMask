package com.feryaeljustice.docmask.domain.usecase

import com.feryaeljustice.docmask.domain.repository.BillingRepository
import com.feryaeljustice.docmask.domain.repository.DocumentQuotaRepository
import kotlinx.coroutines.flow.first

class CheckDailyQuotaUseCase(
    private val quotaRepository: DocumentQuotaRepository,
    private val billingRepository: BillingRepository
) {
    suspend operator fun invoke(): Boolean {
        val isPro = billingRepository.isProUser.first()
        return quotaRepository.canExportToday(isPro)
    }

    suspend fun getRemainingQuota(): Int {
        val isPro = billingRepository.isProUser.first()
        return quotaRepository.getRemainingExports(isPro)
    }
}
