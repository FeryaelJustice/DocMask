package com.feryaeljustice.docmask.domain.repository

import android.app.Activity
import kotlinx.coroutines.flow.Flow

interface BillingRepository {
    val isProUser: Flow<Boolean>
    suspend fun refreshPurchases()
    suspend fun restorePurchases(): Result<Boolean>
    suspend fun launchBillingFlow(activity: Activity): Result<Unit>
    fun setDebugProStatus(isPro: Boolean)
}
