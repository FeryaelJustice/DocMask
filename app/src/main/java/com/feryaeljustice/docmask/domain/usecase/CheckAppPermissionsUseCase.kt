package com.feryaeljustice.docmask.domain.usecase

import android.content.Context
import com.feryaeljustice.docmask.R
import com.feryaeljustice.docmask.core.model.AppPermissionStatus
import com.feryaeljustice.docmask.core.model.AppPermissionType
import com.feryaeljustice.docmask.core.model.PermissionItemState
import com.feryaeljustice.docmask.core.util.PermissionHelper

class CheckAppPermissionsUseCase {

    operator fun invoke(context: Context): List<PermissionItemState> {
        val cameraStatus = PermissionHelper.checkCameraPermissionStatus(context)
        val mediaStatus = PermissionHelper.checkMediaPermissionStatus(context)

        return listOf(
            PermissionItemState(
                type = AppPermissionType.CAMERA,
                title = context.getString(R.string.settings_permission_camera_title),
                description = context.getString(R.string.settings_permission_camera_desc),
                purposeRationale = context.getString(R.string.settings_permission_camera_rationale),
                status = cameraStatus,
                isSafAccess = false
            ),
            PermissionItemState(
                type = AppPermissionType.GALLERY_MEDIA,
                title = context.getString(R.string.settings_permission_gallery_title),
                description = context.getString(R.string.settings_permission_gallery_desc),
                purposeRationale = context.getString(R.string.settings_permission_gallery_rationale),
                status = mediaStatus,
                isSafAccess = false
            ),
            PermissionItemState(
                type = AppPermissionType.DOCUMENTS_SAF,
                title = context.getString(R.string.settings_permission_saf_title),
                description = context.getString(R.string.settings_permission_saf_desc),
                purposeRationale = context.getString(R.string.settings_permission_saf_rationale),
                status = AppPermissionStatus.GRANTED,
                isSafAccess = true
            )
        )
    }
}
