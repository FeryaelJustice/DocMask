package com.feryaeljustice.docmask.core.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.feryaeljustice.docmask.core.model.AppPermissionStatus
import com.feryaeljustice.docmask.core.model.AppPermissionType

object PermissionHelper {

    fun getRequiredPermissions(
        type: AppPermissionType,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): List<String> {
        return when (type) {
            AppPermissionType.CAMERA -> listOf(Manifest.permission.CAMERA)
            AppPermissionType.GALLERY_MEDIA -> {
                if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.READ_MEDIA_IMAGES)
                } else {
                    listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
            AppPermissionType.DOCUMENTS_SAF -> emptyList()
        }
    }

    fun isPermissionGranted(
        context: Context,
        type: AppPermissionType,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): Boolean {
        val permissions = getRequiredPermissions(type, sdkInt)
        if (permissions.isEmpty()) return true

        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkPermissionStatus(
        context: Context,
        type: AppPermissionType,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): AppPermissionStatus {
        if (type == AppPermissionType.DOCUMENTS_SAF) {
            return AppPermissionStatus.GRANTED
        }

        return if (isPermissionGranted(context, type, sdkInt)) {
            AppPermissionStatus.GRANTED
        } else {
            AppPermissionStatus.DENIED
        }
    }

    fun createAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
