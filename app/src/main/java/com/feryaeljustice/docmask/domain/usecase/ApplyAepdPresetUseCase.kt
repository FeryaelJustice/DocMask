package com.feryaeljustice.docmask.domain.usecase

import com.feryaeljustice.docmask.core.model.RedactionArea

/**
 * Use case to apply the official AEPD (Agencia Española de Protección de Datos) preset.
 * Automatically marks high-risk identity and financial fields (support number, CAN, signature, MRZ, IBAN, cards)
 * while keeping biometric/identity verification elements (face, expiration) visible.
 */
class ApplyAepdPresetUseCase {

    operator fun invoke(areas: List<RedactionArea>): List<RedactionArea> {
        return areas.map { area ->
            area.copy(isRedacted = area.type.recommendedToRedact)
        }
    }
}
