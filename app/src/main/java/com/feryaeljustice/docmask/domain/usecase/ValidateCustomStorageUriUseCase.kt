package com.feryaeljustice.docmask.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

class ValidateCustomStorageUriUseCase(
    private val context: Context
) {

    operator fun invoke(uriString: String?): Result<String> {
        if (uriString.isNullOrBlank()) {
            return Result.success("Carpeta de la aplicación (Predeterminada)")
        }
        return try {
            val uri = uriString.toUri()
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            if (documentFile == null || !documentFile.exists()) {
                Result.failure(IllegalArgumentException("La carpeta seleccionada no existe o no es accesible."))
            } else if (!documentFile.canRead() || !documentFile.canWrite()) {
                Result.failure(SecurityException("No se cuenta con permisos de lectura y escritura en la carpeta seleccionada."))
            } else {
                val displayName = documentFile.name ?: uri.lastPathSegment ?: "Carpeta personalizada"
                Result.success(displayName)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}