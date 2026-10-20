package com.feryaeljustice.docmask.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.feryaeljustice.docmask.domain.repository.DocumentQuotaRepository
import com.feryaeljustice.docmask.domain.repository.DocumentQuotaRepository.Companion.FREE_DAILY_LIMIT
import com.feryaeljustice.docmask.domain.repository.DocumentQuotaRepository.Companion.PRO_DAILY_LIMIT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Context.quotaDataStore by preferencesDataStore(name = "docmask_quota_preferences")

class DocumentQuotaRepositoryImpl(
    private val context: Context
) : DocumentQuotaRepository {

    companion object {
        private val KEY_LAST_EXPORT_DATE = stringPreferencesKey("key_last_export_date")
        private val KEY_DAILY_EXPORT_COUNT = intPreferencesKey("key_daily_export_count")
    }

    private fun getCurrentDateKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    override val dailyExportCount: Flow<Int> = context.quotaDataStore.data.map { preferences ->
        val savedDate = preferences[KEY_LAST_EXPORT_DATE]
        val today = getCurrentDateKey()
        if (savedDate == today) {
            preferences[KEY_DAILY_EXPORT_COUNT] ?: 0
        } else {
            0
        }
    }

    override suspend fun getMaxDailyLimit(isPro: Boolean): Int {
        return if (isPro) PRO_DAILY_LIMIT else FREE_DAILY_LIMIT
    }

    override suspend fun canExportToday(isPro: Boolean): Boolean {
        val count = dailyExportCount.first()
        val limit = getMaxDailyLimit(isPro)
        return count < limit
    }

    override suspend fun getRemainingExports(isPro: Boolean): Int {
        val count = dailyExportCount.first()
        val limit = getMaxDailyLimit(isPro)
        return (limit - count).coerceAtLeast(0)
    }

    override suspend fun recordExport() {
        val today = getCurrentDateKey()
        context.quotaDataStore.edit { preferences ->
            val savedDate = preferences[KEY_LAST_EXPORT_DATE]
            val currentCount = if (savedDate == today) {
                preferences[KEY_DAILY_EXPORT_COUNT] ?: 0
            } else {
                0
            }
            preferences[KEY_LAST_EXPORT_DATE] = today
            preferences[KEY_DAILY_EXPORT_COUNT] = currentCount + 1
        }
    }
}
