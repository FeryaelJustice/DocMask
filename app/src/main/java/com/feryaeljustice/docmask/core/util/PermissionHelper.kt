package com.feryaeljustice.docmask.core.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.feryaeljustice.docmask.core.model.AppPermissionStatus
import com.feryaeljustice.docmask.core.model.AppPermissionType

object PermissionHelper {

    fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun isCameraPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldShowCameraRationale(context: Context): Boolean {
        val activity = findActivity(context) ?: return false
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.CAMERA
        )
    }

    fun checkCameraPermissionStatus(context: Context): AppPermissionStatus {
        return if (isCameraPermissionGranted(context)) {
            AppPermissionStatus.GRANTED
        } else {
            AppPermissionStatus.DENIED
        }
    }

    fun getRequiredMediaPermissions(sdkInt: Int = Build.VERSION.SDK_INT): List<String> {
        return when {
            sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                listOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            }
            sdkInt >= Build.VERSION_CODES.TIRAMISU -> {
                listOf(Manifest.permission.READ_MEDIA_IMAGES)
            }
            else -> {
                listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    fun checkMediaPermissionStatus(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): AppPermissionStatus {
        if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasFull = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            val hasPartial = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED

            return when {
                hasFull -> AppPermissionStatus.GRANTED
                hasPartial -> AppPermissionStatus.PARTIAL
                else -> AppPermissionStatus.DENIED
            }
        } else if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
            val hasImages = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
            return if (hasImages) AppPermissionStatus.GRANTED else AppPermissionStatus.DENIED
        } else {
            val hasStorage = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            return if (hasStorage) AppPermissionStatus.GRANTED else AppPermissionStatus.DENIED
        }
    }

    fun isMediaPermissionGranted(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): Boolean {
        val status = checkMediaPermissionStatus(context, sdkInt)
        return status == AppPermissionStatus.GRANTED || status == AppPermissionStatus.PARTIAL
    }

    fun shouldShowMediaRationale(context: Context): Boolean {
        val activity = findActivity(context) ?: return false
        val permissions = getRequiredMediaPermissions()
        return permissions.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    fun checkPermissionStatus(
        context: Context,
        type: AppPermissionType,
        sdkInt: Int = Build.VERSION.SDK_INT
    ): AppPermissionStatus {
        return when (type) {
            AppPermissionType.CAMERA -> checkCameraPermissionStatus(context)
            AppPermissionType.GALLERY_MEDIA -> checkMediaPermissionStatus(context, sdkInt)
            AppPermissionType.DOCUMENTS_SAF -> AppPermissionStatus.GRANTED
        }
    }

    fun createAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
