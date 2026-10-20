package com.feryaeljustice.docmask.domain.repository

import kotlinx.coroutines.flow.Flow

interface DocumentQuotaRepository {
    val dailyExportCount: Flow<Int>
    suspend fun getRemainingExports(isPro: Boolean): Int
    suspend fun canExportToday(isPro: Boolean): Boolean
    suspend fun recordExport()
    suspend fun getMaxDailyLimit(isPro: Boolean): Int

    companion object {
        const val FREE_DAILY_LIMIT = 3
        const val PRO_DAILY_LIMIT = 40
        const val FREE_MAX_TEMPLATES = 4
        const val PRO_MAX_TEMPLATES = 15
    }
}
