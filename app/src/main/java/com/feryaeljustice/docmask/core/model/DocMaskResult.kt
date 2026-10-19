package com.feryaeljustice.docmask.core.model

/**
 * Functional result type for Clean Architecture error handling.
 */
sealed interface DocMaskResult<out T> {
    data class Success<T>(val data: T) : DocMaskResult<T>
    data class Failure(val error: DocMaskError) : DocMaskResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
}

sealed class DocMaskError(open val message: String, open val cause: Throwable? = null) {
    data class ImageProcessingFailed(override val message: String, override val cause: Throwable? = null) :
        DocMaskError(message, cause)

    data class DetectionFailed(override val message: String, override val cause: Throwable? = null) :
        DocMaskError(message, cause)

    data class StorageError(override val message: String, override val cause: Throwable? = null) :
        DocMaskError(message, cause)

    data class SecurityError(override val message: String) :
        DocMaskError(message)

    data class ProFeatureRequired(val featureName: String) :
        DocMaskError("La funcionalidad '$featureName' requiere la versión Pro de DocMask.")
}
