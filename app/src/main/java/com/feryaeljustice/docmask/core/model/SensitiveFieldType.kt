package com.feryaeljustice.docmask.core.model

/**
 * Types of sensitive identity fields detected in Spanish/European identity documents
 * (DNI 3.0/4.0, NIE, Passport, Driving License) per AEPD guidelines.
 */
enum class SensitiveFieldType(val displayName: String, val recommendedToRedact: Boolean) {
    SUPPORT_NUMBER(displayName = "Número de Soporte / IDESP", recommendedToRedact = true),
    CAN_CODE(displayName = "Código CAN (Card Access Number)", recommendedToRedact = true),
    SIGNATURE(displayName = "Firma Manuscrita", recommendedToRedact = true),
    FACE(displayName = "Fotografía de Rostro", recommendedToRedact = false),
    DATE_OF_BIRTH(displayName = "Fecha de Nacimiento", recommendedToRedact = false),
    MACHINE_READABLE_ZONE(displayName = "Zona MRZ (Lectura Mecánica)", recommendedToRedact = true),
    CUSTOM_SELECTION(displayName = "Selección Manual", recommendedToRedact = true)
}
