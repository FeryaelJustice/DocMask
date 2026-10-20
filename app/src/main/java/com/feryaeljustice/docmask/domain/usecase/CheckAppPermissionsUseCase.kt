package com.feryaeljustice.docmask.domain.usecase

import android.content.Context
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
                title = "Cámara",
                description = "Permite tomar fotos de tus documentos de identidad de forma directa sin almacenar fotos sin anonimizar.",
                purposeRationale = "La cámara solo se utiliza cuando decides tomar una foto directamente desde la app. La imagen se procesa 100% en local y nunca se sube a internet.",
                status = cameraStatus,
                isSafAccess = false
            ),
            PermissionItemState(
                type = AppPermissionType.GALLERY_MEDIA,
                title = "Galería y Multimedia",
                description = "Permite acceder a tus fotos para seleccionar el anverso o reverso del documento. Admite acceso a fotos seleccionadas o a toda la biblioteca.",
                purposeRationale = "El acceso a la galería te permite importar tu DNI o pasaporte desde tus fotos. Puedes elegir si permites el acceso solo a ciertas fotos o a todas. DocMask procesa la foto 100% en tu dispositivo y nunca la comparte.",
                status = mediaStatus,
                isSafAccess = false
            ),
            PermissionItemState(
                type = AppPermissionType.DOCUMENTS_SAF,
                title = "Archivos y Documentos (SAF)",
                description = "Acceso seguro a través del Selector del Sistema (Storage Access Framework) para importar PDFs y guardar en carpetas personalizadas.",
                purposeRationale = "DocMask no requiere permisos invasivos de gestión total de archivos en tu móvil. Cumple con las políticas de privacidad de Google Play permitiéndote elegir únicamente los PDFs que deseas anonimizar.",
                status = AppPermissionStatus.GRANTED,
                isSafAccess = true
            )
        )
    }
}
