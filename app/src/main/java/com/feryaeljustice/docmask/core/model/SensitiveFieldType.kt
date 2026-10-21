package com.feryaeljustice.docmask.core.model

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.feryaeljustice.docmask.R

/**
 * Types of sensitive identity fields detected in Spanish/European identity documents
 * (DNI 3.0/4.0, NIE, Passport, Driving License) per AEPD guidelines.
 */
enum class SensitiveFieldType(
    @StringRes val titleRes: Int,
    val displayName: String,
    val recommendedToRedact: Boolean
) {
    SUPPORT_NUMBER(
        titleRes = R.string.sensitive_field_support_number,
        displayName = "Número de Soporte / IDESP",
        recommendedToRedact = true
    ),
    CAN_CODE(
        titleRes = R.string.sensitive_field_can_code,
        displayName = "Código CAN (Card Access Number)",
        recommendedToRedact = true
    ),
    SIGNATURE(
        titleRes = R.string.sensitive_field_signature,
        displayName = "Firma Manuscrita",
        recommendedToRedact = true
    ),
    FACE(
        titleRes = R.string.sensitive_field_face,
        displayName = "Fotografía de Rostro",
        recommendedToRedact = false
    ),
    DATE_OF_BIRTH(
        titleRes = R.string.sensitive_field_date_of_birth,
        displayName = "Fecha de Nacimiento",
        recommendedToRedact = false
    ),
    MACHINE_READABLE_ZONE(
        titleRes = R.string.sensitive_field_mrz,
        displayName = "Zona MRZ (Lectura Mecánica)",
        recommendedToRedact = true
    ),
    CUSTOM_SELECTION(
        titleRes = R.string.sensitive_field_custom_selection,
        displayName = "Selección Manual",
        recommendedToRedact = true
    );

    fun getLabel(context: Context): String = context.getString(titleRes)

    @Composable
    fun getLabel(): String = stringResource(titleRes)
}
