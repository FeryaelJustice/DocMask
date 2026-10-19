package com.feryaeljustice.docmask.domain.repository

import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    val isProUser: Flow<Boolean>
    suspend fun refreshPurchases()
    fun setDebugProStatus(isPro: Boolean)
}
