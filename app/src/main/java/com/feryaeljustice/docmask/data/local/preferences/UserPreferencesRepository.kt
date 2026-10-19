package com.feryaeljustice.docmask.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "docmask_preferences")

interface UserPreferencesRepository {
    val isBiometricEnabled: Flow<Boolean>
    suspend fun setBiometricEnabled(enabled: Boolean)
}

class UserPreferencesRepositoryImpl(
    private val context: Context
) : UserPreferencesRepository {

    companion object {
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("key_biometric_enabled")
    }

    override val isBiometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_BIOMETRIC_ENABLED] ?: false
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }
}
