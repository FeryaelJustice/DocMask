package com.feryaeljustice.docmask.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CameraCaptureHelper {

    fun createTempPictureUri(context: Context): Uri {
        val storageDir = File(context.cacheDir, "camera_captures").apply {
            if (!exists()) mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(storageDir, "capture_$timestamp.jpg")
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun createCameraTempUri(context: Context): Uri = createTempPictureUri(context)
}
