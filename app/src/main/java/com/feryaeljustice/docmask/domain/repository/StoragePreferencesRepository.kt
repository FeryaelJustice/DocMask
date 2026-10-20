package com.feryaeljustice.docmask.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository interface to persist and observe the custom storage directory URI chosen via SAF.
 */
interface StoragePreferencesRepository {

    /**
     * Flow of the persisted tree URI string chosen by the user in Settings, or null if default.
     */
    val customStorageUri: Flow<String?>

    /**
     * Updates or clears the custom storage directory URI.
     */
    suspend fun setCustomStorageUri(uriString: String?)
}
