package com.feryaeljustice.docmask.data.ml

import android.graphics.Rect
import com.feryaeljustice.docmask.core.model.BoundingBox
import com.feryaeljustice.docmask.core.model.toBoundingBox
import com.feryaeljustice.docmask.core.model.RedactionArea
import com.feryaeljustice.docmask.core.model.SensitiveFieldType
import com.google.mlkit.vision.text.Text

/**
 * Heuristics and regex patterns tailored for Spanish Identity Documents (DNI, NIE, Pasaporte)
 * to automatically detect sensitive fields per AEPD recommendations.
 */
class SensitiveFieldDetector {

    // Regex for DNI Support number: typically 3 letters followed by 6 digits (e.g., AAA123456, ESP123456, IDESP)
    private val supportNumberRegex = Regex("\\b[A-Z]{3}\\s*\\d{6}\\b", RegexOption.IGNORE_CASE)

    // Regex for CAN code: 6 digits
    private val canCodeRegex = Regex("\\b\\d{6}\\b")

    // Keywords indicating support number vicinity
    private val supportKeywords = listOf("soporte", "idesp", "num", "support")

    // Keywords indicating CAN code vicinity
    private val canKeywords = listOf("can", "c.a.n")

    fun analyzeTextBlocks(visionText: Text): List<RedactionArea> {
        val detectedAreas = mutableListOf<RedactionArea>()
        val textBlocks = visionText.textBlocks

        for (block in textBlocks) {
            val blockText = block.text.trim()
            val blockBoundingBox = block.boundingBox

            // 1. Detect Support Number
            if (supportNumberRegex.containsMatchIn(blockText) ||
                supportKeywords.any { blockText.contains(it, ignoreCase = true) }
            ) {
                for (line in block.lines) {
                    val lineText = line.text.trim()
                    if (supportNumberRegex.containsMatchIn(lineText) ||
                        supportKeywords.any { lineText.contains(it, ignoreCase = true) }
                    ) {
                        line.boundingBox?.toBoundingBox()?.let { box ->
                            detectedAreas.add(
                                RedactionArea(
                                    type = SensitiveFieldType.SUPPORT_NUMBER,
                                    boundingBox = box,
                                    detectedText = lineText,
                                    isRedacted = true
                                )
                            )
                        }
                    }
                }
            }

            // 2. Detect CAN Code (Card Access Number)
            if (canKeywords.any { blockText.contains(it, ignoreCase = true) }) {
                for (line in block.lines) {
                    val lineText = line.text.trim()
                    if (canCodeRegex.containsMatchIn(lineText) ||
                        canKeywords.any { lineText.contains(it, ignoreCase = true) }
                    ) {
                        line.boundingBox?.toBoundingBox()?.let { box ->
                            detectedAreas.add(
                                RedactionArea(
                                    type = SensitiveFieldType.CAN_CODE,
                                    boundingBox = box,
                                    detectedText = lineText,
                                    isRedacted = true
                                )
                            )
                        }
                    }
                }
            }

            // 3. Detect Signature vicinity
            if (blockText.contains("firma", ignoreCase = true)) {
                blockBoundingBox?.toBoundingBox()?.let { box ->
                    detectedAreas.add(
                        RedactionArea(
                            type = SensitiveFieldType.SIGNATURE,
                            boundingBox = box,
                            detectedText = blockText,
                            isRedacted = true
                        )
                    )
                }
            }
        }

        return detectedAreas
    }
}
