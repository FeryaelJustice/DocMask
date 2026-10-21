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

inline fun <T> DocMaskResult<T>.onSuccess(action: (value: T) -> Unit): DocMaskResult<T> {
    if (this is DocMaskResult.Success) {
        action(data)
    }
    return this
}

inline fun <T> DocMaskResult<T>.onFailure(action: (error: DocMaskError) -> Unit): DocMaskResult<T> {
    if (this is DocMaskResult.Failure) {
        action(error)
    }
    return this
}

inline fun <T, R> DocMaskResult<T>.map(transform: (value: T) -> R): DocMaskResult<R> {
    return when (this) {
        is DocMaskResult.Success -> DocMaskResult.Success(transform(data))
        is DocMaskResult.Failure -> DocMaskResult.Failure(error)
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
