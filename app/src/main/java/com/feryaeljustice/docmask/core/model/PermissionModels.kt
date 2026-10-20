package com.feryaeljustice.docmask.core.model

enum class AppPermissionType {
    CAMERA,
    GALLERY_MEDIA,
    DOCUMENTS_SAF
}

enum class AppPermissionStatus {
    GRANTED,
    DENIED,
    NOT_REQUIRED
}

data class PermissionItemState(
    val type: AppPermissionType,
    val title: String,
    val description: String,
    val purposeRationale: String,
    val status: AppPermissionStatus,
    val isSafAccess: Boolean = false
)
