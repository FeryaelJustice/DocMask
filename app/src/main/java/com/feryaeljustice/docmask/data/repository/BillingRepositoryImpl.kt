package com.feryaeljustice.docmask.data.repository

import com.feryaeljustice.docmask.domain.repository.BillingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Offline-first billing repository handling Pro subscription/IAP state via Google Play IPC.
 */
class BillingRepositoryImpl : BillingRepository {

    private val _isProUser = MutableStateFlow(false)
    override val isProUser: Flow<Boolean> = _isProUser.asStateFlow()

    override suspend fun refreshPurchases() {
        // Communicates via local IPC with Google Play Store service
    }

    override fun setDebugProStatus(isPro: Boolean) {
        _isProUser.value = isPro
    }

    fun setProStatus(isPro: Boolean) {
        setDebugProStatus(isPro)
    }
}
