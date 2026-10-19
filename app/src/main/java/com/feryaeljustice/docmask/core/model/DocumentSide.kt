package com.feryaeljustice.docmask.core.model

/**
 * Represents the side of a two-sided document (e.g. Spanish DNI front and back).
 */
enum class DocumentSide(val displayName: String) {
    FRONT("Anverso"),
    BACK("Reverso")
}
