package com.feryaeljustice.docmask.domain.usecase

import android.content.Context
import com.feryaeljustice.docmask.core.model.AppPermissionType
import com.feryaeljustice.docmask.core.model.PermissionItemState
import com.feryaeljustice.docmask.core.util.PermissionHelper

class CheckAppPermissionsUseCase {

    operator fun invoke(context: Context): List<PermissionItemState> {
        val cameraStatus = PermissionHelper.checkPermissionStatus(context, AppPermissionType.CAMERA)
        val galleryStatus = PermissionHelper.checkPermissionStatus(context, AppPermissionType.GALLERY_MEDIA)
        val safStatus = PermissionHelper.checkPermissionStatus(context, AppPermissionType.DOCUMENTS_SAF)

        return listOf(
            PermissionItemState(
                type = AppPermissionType.CAMERA,
                title = "Cámara",
                description = "Permite tomar fotos de tus documentos de identidad de forma directa sin almacenar fotos sin anonimizar.",
                purposeRationale = "La cámara solo se utiliza cuando decides tomar una foto directamente desde la app. La imagen se procesa 100% en local y nunca se sube a internet.",
                status = cameraStatus,
                isSafAccess = false
            ),
            PermissionItemState(
                type = AppPermissionType.GALLERY_MEDIA,
                title = "Galería y Multimedia",
                description = "Permite cargar imágenes de documentos escaneados o fotos guardadas en el dispositivo.",
                purposeRationale = "El acceso a la galería permite seleccionar la imagen del DNI o pasaporte que deseas proteger. DocMask solo lee la imagen que elijas expresamente y la procesa en tu dispositivo sin conexión.",
                status = galleryStatus,
                isSafAccess = false
            ),
            PermissionItemState(
                type = AppPermissionType.DOCUMENTS_SAF,
                title = "Archivos y Documentos (SAF)",
                description = "Acceso seguro a través del Selector del Sistema (Storage Access Framework) para importar PDFs y guardar en carpetas personalizadas.",
                purposeRationale = "DocMask no requiere permisos invasivos de gestión total de archivos en tu móvil. Cumple con las políticas de privacidad de Google Play permitiéndote elegir únicamente los PDFs que deseas anonimizar.",
                status = safStatus,
                isSafAccess = true
            )
        )
    }
}
