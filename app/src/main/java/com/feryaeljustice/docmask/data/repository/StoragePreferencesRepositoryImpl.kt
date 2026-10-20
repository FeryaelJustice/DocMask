package com.feryaeljustice.docmask.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.feryaeljustice.docmask.data.local.preferences.docMaskDataStore
import com.feryaeljustice.docmask.domain.repository.StoragePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StoragePreferencesRepositoryImpl(
    private val context: Context
) : StoragePreferencesRepository {

    companion object {
        private val KEY_CUSTOM_STORAGE_URI = stringPreferencesKey("custom_storage_tree_uri")
    }

    override val customStorageUri: Flow<String?> = context.docMaskDataStore.data.map { preferences ->
        preferences[KEY_CUSTOM_STORAGE_URI]
    }

    override suspend fun setCustomStorageUri(uriString: String?) {
        context.docMaskDataStore.edit { preferences ->
            if (uriString.isNullOrBlank()) {
                preferences.remove(KEY_CUSTOM_STORAGE_URI)
            } else {
                preferences[KEY_CUSTOM_STORAGE_URI] = uriString
            }
        }
    }
}
